package zemberek.morphology.ambiguity;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import zemberek.core.collections.IntValueMap;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Based on "Haşim Sak, Tunga Güngör, and Murat Saraçlar. Morphological disambiguation of Turkish text with perceptron algorithm.
 * In CICLing 2007, volume LNCS 4394, pages 107-118, 2007"
 * Original Perl implementation is from <a href="http://www.cmpe.boun.edu.tr/~hasim">Haşim Sak</a>
 */
public class AveragedPerceptronMorphDisambiguator extends AbstractDisambiguator {

    Model weights = new Model();
    Model averagedWeights = new Model();
    IntValueMap<String> counts = new IntValueMap<>();

    Random random = new Random(1);

    public AveragedPerceptronMorphDisambiguator(File modelFile) throws IOException {
        this.averagedWeights = Model.loadFromTextFile(modelFile);
    }

    private AveragedPerceptronMorphDisambiguator() {
        this.averagedWeights = new Model(new HashMap<String, Double>());
    }

    static void train(File trainFile, File modelFile) throws IOException {
        AveragedPerceptronMorphDisambiguator disambiguator = new AveragedPerceptronMorphDisambiguator();
        DataSet trainingSet = com.google.common.io.Files.readLines(trainFile, Charsets.UTF_8, new DataSetLoader());
        int numExamples = 0;
        for (int i = 0; i < 4; i++) {
            System.out.println("Iteration:" + i);
            for (SentenceData sentence : trainingSet) {
                numExamples++;
                ParseResult result = disambiguator.bestParse(sentence, false);
                if (sentence.correctParse.equals(result.bestParse))
                    continue;
                IntValueMap<String> correctFeatures = disambiguator.extractFeatures(sentence.correctParse);
                IntValueMap<String> bestFeatures = disambiguator.extractFeatures(result.bestParse);
                disambiguator.updateWeights(correctFeatures, bestFeatures, numExamples);
            }
            for (String key : disambiguator.averagedWeights) {
                disambiguator.updateAverageWeights(numExamples, key);
            }
        }
        disambiguator.averagedWeights.saveAsText(modelFile);
    }

