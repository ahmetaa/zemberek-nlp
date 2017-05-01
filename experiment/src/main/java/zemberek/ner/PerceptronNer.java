package zemberek.ner;

import zemberek.core.ScoredItem;
import zemberek.core.collections.DoubleValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisFormatter;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Multi-class averaged perceptron training.
 */
public class PerceptronNer {

    private Map<String, ClassWeights> model = new HashMap<>();
    private TurkishMorphology morphology;
    private Gazetteers gazetteers;

    static class Gazetteers {
        Set<String> locationWords = new HashSet<>();
        Set<String> organizationWords = new HashSet<>();

        public Gazetteers(Path locationPath, Path organizationPath) throws IOException {
            locationWords.addAll(Files.readAllLines(locationPath));
            organizationWords.addAll(Files.readAllLines(organizationPath));
        }

        public Gazetteers() {
        }
    }

    public PerceptronNer(
            Map<String, ClassWeights> model,
            TurkishMorphology morphology) {
        this.model = model;
        this.morphology = morphology;
        this.gazetteers = new Gazetteers();
    }

    public PerceptronNer(Map<String, ClassWeights> model, TurkishMorphology morphology, Gazetteers gazetteers) {
        this.model = model;
        this.morphology = morphology;
        this.gazetteers = gazetteers;
    }

    public static class ClassWeights {
        String id;
        DoubleValueMap<String> sparseWeights = new DoubleValueMap<>();
        List<DenseWeights> denseWeights = new ArrayList<>();

        public ClassWeights(String id) {
            this.id = id;
        }

        void updateSparse(List<String> inputs, float value) {
            for (String input : inputs) {
                sparseWeights.incrementByAmount(input, value);
            }
        }

        ClassWeights copy() {
            ClassWeights weights = new ClassWeights(id);
            weights.sparseWeights = sparseWeights.copy();
            List<DenseWeights> copy = new ArrayList<>();
            for (DenseWeights denseWeight : denseWeights) {
                copy.add(new DenseWeights(denseWeight.id, denseWeight.weights.clone()));
            }
            weights.denseWeights = copy;
            return weights;
        }
    }

    private static Map<String, ClassWeights> copyModel(Map<String, ClassWeights> model) {
        Map<String, ClassWeights> copy = new HashMap<>();
        for (String s : model.keySet()) {
            copy.put(s, model.get(s).copy());
        }
        return copy;
    }


    public static class DenseWeights {
        String id;
        float[] weights;

        public DenseWeights(String id, float[] weights) {
            this.id = id;
            this.weights = weights;
        }
    }

