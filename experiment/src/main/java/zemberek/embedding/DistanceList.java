package zemberek.embedding;

import zemberek.core.logging.Log;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class DistanceList {
    public final String source;
    List<WordDistance> distances;

    public DistanceList(String source, List<WordDistance> distList) {
        this.source = source;
        this.distances = distList;
    }

    public static void saveDistanceListTxt(
            Path vectorFile,
            Path outFile,
            int size,
            int blockSize,
            int threadSize) throws Exception {
        List<WordVector> wordVectors = WordVector.loadFromBinary(vectorFile);

        // create a thread pool executor
        ExecutorService es = Executors.newFixedThreadPool(threadSize);
        CompletionService<List<BlockUnit>> completionService = new ExecutorCompletionService<>(es);

        for (int i = 0; i < wordVectors.size(); i += blockSize) {
            int endIndex = i + blockSize >= wordVectors.size() ? wordVectors.size() : i + blockSize;
            completionService.submit(new BlockDistanceTask(wordVectors, size, blockSize, i, endIndex));
        }
        es.shutdown();

        try (PrintWriter pw = new PrintWriter(outFile.toFile())) {
            int i = 0;
            while (i < wordVectors.size() / blockSize) {
                List<BlockUnit> units = completionService.take().get();
                for (BlockUnit unit : units) {

                    if (unit.vector.word.startsWith("_"))
                        continue;

                    pw.println(unit.vector.word + " --------------------- ");
                    List<WordDistance> distList = new ArrayList<>(unit.distQueue);
                    Collections.sort(distList);
                    Collections.reverse(distList);
                    if (distList.size() == 0) {
                        Log.warn("No distance for %s", unit.vector.word);
                    }
                    for (WordDistance dist : distList) {
                        pw.println(String.valueOf(dist));
                    }
                }
                i++;
                if ((i * blockSize) % 10 == 0) {
                    Log.info("%d completed", i * blockSize);
                }
            }
        }
    }

    public static void saveDistanceListBin(
            Path vectorFile,
            Path outFile,
            int distanceAmount,
            int blockSize,
            int threadSize) throws Exception {
        List<WordVector> wordVectors = WordVector.loadFromBinary(vectorFile);

        // create a thread pool executor
        ExecutorService es = Executors.newFixedThreadPool(threadSize);
        CompletionService<List<BlockUnit>> completionService = new ExecutorCompletionService<>(es);

        for (int i = 0; i < wordVectors.size(); i += blockSize) {
            int endIndex = i + blockSize >= wordVectors.size() ? wordVectors.size() : i + blockSize;
            completionService.submit(new BlockDistanceTask(wordVectors, distanceAmount, blockSize, i, endIndex));
        }
        es.shutdown();

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile.toFile())))) {
            dos.writeInt(wordVectors.size());
            dos.writeInt(distanceAmount);
            int i = 0;
            while (i < wordVectors.size() / blockSize) {
                List<BlockUnit> units = completionService.take().get();
                for (BlockUnit unit : units) {
                    dos.writeUTF(unit.vector.word);
                    List<WordDistance> distList = new ArrayList<>(unit.distQueue);
                    Collections.sort(distList);
                    Collections.reverse(distList);
                    for (WordDistance dist : distList) {
                        dos.writeUTF(dist.v.word);
                        dos.writeFloat(dist.distance);
                    }
                }
                i++;
                if ((i * blockSize % 10) == 0) {
                    Log.info("%d completed", i * blockSize);
                }

            }
        }
    }

    private static class BlockDistanceTask implements Callable<List<BlockUnit>> {

        List<WordVector> wordVectors;
        int distanceAmount;
        int blockSize;
        int blockBeginIndex;
        int endIndex;

        public BlockDistanceTask(
                List<WordVector> wordVectors,
                int distanceAmount,
                int blockSize,
                int blockBeginIndex,
                int endIndex) {
            this.wordVectors = wordVectors;
            this.distanceAmount = distanceAmount;
            this.blockSize = blockSize;
            this.blockBeginIndex = blockBeginIndex;
            this.endIndex = endIndex;
        }

        @Override
        public List<BlockUnit> call() throws Exception {
            List<BlockUnit> units = new ArrayList<>();
            for (int j = blockBeginIndex; j < endIndex; j++) {
                WordVector vector = wordVectors.get(j);
                units.add(new BlockUnit(vector));
            }
            for (int k = 0; k < wordVectors.size(); k += blockSize) {
                int end = (k + blockSize) >= wordVectors.size() ? wordVectors.size() : (k + blockSize);
                for (BlockUnit unit : units) {
                    for (int j = k; j < end; j++) {
                        WordVector v = wordVectors.get(j);
                        if (v == unit.vector)
                            continue;
                        if (v.word.startsWith("_") || v.word.startsWith("^"))
                            continue;
                        float distance = unit.vector.cosDistance(v);
                        if (unit.distQueue.size() < distanceAmount)
                            unit.distQueue.add(new WordDistance(v, distance));
                        else {
                            WordDistance weakest = unit.distQueue.peek();
                            if (weakest.distance < distance) {
                                unit.distQueue.remove();
                                unit.distQueue.add(new WordDistance(v, distance));
                            }
                        }
                    }
                }
            }
            return units;
        }
    }


    public static List<DistanceList> readFromBinary(Path binFile) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(binFile.toFile()), 100000))) {
            int wordSize = in.readInt();
            int vectorSize = in.readInt();
            List<DistanceList> distLists = new ArrayList<>();
            for (int i = 0; i < wordSize; i++) {
                String s = in.readUTF();
                List<WordDistance> dists = new ArrayList<>();
                for (int j = 0; j < (vectorSize); j++) {
                    String word = in.readUTF();
                    Float f = in.readFloat();
                    dists.add(new WordDistance(new WordVector(word), f));
                }
                Log.info("%d completed", i);
                distLists.add(new DistanceList(s, dists));
            }
            return distLists;
        }
    }


    public static void writeToTxt(Path in, Path out, int size) throws IOException {
        try (PrintWriter pw = new PrintWriter(out.toFile())) {
            for (DistanceList distList : DistanceList.readFromBinary(in)) {
                pw.println(distList.source);
                for (int k = 0; k < size; k++) {
                    pw.println(distList.distances.get(k));
                }
            }
        }
    }

}
