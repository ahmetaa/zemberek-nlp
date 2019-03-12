package zemberek.embedding;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.ScoredItem;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class DistanceBasedStemmer {

  static Locale TR = new Locale("tr");
  static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
  Map<String, WordVector> vectorMap;
  TurkishMorphology morphology;
  DistanceList distances;

  public DistanceBasedStemmer(
      Map<String, WordVector> vectorMap,
      DistanceList distances,
      TurkishMorphology morphology) throws IOException {
    this.vectorMap = vectorMap;
    this.morphology = morphology;
    this.distances = distances;
  }

  public static DistanceBasedStemmer load(Path vector, Path distances, Path vocabFile)
      throws IOException {
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

  static String normalize(String input) {
    String s = alphabet.normalize(input.toLowerCase(TR).replaceAll("'", ""));
    return alphabet.normalizeCircumflex(s);
  }

  public static void main(String[] args) throws IOException {
    Path root = Paths.get("/home/ahmetaa/data/vector");
    Path binVectorFile = root.resolve("model-large-min10.vec.bin");
    Path vocabFile = root.resolve("vocab-large-min10.bin");
    Path distanceFile = root.resolve("distance-large-min10.bin");
    DistanceBasedStemmer experiment = DistanceBasedStemmer
        .load(binVectorFile, distanceFile, vocabFile);
    String input;
    System.out.println("Enter sentence:");
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

  public float distance(String a, String b) {
    if (!vectorMap.containsKey(a) || !vectorMap.containsKey(b)) {
      return 0;
    }
    return vectorMap.get(a).cosDistance(vectorMap.get(b));
  }

  public float totalDistance(String a, List<String> b) {
    float score = 0;
    for (String s : b) {
      score += distance(a, s);
    }
    return score;
  }

  public void findStems(String str) {
    str = "<s> <s> " + str + " </s> </s>";
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(str);
    List<SentenceWordAnalysis> swaList = analysis.getWordAnalyses();

    for (int i = 2; i < analysis.size() - 2; i++) {

      SentenceWordAnalysis swa = swaList.get(i);

      String s = swaList.get(i).getWordAnalysis().getInput();
      List<String> bigramContext = Lists.newArrayList(
          normalize(swaList.get(i - 1).getWordAnalysis().getInput()),
          normalize(swaList.get(i - 2).getWordAnalysis().getInput()),
          normalize(swaList.get(i + 1).getWordAnalysis().getInput()),
          normalize(swaList.get(i + 2).getWordAnalysis().getInput()));

      List<String> unigramContext = Lists.newArrayList(
          normalize(swaList.get(i - 1).getWordAnalysis().getInput()),
          normalize(swaList.get(i + 1).getWordAnalysis().getInput()));

      WordAnalysis wordResults = swa.getWordAnalysis();
      Set<String> stems = wordResults.stream()
          .map(a -> normalize(a.getDictionaryItem().lemma))
          .collect(Collectors.toSet());
      List<ScoredItem<String>> scores = new ArrayList<>();
      for (String stem : stems) {
        if (!distances.containsWord(stem)) {
          Log.info("Cannot find %s in vocab.", stem);
          continue;
        }
        List<WordDistances.Distance> distances = this.distances.getDistance(stem);
        float score = totalDistance(stem, bigramContext);
        int k = 0;
        for (WordDistances.Distance distance : distances) {
/*                    if (s.equals(distance.word)) {
                        continue;
                    }*/
          score += distance(s, distance.word);
          if (k++ == 10) {
            break;
          }
        }
        scores.add(new ScoredItem<>(stem, score));
      }
      Collections.sort(scores);
      Log.info("%n%s : ", s);
      for (ScoredItem<String> score : scores) {
        Log.info("Lemma = %s Score = %.7f", score.item, score.score);
      }
    }

    Log.info("==== Z disambiguation result ===== ");

    for (SentenceWordAnalysis a : analysis) {
      Log.info("%n%s : ", a.getWordAnalysis().getInput());
      LinkedHashSet<String> items = new LinkedHashSet<>();
      for (SingleAnalysis wa : a.getWordAnalysis()) {
        items.add(wa.getDictionaryItem().toString());
      }
      for (String item : items) {
        Log.info("%s", item);
      }
    }
  }
}