    public static Map<String, ClassWeights> train(
            TurkishMorphology morphology,
            Gazetteers gazetteers,
            NerDataSet trainingSet,
            NerDataSet devSet,
            int iterationCount,
            float learningRate) {

        Map<String, ClassWeights> averages = new HashMap<>();
        Map<String, ClassWeights> model = new HashMap<>();
        IntValueMap<String> counts = new IntValueMap<>();

        //initialize model weights for all classes.
        for (String typeId : trainingSet.typeIds) {
            model.put(typeId, new ClassWeights(typeId));
            averages.put(typeId, new ClassWeights(typeId));
        }

        int count = 0;

        for (int it = 0; it < iterationCount; it++) {

            int errorCount = 0;
            int tokenCount = 0;

            for (NerSentence sentence : trainingSet.sentences) {

                for (int i = 0; i < sentence.tokens.size(); i++) {

                    tokenCount++;
                    NerToken currentToken = sentence.tokens.get(i);
                    String currentId = currentToken.tokenId;

                    FeatureData data = new FeatureData(morphology, gazetteers, sentence, i);
                    List<String> sparseFeatures = data.getSparseFeatures();

                    if (i > 0) {
                        sparseFeatures.add("PreType=" + sentence.tokens.get(i - 1).tokenId);
                    }
                    if (i > 1) {
                        sparseFeatures.add("2PreType=" + sentence.tokens.get(i - 2).tokenId);
                    }
                    if (i > 2) {
                        sparseFeatures.add("3PreType=" + sentence.tokens.get(i - 3).tokenId);
                    }

                    ScoredItem<String> predicted = predictTypeAndPosition(model, sparseFeatures);
                    String predictedId = predicted.item;

                    if (predictedId.equals(currentId)) {
                        // do nothing
                        counts.addOrIncrement(predictedId);
                        count++;
                        continue;
                    }
                    count++;
                    counts.addOrIncrement(currentId);
                    counts.addOrIncrement(predictedId);
                    errorCount++;

                    model.get(currentId).updateSparse(sparseFeatures, +learningRate);
                    model.get(predictedId).updateSparse(sparseFeatures, -learningRate);

                    averages.get(currentId).updateSparse(sparseFeatures, counts.get(currentId) * learningRate);
                    averages.get(predictedId).updateSparse(sparseFeatures, -counts.get(predictedId) * learningRate);
                }
            }
            Log.info("Iteration %d, Token error = %.6f", it + 1, (errorCount * 1d) / tokenCount);

            Map<String, ClassWeights> copyModel = copyModel(model);
            averageWeights(averages, copyModel, counts);
            PerceptronNer ner = new PerceptronNer(copyModel, morphology, gazetteers);
            NerDataSet result = ner.test(devSet);
            testLog(devSet, result).dump();
        }

        averageWeights(averages, model, counts);

        Log.info("Training finished.");
        return model;
    }

    private static void averageWeights(Map<String, ClassWeights> averages, Map<String, ClassWeights> model, IntValueMap<String> counts) {
        for (String typeId : model.keySet()) {
            DoubleValueMap<String> w = model.get(typeId).sparseWeights;
            DoubleValueMap<String> a = averages.get(typeId).sparseWeights;
            for (String s : w) {
                w.set(s, w.get(s) - a.get(s) / counts.get(typeId));
            }
        }
    }

    public static ScoredItem<String> predictTypeAndPosition(Map<String, ClassWeights> model, List<String> sparseKeys) {
        List<ScoredItem<String>> scores = new ArrayList<>();
        for (String out : model.keySet()) {
            ClassWeights o = model.get(out);
            float score = 0f;
            for (String s : sparseKeys) {
                score += o.sparseWeights.get(s);
            }
            scores.add(new ScoredItem<>(out, score));
        }
        return scores.stream().max((a, b) -> Float.compare(a.score, b.score)).get();
    }

    static class FeatureData {
        TurkishMorphology morphology;
        Gazetteers gazetteers;
        String currentWord;
        String currentWordOrig;
        String nextWord;
        String nextWordOrig;
        String nextWord2;
        String nextWord2Orig;
        String previousWord;
        String previousWordOrig;
        String previousWord2;
        String previousWord2Orig;

        static WordAnalysisFormatter formatter = new WordAnalysisFormatter();

        FeatureData(TurkishMorphology morphology, Gazetteers gazetteers, NerSentence sentence, int index) {
            this.morphology = morphology;
            this.gazetteers = gazetteers;
            List<NerToken> tokens = sentence.tokens;
            this.currentWord = tokens.get(index).normalized;
            this.currentWordOrig = tokens.get(index).word;
            if (index == tokens.size() - 1) {
                //this.nextWord = "</s>";
                //this.nextWord2 = "</s>";
                //this.nextWordOrig = "</s>";
                //this.nextWord2Orig = "</s>";
            } else if (index == tokens.size() - 2) {
                this.nextWord = tokens.get(index + 1).normalized;
                //this.nextWord2 = "</s>";
                this.nextWordOrig = tokens.get(index + 1).word;
                this.nextWord2Orig = tokens.get(index + 1).word;
            } else {
                this.nextWord = tokens.get(index + 1).normalized;
                this.nextWord2 = tokens.get(index + 2).normalized;
                this.nextWordOrig = tokens.get(index + 1).word;
                this.nextWord2Orig = tokens.get(index + 1).word;
            }
            if (index == 0) {
                //this.previousWord = "<s>";
                //this.previousWord2 = "<s>";
                //this.previousWordOrig = "<s>";
                //this.previousWord2Orig = "<s>";
            } else if (index == 1) {
                this.previousWord = tokens.get(index - 1).normalized;
                //this.previousWord2 = "<s>";
                this.previousWordOrig = tokens.get(index - 1).word;
                this.previousWord2Orig = tokens.get(index - 1).word;
            } else {
                this.previousWord = tokens.get(index - 1).normalized;
                this.previousWord2 = tokens.get(index - 2).normalized;
                this.previousWordOrig = tokens.get(index - 1).word;
                this.previousWord2Orig = tokens.get(index - 1).word;
            }
        }

