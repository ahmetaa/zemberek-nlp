package zemberek.embedding;

import zemberek.core.logging.Log;
import zemberek.lm.LmVocabulary;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.*;

public class WordDistances {

    public final String source;
    Distance[] distances;

    public WordDistances(String source, Distance[] distList) {
        this.source = source;
        this.distances = distList;
    }

    public static void saveDistanceListBin(
            Path vectorFile,
            Path outFile,
            Path vocabFile,
            int distanceAmount,
            int blockSize,
            int threadSize) throws Exception {
        Log.info("Loading vectors.");
        List<WordVector> wordVectors = WordVector.loadFromBinary(vectorFile);

        Log.info("Writing vocabulary.");
        // write vocabulary.
        List<String> words = new ArrayList<>(wordVectors.size());
        wordVectors.forEach(s -> words.add(s.word));
        LmVocabulary vocabulary = new LmVocabulary(words);
        vocabulary.saveBinary(vocabFile.toFile());

        Log.info("Calculating distances.");
        // create a thread pool executor
        ExecutorService es = Executors.newFixedThreadPool(threadSize);
        CompletionService<List<BlockUnit>> completionService = new ExecutorCompletionService<>(es);

        int blockCounter = 0;
        for (int i = 0; i < wordVectors.size(); i += blockSize) {
            int endIndex = i + blockSize >= wordVectors.size() ? wordVectors.size() : i + blockSize;
            completionService.submit(new BlockDistanceTask(wordVectors, distanceAmount, blockSize, i, endIndex));
            blockCounter++;
        }
        es.shutdown();
        List<WordDistances> distancesToWrite = new ArrayList<>(wordVectors.size());
        int i = 0;
        while (i < blockCounter) {
            List<BlockUnit> units = completionService.take().get();
            for (BlockUnit unit : units) {
                String source = unit.vector.word;
                List<Distance> distList = new ArrayList<>(unit.distQueue);
                Collections.sort(distList);
                Collections.reverse(distList);
                distancesToWrite.add(new WordDistances(source, distList.toArray(new Distance[distList.size()])));
            }
            i++;
            if ((i * blockSize % 10) == 0) {
                Log.info("%d of %d completed", i * blockSize, wordVectors.size());
            }
        }

        Log.info("Writing.");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile.toFile())))) {
            dos.writeInt(wordVectors.size());
            dos.writeInt(distanceAmount);
            for (WordDistances w : distancesToWrite) {
                dos.writeInt(vocabulary.indexOf(w.source));
                for (Distance token : w.distances) {
                    dos.writeInt(vocabulary.indexOf(token.word));
                    dos.writeFloat(token.distance);
                }
            }
        }
    }

    public static class Distance implements Comparable<Distance> {
        public final String word;
        public final float distance;

        public Distance(String word, float distance) {
            this.word = word;
            this.distance = distance;
        }

        public String toString() {
            return word + " " + distance;
        }

        @Override
        public int compareTo(Distance o) {
            return Float.compare(distance, o.distance);
        }
    }

    private static class BlockUnit {
        public final WordVector vector;
        PriorityQueue<Distance> distQueue = new PriorityQueue<>();

        public BlockUnit(WordVector vector) {
            this.vector = vector;
        }
    }


    private static class BlockDistanceTask implements Callable<List<BlockUnit>> {

        List<WordVector> wordVectors;
        int distanceAmount;
        int blockSize;
        int blockBeginIndex;
        int endIndex;

        private BlockDistanceTask(
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
                        // skip self.
                        if (v == unit.vector) {
                            continue;
                        }
                        float distance = unit.vector.cosDistance(v);
                        if (unit.distQueue.size() < distanceAmount) {
                            unit.distQueue.add(new Distance(v.word, distance));
                        } else {
                            Distance weakest = unit.distQueue.peek();
                            if (weakest.distance < distance) {
                                unit.distQueue.remove();
                                unit.distQueue.add(new Distance(v.word, distance));
                            }
                        }
                    }
                }
            }
            return units;
        }
    }


    public static List<WordDistances> readFromBinary(Path binFile, Path vocabFile) throws IOException {
        LmVocabulary vocabulary = LmVocabulary.loadFromBinary(vocabFile.toFile());
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(binFile.toFile()), 100000))) {
            int wordSize = in.readInt();
            int vectorSize = in.readInt();
            List<WordDistances> distLists = new ArrayList<>(wordSize);
            for (int i = 0; i < wordSize; i++) {
                String s = vocabulary.getWord(in.readInt());
                Distance[] distances = new Distance[vectorSize];
                for (int j = 0; j < vectorSize; j++) {
                    String word = vocabulary.getWord(in.readInt());
                    Float f = in.readFloat();
                    distances[j] = new Distance(word, f);
                }
                if (i % 10000 == 0)
                    Log.info("%d completed", i);
                distLists.add(new WordDistances(s, distances));
            }
            return distLists;
        }
    }


    public static void writeToTxt(Path in, Path vocab, Path out, int size) throws IOException {
        try (PrintWriter pw = new PrintWriter(out.toFile())) {
            for (WordDistances distList : WordDistances.readFromBinary(in, vocab)) {
                pw.println(distList.source);
                for (int k = 0; k < size; k++) {
                    pw.println(distList.distances[k]);
                }
            }
        }
    }

}
