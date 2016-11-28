package zemberek.embedding;

import com.google.common.base.Splitter;
import zemberek.core.ScoredItem;
import zemberek.core.logging.Log;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DistanceBasedStemmer {

    Map<String, WordVector> vectorMap;
    TurkishMorphology morphology;
    DistanceList experiment;

    public DistanceBasedStemmer(
            Map<String, WordVector> vectorMap,
            DistanceList experiment,
            TurkishMorphology morphology) {
        this.vectorMap = vectorMap;
        this.morphology = morphology;
        this.experiment = experiment;
    }

    public static DistanceBasedStemmer load(Path vector, Path distances, Path vocabFile) throws IOException {
        Log.info("Loading vector file.");
        List<WordVector> wordVectors = WordVector.loadFromBinary(vector);
        Map<String, WordVector> map = new HashMap<>(wordVectors.size());
        for (WordVector wordVector : wordVectors) {
            map.put(wordVector.word, wordVector);
        }
        Log.info("Loading distances.");
        DistanceList experiment = DistanceList.readFromBinary(distances, vocabFile);
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        return new DistanceBasedStemmer(map, experiment, morphology);
    }

    public float distance(String a, String b) {
        if (!vectorMap.containsKey(a) || !vectorMap.containsKey(b)) {
            return 0;
        }
        return vectorMap.get(a).cosDistance(vectorMap.get(b));
    }

    public void findStems(String str) {
        str = "<s> <s> " + str + " </s> </s>";
        List<String> tokens = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(str);
        for (int i = 2; i < tokens.size() - 2; i++) {
            String s = tokens.get(i);
            String before = tokens.get(i - 1);
            String before2 = tokens.get(i - 2);
            String after = tokens.get(i + 1);
            String after2 = tokens.get(i + 2);
            Set<String> lemmas = new HashSet<>();
            List<WordAnalysis> result = morphology.analyze(s);
            for (WordAnalysis analysis : result) {
                //List<String> ll = analysis.getLemmas();
                lemmas.add(analysis.getLemma());
            }
            List<ScoredItem<String>> scores = new ArrayList<>();
            float original = distance(s, after) + distance(s, before);
            for (String lemma : lemmas) {
                if(!experiment.containsWord(lemma)) {
                    Log.info("Cannot find %s in vocab.", lemma);
                    continue;
                }
                List<WordDistances.Distance> distances = experiment.getDistance(lemma);
                float score = 0;
                int k = 0;
                for (WordDistances.Distance distance : distances) {
                    score += distance(distance.word, before);
                    score += distance(distance.word, after);
                    score += distance(distance.word, after2);
                    score += distance(distance.word, before2);
                    if(k++==3) {
                        break;
                    }
                }
                scores.add(new ScoredItem<>(lemma, score));
            }
            Collections.sort(scores);
            Log.info("%n%s : ", s);
            for (ScoredItem<String> score : scores) {
                Log.info("Lemma = %s Score = %.7f", score.item, score.score);
            }

        }
    }


    public static void main(String[] args) throws IOException {
        Path binVectorFile = Paths.get("/home/ahmetaa/data/vector/model-large-min10.vec.bin");
        Path vocabFile = Paths.get("/home/ahmetaa/data/vector/vocab-large-min10.bin");
        Path distanceFile = Paths.get("/home/ahmetaa/data/vector/distance-10.bin");
        DistanceBasedStemmer experiment = DistanceBasedStemmer.load(binVectorFile, distanceFile, vocabFile);
        String input;
        System.out.println("Enter word:");
        Scanner sc = new Scanner(System.in);
        input = sc.nextLine();
        while (!input.equals("exit") && !input.equals("quit")) {
            if (input.trim().length() == 0) {
                Log.info(input + " cannot be found.");
                input = sc.nextLine();
                continue;
            }
            experiment.findStems(input);
            input = sc.nextLine();
        }
    }
}