        void morphologicalFeatures(String word, String featurePrefix, List<String> features) {
            if (word == null) {
                return;
            }
            List<WordAnalysis> analyses = morphology.analyze(word);
            WordAnalysis longest = analyses.get(0);
            for (WordAnalysis analysis : analyses) {
                if (analysis.isUnknown()) {
                    return;
                }
                if (analysis == longest) {
                    continue;
                }
                List<String> cLemmas = analysis.getLemmas();
                List<String> lLemmas = longest.getLemmas();

                if (cLemmas.get(cLemmas.size() - 1).length() >
                        lLemmas.get(lLemmas.size() - 1).length()) {
                    longest = analysis;
                }
            }
            List<String> lemmas = longest.getLemmas();
            features.add(featurePrefix + "Stem:" + longest.getRoot());
            String ending = longest.getEnding();
            if (ending.length() > 0) {
                features.add(featurePrefix + "Ending:" + ending);
            }
            features.add(featurePrefix + "LongLemma:" + lemmas.get(lemmas.size() - 1));
            features.add(featurePrefix + "POS:" + longest.getPos());
            features.add(featurePrefix + "LastIg:" + longest.getLastIg().formatNoSurface());

            //features.add(featurePrefix + "ContainsProper:" + containsProper);

/*            if (featurePrefix.equals("CW")) {
                for (String lemma : lemmas) {
                    if (gazetteers.organizationWords.contains(lemma)) {
                        features.add(featurePrefix + "NW_Org_Gzt");
                        break;
                    }
                }

                for (String lemma : lemmas) {
                    if (gazetteers.locationWords.contains(lemma)) {
                        features.add(featurePrefix + "NW_Loc_Gzt");
                        break;
                    }
                }
            }*/
        }

        List<String> getSparseFeatures() {
            List<String> features = new ArrayList<>();
            features.add("CW:" + currentWord);
            features.add("NW:" + nextWord);
            features.add("2NW:" + nextWord2);
            features.add("PW:" + previousWord);
            features.add("2PW:" + previousWord2);

            wordFeatures(currentWordOrig, "CW", features);
            wordFeatures(previousWordOrig, "PW", features);
            //wordFeatures(previousWord2Orig, "PW2", features);
            wordFeatures(nextWordOrig, "NW", features);
            //wordFeatures(nextWord2Orig, "NW2", features);

            morphologicalFeatures(currentWordOrig, "CW", features);
            //morphologicalFeatures(previousWordOrig, "PW", features);
            //morphologicalFeatures(previousWord2Orig, "PW2", features);
            morphologicalFeatures(nextWordOrig, "NW", features);
            //morphologicalFeatures(nextWord2Orig, "NW2", features);


            String cwLast2 = currentWord.length() > 2 ? currentWord.substring(currentWord.length() - 2) : "";
            if (cwLast2.length() > 0) {
                features.add("CWLast2:" + cwLast2);
            }
            String cwLast3 = currentWord.length() > 3 ? currentWord.substring(currentWord.length() - 3) : "";
            if (cwLast3.length() > 0) {
                features.add("CWLast3:" + cwLast3);
            }

            String cwFirst2 = currentWord.length() > 2 ? currentWord.substring(0, 2) : "";
            if (cwFirst2.length() > 0) {
                features.add("CWFirst2:" + cwFirst2);
            }
            String cwFirst3 = currentWord.length() > 3 ? currentWord.substring(0, 3) : "";
            if (cwFirst3.length() > 0) {
                features.add("CWFirst3:" + cwFirst3);
            }

            return features;
        }

