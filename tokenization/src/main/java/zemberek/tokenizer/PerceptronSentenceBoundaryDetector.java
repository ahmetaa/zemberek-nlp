package zemberek.tokenizer;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import zemberek.core.collections.CountSet;
import zemberek.core.collections.DoubleValueMap;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.logging.Log;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.*;
import java.util.*;

/**
 * An average perceptron based sentence boundary detector.
 */
public class PerceptronSentenceBoundaryDetector implements SentenceBoundaryDetector {

    private DoubleValueMap<String> weights = new DoubleValueMap<>();

    // for safe lazy loading singleton, we use an enum.
    enum Morphology {
        INSTANCE;
        TurkishMorphology morphology;

        Morphology() {
            try {
                morphology = TurkishMorphology.createWithDefaults();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Locale localeTr = new Locale("tr");

    public PerceptronSentenceBoundaryDetector(DoubleValueMap<String> weights) {
        this.weights = weights;
    }

    public static List<String> cleanBeginEnd(List<String> in) {
        List<String> res = new ArrayList<>(in.size());
        for (String s : in) {
            res.add(s.replaceAll("<s>|</s>|<S>|</S>", " ").replaceAll("\\s", " ").trim());
        }
        return res;
    }

    public static class Trainer {
        File trainFile;
        int iterationCount;

        public Trainer(File trainFile, int iterationCount) {
            this.trainFile = trainFile;
            this.iterationCount = iterationCount;
        }

        public PerceptronSentenceBoundaryDetector train() throws IOException {
            DoubleValueMap<String> weights = new DoubleValueMap<>();
            CountSet<String> counts = new CountSet<>();
            List<String> sentences = cleanBeginEnd(SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList());

            Set<Integer> indexSet = new LinkedHashSet<>();
            List<String> tokens = new ArrayList<>();

            int boundaryIndexCounter;
            for (String sentence : sentences) {
                List<String> currentTokens = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(sentence);
                tokens.addAll(currentTokens);
                boundaryIndexCounter = tokens.size() - 1;
                indexSet.add(boundaryIndexCounter);
            }

            int updateCount = 0;

            for (int i = 0; i < iterationCount; i++) {

                Log.info("Iteration %d", i);
                for (int j = 0; j < tokens.size(); j++) {

                    List<String> features = extractFeatures(tokens, j);
                    double score = 0;
                    for (String feature : features) {
                        score += weights.get(feature);
                    }
                    counts.incrementAll(features);
                    double update = 0;
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
                                counts.remove(feature);
                            }
                        }
                    }
                }
            }
            for (String weight : weights) {
                weights.set(weight, weights.get(weight) / updateCount);
            }

            return new PerceptronSentenceBoundaryDetector(weights);
        }
    }

    public void saveBinary(File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            dos.writeInt(weights.size());
            for (String feature : weights) {
                dos.writeUTF(feature);
                dos.writeFloat((float) weights.get(feature));
            }
        }
    }

    public static PerceptronSentenceBoundaryDetector loadFromFile(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int size = dis.readInt();
            DoubleValueMap<String> features = new DoubleValueMap<>((int) (size * 1.5));
            for (int i = 0; i < size; i++) {
                features.set(dis.readUTF(), dis.readFloat());
            }
            return new PerceptronSentenceBoundaryDetector(features);
        }
    }

    @Override
    public List<String> getSentences(String input) {

        List<String> sentences = new ArrayList<>();

        List<String> tokens = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(input);

        int begin = 0;
        for (int j = 0; j < tokens.size(); j++) {

            List<String> features = extractFeatures(tokens, j);
            double score = 0;
            for (String feature : features) {
                score += weights.get(feature);
            }
            if (score > 0) {
                sentences.add(Joiner.on(" ").join(tokens.subList(begin, j + 1)));
                begin = j + 1;
            }
        }
        if (begin < tokens.size() - 1) {
            sentences.add(Joiner.on(" ").join(tokens.subList(begin, tokens.size())));
        }
        return sentences;
    }

    private static List<String> extractFeatures(List<String> input, int pointer) {

        List<String> features = new ArrayList<>();

        String currentWord = input.get(pointer);
        String previousWord = pointer > 0 ? input.get(pointer - 1) : "<s>";
        String nextWord = pointer < input.size() - 1 ? input.get(pointer + 1) : "</s>";

        features.addAll(extractLexicalFeatures(previousWord, currentWord, nextWord));

        String twoPreviousWord = pointer > 1 ? input.get(pointer - 2) : "<s>";
        String twoNextWord = pointer < input.size() - 2 ? input.get(pointer + 2) : "</s>";

        features.add("PW2 = " + twoPreviousWord);
        features.add("NW2 = " + twoNextWord);

        features.addAll(extractMorphologicalFeatures(previousWord, currentWord, nextWord));

        return features;
    }

    private static List<String> extractLexicalFeatures(String previousWord, String currentWord, String nextWord) {

        List<String> features = new ArrayList<>();

        features.add("CW = " + currentWord);
        features.add("PW = " + previousWord);
        features.add("NW = " + nextWord);
        features.add("PW + CW = " + previousWord + " " + currentWord);
        features.add("CW + NW = " + currentWord + " " + nextWord);
        features.add("PW + CW + NW = " + previousWord + " " + currentWord + " " + nextWord);

        return features;
    }


    private static List<String> extractMorphologicalFeatures(String previousWord, String currentWord, String nextWord) {
        List<String> features = new ArrayList<>();
        features.addAll(getMorphologicalFeatures(currentWord, "CW"));
        features.addAll(getMorphologicalFeatures(previousWord, "PW"));
        features.addAll(getMorphologicalFeatures(nextWord, "NW"));
        return features;
    }

    public static Collection<String> getMorphologicalFeatures(String word, String position) {
        List<WordAnalysis> analyses = Morphology.INSTANCE.morphology.analyze(word);
        LinkedHashSet<String> lemmas = new LinkedHashSet<>();
        LinkedHashSet<String> posSet = new LinkedHashSet<>();
        LinkedHashSet<String> igSet = new LinkedHashSet<>();

        for (WordAnalysis analysis : analyses) {
            lemmas.add(analysis.getLemma());
            posSet.add(analysis.getPos().shortForm);
            igSet.add(analysis.getLastIg().formatNoSurface());
        }

        LinkedHashSet<String> features = new LinkedHashSet<>();
        for (String s : posSet) {
            features.add(position + "-POS = " + s);
        }
        for (String s : lemmas) {
            features.add(position + "-LEMMA = " + s);
        }
        for (String s : igSet) {
            if (s.contains("Interjection"))
                continue;
            features.add(position + "-IG = " + s);
            if (s.contains("Verb")) {
                features.add(position + "Verb");
            }

        }

        return features;
    }
}
