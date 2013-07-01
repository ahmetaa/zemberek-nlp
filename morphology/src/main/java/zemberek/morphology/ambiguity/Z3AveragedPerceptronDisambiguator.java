package zemberek.morphology.ambiguity;

import com.google.common.collect.Maps;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;

public class Z3AveragedPerceptronDisambiguator extends Z3AbstractDisambiguator {

    Model weights = new Model();
    Model averagedWeights = new Model();
    CountMap counts = new CountMap();

    Random random = new Random(1);

    public Z3AveragedPerceptronDisambiguator(File modelFile) throws IOException {
        this.averagedWeights = Model.loadFromTextFile(modelFile);
    }

    private Z3AveragedPerceptronDisambiguator() {
        this.averagedWeights = new Model(new HashMap<String, Double>());
    }

/*    static void train(File trainFile, File modelFile) throws IOException {
        Z3AveragedPerceptronDisambiguator disambiguator = new Z3AveragedPerceptronDisambiguator();
        DataSet trainingSet = com.google.common.io.Files.readLines(trainFile, Charsets.UTF_8, new DataSetLoader());
        int numExamples = 0;
        for (int i = 0; i < 4; i++) {
            System.out.println("Iteration:" + i);
            for (SentenceData sentence : trainingSet) {
                numExamples++;
                ParseResult result = disambiguator.bestParse(sentence, false);
                if (sentence.correctParse.equals(result.bestParse))
                    continue;
                CountMap correctFeatures = disambiguator.extractFeatures(sentence.correctParse);
                CountMap bestFeatures = disambiguator.extractFeatures(result.bestParse);
                disambiguator.updateWeights(correctFeatures, bestFeatures, numExamples);
            }
            for (String key : disambiguator.averagedWeights) {
                disambiguator.updateAverageWeights(numExamples, key);
            }
        }
        disambiguator.averagedWeights.saveAsText(modelFile);
    }*/

    void extractTrigramFeatures(List<String> trigram, CountMap feats) {
        AbstractDisambiguator.WordParse w1 = new AbstractDisambiguator.WordParse(trigram.get(0));
        AbstractDisambiguator.WordParse w2 = new AbstractDisambiguator.WordParse(trigram.get(1));
        AbstractDisambiguator.WordParse w3 = new AbstractDisambiguator.WordParse(trigram.get(2));
        String r1 = w1.root;
        String r2 = w2.root;
        String r3 = w3.root;
        String ig1 = w1.allIgs;
        String ig2 = w2.allIgs;
        String ig3 = w3.allIgs;

        feats.increment(format("1:%s%s-%s%s-%s%s", r1, ig1, r2, ig2, r3, ig3));
        feats.increment(format("2:%s%s-%s%s", r1, ig2, r3, ig3));
        feats.increment(format("3:%s%s-%s%s", r2, ig2, r3, ig3));
        feats.increment(format("4:%s%s", r3, ig3));
        feats.increment(format("5:%s%s-%s", r2, ig2, ig3));
        feats.increment(format("6:%s%s-%s", r1, ig1, ig3));
        feats.increment(format("7:%s-%s-%s", r1, r2, r3));
        feats.increment(format("8:%s-%s", r1, r3));
        feats.increment(format("9:%s-%s", r2, r3));
        feats.increment(format("10:%s", r3));
        feats.increment(format("11:%s-%s-%s", ig1, ig2, ig3));
        feats.increment(format("12:%s-%s", ig1, ig3));
        feats.increment(format("13:%s-%s", ig2, ig3));
        feats.increment(format("14:%s", ig3));

        String ig1s[] = ig1.split("[ ]");
        String ig2s[] = ig2.split("[ ]");
        String ig3s[] = ig3.split("[ ]");

        for (String ig : ig3s) {
            feats.increment(format("15:%s-%s-%s", ig1s[ig1s.length - 1], ig2s[ig2s.length - 1], ig));
            feats.increment(format("16:%s-%s", ig1s[ig1s.length - 1], ig));
            feats.increment(format("17:%s-%s", ig2s[ig2s.length - 1], ig));
            feats.increment(format("18:%s", ig));
        }

        for (int k = 0; k < ig3s.length - 1; k++)
            feats.increment(format("19:%s-%s", ig3s[k], ig3s[k + 1]));

        for (int k = 0; k < ig3s.length; k++)
            feats.increment(format("20:%d-%s", k, ig3s[k]));

        if (Character.isUpperCase(r3.charAt(0)) && w3.igs.contains("Prop"))
            feats.increment("21:PROPER");

        feats.increment(format("22:%d", ig3s.length));
        if (w3.all.contains(".+Punc") && w3.igs.contains("Verb"))
            feats.increment("23:ENDSVERB");

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

    static class CountMap implements Iterable<String> {

        Map<String, Integer> data = Maps.newHashMap();

        int get(String key) {
            return data.containsKey(key) ? data.get(key) : 0;
        }

        double increment(String key) {
            return increment(key, 1);
        }

        void put(String key, Integer value) {
            this.data.put(key, value);
        }

        double increment(String key, int value) {
            Integer val = data.get(key);
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

}