        void wordFeatures(String word, String featurePrefix, List<String> features) {
            if (word == null) {
                return;
            }
            features.add(featurePrefix + "Upper:" + Character.isUpperCase(word.charAt(0)));
            features.add(featurePrefix + "Punct:" + (word.length() == 1));
            boolean allCap = true;
            for (char c : word.toCharArray()) {
                if (!Character.isUpperCase(c)) {
                    allCap = false;
                    break;
                }
            }
            features.add(featurePrefix + "AllCap:" + allCap);
            String s = TextUtil.normalizeApostrophes(word);
            int apostropheIndex = s.indexOf('\'');
            features.add(featurePrefix + "Apost:" + (apostropheIndex >= 0));
            if (apostropheIndex >= 0) {
                String stem = word.substring(0, apostropheIndex);
                String ending = word.substring(apostropheIndex + 1);
                features.add(featurePrefix + "Stem:" + stem);
                features.add(featurePrefix + "Ending:" + ending);
            }
        }
    }

    public NerDataSet test(NerDataSet set) {

        List<NerSentence> resultSentences = new ArrayList<>();

        for (NerSentence sentence : set.sentences) {
            List<NerToken> predictedTokens = new ArrayList<>();

            for (int i = 0; i < sentence.tokens.size(); i++) {

                NerToken currentToken = sentence.tokens.get(i);

                FeatureData data = new FeatureData(morphology, gazetteers, sentence, i);
                List<String> sparseInputs = data.getSparseFeatures();

                if (i > 0) {
                    sparseInputs.add("PreType=" + predictedTokens.get(i - 1).tokenId);
                }
                if (i > 1) {
                    sparseInputs.add("2PreType=" + predictedTokens.get(i - 2).tokenId);
                }
                if (i > 2) {
                    sparseInputs.add("3PreType=" + predictedTokens.get(i - 3).tokenId);
                }

                ScoredItem<String> predicted = predictTypeAndPosition(model, sparseInputs);

                NerToken predictedToken = NerToken.fromTypePositionString(
                        currentToken.index, currentToken.word, currentToken.normalized, predicted.item);
                predictedTokens.add(predictedToken);

            }
            NerSentence predictedSentence = new NerSentence(sentence.content, predictedTokens);
            resultSentences.add(predictedSentence);
        }
        return new NerDataSet(resultSentences);
    }

    static void testReport(NerDataSet reference, NerDataSet prediction, Path reportPath) throws IOException {

        try (PrintWriter pw = new PrintWriter(reportPath.toFile(), "UTF-8")) {
            List<NerSentence> testSentences = reference.sentences;
            for (int i = 0; i < testSentences.size(); i++) {
                NerSentence ts = testSentences.get(i);
                pw.println(ts.content);
                NerSentence ps = prediction.sentences.get(i);
                for (int j = 0; j < ts.tokens.size(); j++) {
                    NerToken tt = ts.tokens.get(j);
                    NerToken pt = ps.tokens.get(j);
                    if (tt.word.equals(tt.normalized)) {
                        pw.println(String.format("%s %s -> %s", tt.word, tt.tokenId, pt.tokenId));
                    } else {
                        pw.println(String.format("%s:%s %s -> %s", tt.word, tt.normalized, tt.tokenId, pt.tokenId));
                    }
                }
            }
            TestResult result = testLog(reference, prediction);
            pw.println(result.dump());
        }
    }

    public static class TestResult {
        int errorCount = 0;
        int tokenCount = 0;
        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        int testNamedEntityCount = 0;
        int correctNamedEntityCount = 0;

