package zemberek.embedding;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

public class WordSimilarityConsole {

  public static void main(String[] args) throws Exception {
    Path root = Paths.get("/media/ahmetaa/depo/data/zemberek/data/out");
    String id = "open-subtitles";
    Path vectorFile = root.resolve(id + ".vec");
    Path binVectorFile = root.resolve(id + ".vec.bin");
    WordVectorLookup.loadFromText(vectorFile, true).saveToFolder(root, id);

    Path vocabFile = root.resolve(id + ".vocab");
    new WordSimilarityConsole().run(binVectorFile, vocabFile);
  }

  void run(Path vectorFile, Path vocabFile) throws IOException {

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    System.out.println("Loading from " + vectorFile);
    WordVectorLookup lookup = WordVectorLookup.loadFromBinaryFast(vectorFile, vocabFile);
    WordVectorLookup.DistanceMatcher distanceMatcher = new WordVectorLookup.DistanceMatcher(lookup);
    String input;
    System.out.println("Enter word:");
    Scanner sc = new Scanner(System.in);
    input = sc.nextLine();
    while (!input.equals("exit") && !input.equals("quit")) {
      if (!lookup.containsWord(input)) {
        Log.info(input + " cannot be found.");
        input = sc.nextLine();
        continue;
      }
      List<WordDistances.Distance> distances = distanceMatcher.nearestK(input, 30);

      List<String> dist = new ArrayList<>(distances.size());
      dist.addAll(distances.stream().map(d -> d.word).collect(Collectors.toList()));
      System.out.println(String.join(" ", dist));

      List<String> noParse = new ArrayList<>();
      for (String s : dist) {
        WordAnalysis an = morphology.analyze(s);
        if (an.isCorrect() || (an.analysisCount() == 1
            && an.getAnalysisResults().get(0).getDictionaryItem().primaryPos == PrimaryPos.Unknown)) {
          noParse.add(s);
        }
      }
      System.out.println(String.join(" ", noParse));
      input = sc.nextLine();
    }
  }
}