    public void test(File testFile) throws IOException {
        DataSet testSet = com.google.common.io.Files.readLines(testFile, Charsets.UTF_8, new DataSetLoader());
        int hit = 0, total = 0;
        Stopwatch sw = Stopwatch.createStarted();
        for (SentenceData sentence : testSet.sentences) {
            ParseResult result = bestParse(sentence, true);
            int i = 0;
            for (String best : result.bestParse) {
                if (sentence.correctParse.get(i).equals(best)) {
                    hit++;
                }
                total++;
                i++;
            }
        }
        System.out.println("Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("total:" + total + " hit=" + hit + String.format(" Accuracy:%f", (double) hit / total));
    }

    private void updateWeights(IntValueMap<String> correctFeatures, IntValueMap<String> bestFeatures, int numExamples) {
        Set<String> keySet = Sets.newHashSet();
        keySet.addAll(Lists.newArrayList(correctFeatures));
        keySet.addAll(Lists.newArrayList(bestFeatures));

        for (String feat : keySet) {
            updateAverageWeights(numExamples, feat);
            weights.increment(feat, correctFeatures.get(feat) - bestFeatures.get(feat));
            if (averagedWeights.weight(feat) == 0)
                averagedWeights.data.remove(feat);
            if (weights.weight(feat) == 0)
                weights.data.remove(feat);
        }
    }

    private void updateAverageWeights(int numExamples, String feat) {
        int featureCount = counts.get(feat);
        averagedWeights.put(
                feat,
                (averagedWeights.weight(feat) * featureCount + (numExamples - featureCount) * weights.weight(feat))
                        / numExamples);
        counts.put(feat, numExamples);
    }

    IntValueMap<String> extractFeatures(List<String> parseSequence) {
        List<String> seq = Lists.newArrayList("<s>", "<s>");
        seq.addAll(parseSequence);
        seq.add("</s>");
        IntValueMap<String> featureModel = new IntValueMap<>();
        for (int i = 2; i < seq.size(); i++) {
            List<String> trigram = Lists.newArrayList(
                    seq.get(i - 2),
                    seq.get(i - 1),
                    seq.get(i)
            );
            extractTrigramFeatures(trigram, featureModel);
        }
        return featureModel;
    }

    void extractTrigramFeatures(List<String> trigram, IntValueMap<String> feats) {
        WordParse w1 = new WordParse(trigram.get(0));
        WordParse w2 = new WordParse(trigram.get(1));
        WordParse w3 = new WordParse(trigram.get(2));
        String r1 = w1.root;
        String r2 = w2.root;
        String r3 = w3.root;
        String ig1 = w1.allIgs;
        String ig2 = w2.allIgs;
        String ig3 = w3.allIgs;

        //feats.increment1(format("1:%s%s-%s%s-%s%s", r1, ig1, r2, ig2, r3, ig3));
        feats.addOrIncrement(format("2:%s%s-%s%s", r1, ig2, r3, ig3));
        feats.addOrIncrement(format("3:%s%s-%s%s", r2, ig2, r3, ig3));
        feats.addOrIncrement(format("4:%s%s", r3, ig3));
        //feats.increment1(format("5:%s%s-%s", r2, ig2, ig3));
        //feats.increment1(format("6:%s%s-%s", r1, ig1, ig3));
        //feats.increment1(format("7:%s-%s-%s", r1, r2, r3));
        //feats.increment1(format("8:%s-%s", r1, r3));
        feats.addOrIncrement(format("9:%s-%s", r2, r3));
        feats.addOrIncrement(format("10:%s", r3));
        //feats.increment1(format("11:%s-%s-%s", ig1, ig2, ig3));
        //feats.increment1(format("12:%s-%s", ig1, ig3));
        //feats.increment1(format("13:%s-%s", ig2, ig3));
        //feats.increment1(format("14:%s", ig3));

        String ig1s[] = ig1.split("[ ]");
        String ig2s[] = ig2.split("[ ]");
        String ig3s[] = ig3.split("[ ]");

        for (String ig : ig3s) {
            feats.addOrIncrement(format("15:%s-%s-%s", ig1s[ig1s.length - 1], ig2s[ig2s.length - 1], ig));
          //  feats.increment1(format("16:%s-%s", ig1s[ig1s.length - 1], ig));
            feats.addOrIncrement(format("17:%s-%s", ig2s[ig2s.length - 1], ig));
           // feats.increment1(format("18:%s", ig));
        }

//        for (int k = 0; k < ig3s.length - 1; k++)
//            feats.increment1(format("19:%s-%s", ig3s[k], ig3s[k + 1]));

        for (int k = 0; k < ig3s.length; k++)
            feats.addOrIncrement(format("20:%d-%s", k, ig3s[k]));

//        if (Character.isUpperCase(r3.charAt(0)) && w3.igs.contains("Prop"))
//            feats.increment1("21:PROPER");

        feats.addOrIncrement(format("22:%d", ig3s.length));
/*        if (w3.all.contains(".+Punc") && w3.igs.contains("Verb"))
            feats.increment1("23:ENDSVERB");*/

    }

    static class Model implements Iterable<String> {

        Map<String, Double> data;

        Model(Map<String, Double> data) {
            this.data = data;
        }

        Model() {
            data = Maps.newHashMap();
        }

        public static Model loadFromTextFile(File file) throws IOException {
            Map<String, Double> data = Maps.newHashMap();
            List<String> all = SimpleTextReader.trimmingUTF8Reader(file).asStringList();
            for (String s : all) {
                double weight = Double.parseDouble(Strings.subStringUntilFirst(s, " "));
                String key = Strings.subStringAfterFirst(s, " ");
                data.put(key, weight);
            }
            return new Model(data);
        }

        public void saveAsText(File file) throws IOException {
            SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(file);
            for (String s : data.keySet()) {
                stw.writeLine(data.get(s) + " " + s);
            }
            stw.close();
        }

        double weight(String key) {
            return data.containsKey(key) ? data.get(key) : 0;
        }

        void put(String key, Double value) {
            this.data.put(key, value);
        }

        double increment(String key, double value) {
            Double val = data.get(key);
            if (val == null) {
                data.put(key, value);
                return value;
            } else data.put(key, val + value);
            return val + value;
        }

        @Override
        public Iterator<String> iterator() {
            return data.keySet().iterator();
        }

    }

    //represents a state in Viterbi search.
    class State {
        int previous;
        double score;
        String parse;

        State(int previous, double score, String parse) {
            this.previous = previous;
            this.score = score;
            this.parse = parse;
        }
    }

    // represents the ID of the state
    class StateId {
        String first;
        String second;

        StateId(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StateId stateId = (StateId) o;

            if (!first.equals(stateId.first)) return false;
            if (!second.equals(stateId.second)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }
    }

    /**
     * Calculates the best path using Viterbi decoding.
     * @param sentence sentece with ambiguous wrods.
     * @param useAveragedWeights if true, average weights are used for scoring, else, normal weights are used.
     * @return best parse sequence and its score.
     */
    ParseResult bestParse(SentenceData sentence, boolean useAveragedWeights) {

        sentence.words.add(WordData.SENTENCE_END);
        Map<StateId, Integer> stateIds = Maps.newHashMap();
        Map<Integer, State> bestPath = Maps.newHashMap();

        // initial path and state
        bestPath.put(0, new State(-1, 0, null));
        stateIds.put(new StateId("<s>", "<s>"), 0);

        int bestStateNum = 0;
        double bestScore = -100000;
        int n = 0;
        for (WordData word : sentence.words) {
            Map<StateId, Integer> nextStates = Maps.newHashMap();
            // shuffle the parses for randomness.
            List<String> allParses = Lists.newArrayList(word.allParses);
            Collections.shuffle(allParses, random);
            bestScore = -100000;
            for (String parse : allParses) {

                for (StateId id : stateIds.keySet()) {
                    int stateNum = stateIds.get(id);
                    State st = bestPath.get(stateNum);
                    List<String> trigram = Lists.newArrayList(
                            id.first,
                            id.second,
                            parse
                    );

                    IntValueMap<String> features = new IntValueMap<>();
                    extractTrigramFeatures(trigram, features);

                    double trigramScore = 0;
                    for (String key : features) {
                        if (useAveragedWeights)
                            trigramScore += averagedWeights.weight(key) * features.get(key);
                        else
                            trigramScore += weights.weight(key) * features.get(key);
                    }

                    double newScore = trigramScore + st.score;

                    StateId newStateId = new StateId(id.second, parse);
                    if (!nextStates.containsKey(newStateId))
                        nextStates.put(newStateId, ++n);

                    // Viterbi path selection
                    int nextStateNum = n;
                    if (bestPath.containsKey(nextStateNum)) {
                        State s = bestPath.get(nextStateNum);
                        if (newScore > s.score)
                            bestPath.put(nextStateNum, new State(stateNum, newScore, parse));
                    } else {
                        bestPath.put(nextStateNum, new State(stateNum, newScore, parse));
                    }

                    if (newScore > bestScore) {
                        bestScore = newScore;
                        bestStateNum = nextStateNum;
                    }
                }
            }
            stateIds = nextStates;
        }

        LinkedList<String> best = Lists.newLinkedList();
        int stateNum = bestStateNum;
        while (stateNum > 0) {
            State s = bestPath.get(stateNum);
            best.addFirst(s.parse);
            stateNum = s.previous;
        }
        best.removeLast();
        return new ParseResult(best, bestScore);
    }

    private static class ParseResult {
        List<String> bestParse;
        double score;

        private ParseResult(LinkedList<String> bestParse, double score) {
            this.bestParse = bestParse;
            this.score = score;
        }
    }

    public static void main(String[] args) throws IOException {



    }
}
