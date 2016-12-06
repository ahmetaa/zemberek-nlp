package zemberek.embedding;

import zemberek.core.collections.UIntMap;
import zemberek.core.logging.Log;
import zemberek.lm.LmVocabulary;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DistanceList {

    public final LmVocabulary vocabulary;
    private UIntMap<_Distance> distanceMap;

    public DistanceList(LmVocabulary vocabulary, UIntMap<_Distance> distanceMap) {
        this.vocabulary = vocabulary;
        this.distanceMap = distanceMap;
    }

    public static DistanceList readFromBinary(Path binFile, Path vocabFile) throws IOException {
        LmVocabulary vocabulary = LmVocabulary.loadFromBinary(vocabFile.toFile());
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(binFile.toFile()), 100000))) {
            int wordSize = in.readInt();
            int vectorSize = in.readInt();
            UIntMap<_Distance> distanceMap = new UIntMap<>(wordSize * 2);
            for (int i = 0; i < wordSize; i++) {
                int sourceWordIndex = in.readInt();
                int[] wordIndexes = new int[vectorSize];
                float[] scores = new float[vectorSize];
                for (int j = 0; j < vectorSize; j++) {
                    wordIndexes[j] = in.readInt();
                    scores[j] = in.readFloat();
                }

                if (i % 10000 == 0)
                    Log.info("%d completed", i);
                distanceMap.put(sourceWordIndex, new _Distance(wordIndexes, scores));
            }
            return new DistanceList(vocabulary, distanceMap);
        }
    }


    public void saveReduced(Path outFile, int count) throws IOException {

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outFile.toFile())))) {
            dos.writeInt(distanceMap.size());
            dos.writeInt(count);
            int[] keysSorted = distanceMap.getKeyArraySorted();
            for (int i = 0; i < keysSorted.length; i++) {
                dos.writeInt(keysSorted[i]);
                _Distance distance = distanceMap.get(keysSorted[i]);
                for (int j = 0; j < count; j++) {
                    dos.writeInt(distance.wordIndexes[j]);
                    dos.writeFloat(distance.scores[j]);
                }
                if (i % 10000 == 0)
                    Log.info("%d completed", i);
            }
        }
    }

    private static class _Distance {
        int[] wordIndexes;
        float[] scores;

        public _Distance(int[] wordIndexes, float[] scores) {
            this.wordIndexes = wordIndexes;
            this.scores = scores;
        }
    }

    public boolean containsWord(String s) {
        return vocabulary.contains(s);
    }

    public List<WordDistances.Distance> getDistance(String word) {
        if (!vocabulary.contains(word)) {
            return Collections.emptyList();
        }
        List<WordDistances.Distance> distances = new ArrayList<>();
        _Distance d = distanceMap.get(vocabulary.indexOf(word));
        for (int i = 0; i < d.wordIndexes.length; i++) {
            distances.add(new WordDistances.Distance(
                    vocabulary.getWord(d.wordIndexes[i]),
                    d.scores[i]));
        }
        return distances;
    }

    public static void main(String[] args) throws IOException {
        DistanceList experiment = DistanceList.readFromBinary(
                Paths.get("/media/depo/data/aaa/corpora/distance-large-min10.bin"),
                Paths.get("/media/depo/data/aaa/corpora/vocab-large-min10.bin"));
        Log.info("Writing");
        experiment.saveReduced(Paths.get("/media/depo/data/aaa/corpora/distance-10.bin"),10);

    }


}
