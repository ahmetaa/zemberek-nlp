package zemberek.tokenizer;


import zemberek.core.collections.DoubleValueMap;
import zemberek.core.collections.UIntSet;
import zemberek.core.io.SimpleTextReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrainableTokenizer {
    public static final int SKIP_SPACE_FREQUENCY = 50;
    public static final String BOUNDARY_CHARS = ".!?";

    DoubleValueMap<String> weights = new DoubleValueMap<>();

    public TrainableTokenizer(DoubleValueMap<String> weights) {
        this.weights = weights;
    }

    public static class Trainer {
        File trainFile;
        int iterationCount;

        public Trainer(File trainFile, int iterationCount) {
            this.trainFile = trainFile;
            this.iterationCount = iterationCount;
        }

        public TrainableTokenizer train() throws IOException {
            DoubleValueMap<String> weights = new DoubleValueMap<>();
            List<String> sentences = SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList();
            DoubleValueMap<String> averages = new DoubleValueMap<>();
            UIntSet indexSet = new UIntSet();
            Random rnd = new Random(1);
            StringBuilder sb = new StringBuilder();
            int boundaryIndexCounter = 0;
            int sentenceCounter = 0;
            for (String sentence : sentences) {
                sb.append(sentence);
                boundaryIndexCounter = sb.length() - 1;
                indexSet.add(boundaryIndexCounter);
                // in some sentences we skip adding a space between sentences.
                if (rnd.nextInt(SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                    sb.append(" ");
                }
                sentenceCounter++;
            }

            int updateCount = 0;

            String joinedSentence = sb.toString();
            for (int i = 0; i < iterationCount; i++) {

                for (int j = 0; j < joinedSentence.length(); j++) {
                    // skip if char cannot be a boundary char.
                    char chr = joinedSentence.charAt(j);
                    if (BOUNDARY_CHARS.indexOf(chr) < 0) {
                        continue;
                    }
                    List<String> features = extractFeatures(joinedSentence, j);
                    double score = 0;
                    for (String feature : features) {
                        score += weights.get(feature);
                    }
                    int update = 0;
                    // if we found no-boundary but it is a boundary
                    if (score <= 0 && indexSet.contains(j)) {
                        update = 1;
                    }
                    // if we found boundary but it is not a boundary
                    else if (score > 0 && !indexSet.contains(j)) {
                        update = -1;
                    }
                    updateCount++;
                    if (update != 0) {
                        for (String feature : features) {
                            double d = weights.incrementByAmount(feature, update);
                            if (d == 0.0) {
                                weights.remove(feature);
                            }
                            d = averages.incrementByAmount(feature, updateCount*update);
                            if (d == 0.0) {
                                averages.remove(feature);
                            }
                        }
                    }
                }
            }
            for (String key : weights) {
                weights.set(key, weights.get(key) - averages.get(key)*1d / updateCount);
            }

            return new TrainableTokenizer(weights);
        }


    }

    private static List<String> extractFeatures(String joinedSentence, int j) {
        return null;
    }

    public List<String> getTokensSentences(String doc) {
        List<String> sentences = new ArrayList<>();
        return sentences;
    }

}