        double tokenErrorRatio() {
            return (errorCount * 1d) / tokenCount;
        }

        double tokenPresicion() {
            return (truePositives * 1d) / (truePositives + falsePositives);
        }

        //TODO: check this
        double tokenRecall() {
            return (truePositives * 1d) / (truePositives + falseNegatives);
        }

        double exactMatch() {
            return (correctNamedEntityCount * 1d) / testNamedEntityCount;
        }

        String dump() {
            List<String> lines = new ArrayList<>();
            lines.add(String.format("Token Error ratio   = %.6f", tokenErrorRatio()));
            Log.info(String.format("NE Token Precision  = %.6f", tokenPresicion()));
            Log.info(String.format("NE Token Recall     = %.6f", tokenRecall()));
            Log.info(String.format("Exact NER match     = %.6f", exactMatch()));
            return String.join(" ", lines);
        }

    }

    static TestResult testLog(NerDataSet reference, NerDataSet prediction) {

        int errorCount = 0;
        int tokenCount = 0;
        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        int testNamedEntityCount = 0;
        int correctNamedEntityCount = 0;


        List<NerSentence> testSentences = reference.sentences;
        for (int i = 0; i < testSentences.size(); i++) {
            NerSentence ts = testSentences.get(i);
            NerSentence ps = prediction.sentences.get(i);
            for (int j = 0; j < ts.tokens.size(); j++) {
                NerToken tt = ts.tokens.get(j);
                NerToken pt = ps.tokens.get(j);
                if (!tt.tokenId.equals(pt.tokenId)) {
                    errorCount++;
                    if (tt.position == NePosition.OUTSIDE) {
                        falsePositives++;
                    }
                    if (pt.position == NePosition.OUTSIDE) {
                        falseNegatives++;
                    }
                } else {
                    if (tt.position != NePosition.OUTSIDE) {
                        truePositives++;
                    }
                }
                tokenCount++;
            }
            List<NamedEntity> namedEntities = ts.getNamedEntities();
            testNamedEntityCount += namedEntities.size();
            correctNamedEntityCount += ps.matchingNEs(namedEntities).size();
        }

        TestResult result = new TestResult();
        result.correctNamedEntityCount = correctNamedEntityCount;
        result.errorCount = errorCount;
        result.falseNegatives = falseNegatives;
        result.falsePositives = falsePositives;
        result.testNamedEntityCount = testNamedEntityCount;
        result.tokenCount = tokenCount;
        result.truePositives = truePositives;
        return result;
    }

    public static void main(String[] args) throws IOException {
        //Path trainPath = Paths.get("experiment/src/main/resources/ner/reyyan.train.txt");
        Path trainPath = Paths.get("experiment/src/main/resources/ner/NE-bracket.train.txt");
        NerDataSet trainingSet = NerDataSet.loadBracketTurkishCorpus(trainPath);
        new NerDataSet.Info(trainingSet).log();

        //Path testPath = Paths.get("experiment/src/main/resources/ner/reyyan.test.txt");
        Path testPath = Paths.get("experiment/src/main/resources/ner/NE-bracket.test.txt");
        NerDataSet testSet = NerDataSet.loadBracketTurkishCorpus(testPath);
        new NerDataSet.Info(testSet).log();

        Gazetteers gazetteers = new Gazetteers(
                Paths.get("experiment/src/main/resources/ner/location-words.txt"),
                Paths.get("experiment/src/main/resources/ner/organization-words.txt")
        );

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        Map<String, ClassWeights> model = PerceptronNer.train(morphology, gazetteers, trainingSet, testSet, 10, 0.1f);

        PerceptronNer ner = new PerceptronNer(model, morphology, gazetteers);

        Log.info("Testing %d sentences.", testSet.sentences.size());
        NerDataSet testResult = ner.test(testSet);

        testReport(testSet, testResult, Paths.get("experiment/src/main/resources/ner/test-result.txt"));
        Log.info("Done.");
    }
}
