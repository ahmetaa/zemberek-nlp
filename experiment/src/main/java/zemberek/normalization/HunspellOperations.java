package zemberek.normalization;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;


public class HunspellOperations {

  public static void main(String[] args) throws IOException {
    Path vocabFile = Paths.get("data/vocabulary/yapmak.txt");
    Path vocabFiltered = Paths.get("data/vocabulary/vocab.txt");
    Path annotationsPath = Paths.get("data/vocabulary/annotations.txt");
    //filterVocab(vocabFile, vocabFiltered);
    //generateAnnotationFileMultiSplit(vocabFiltered, annotationsPath);
    countModelTokens();
  }

  private static void filterVocab(Path vocabFile, Path outFile) throws IOException {
    List<String> words = Files.readAllLines(vocabFile, StandardCharsets.UTF_8);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<String> result = new ArrayList<>();
    for (String word : words) {
      WordAnalysis analysis = morphology.analyze(word);
      if (!analysis.isCorrect()) {
        Log.warn("Cannot analyze %s", word);
        continue;
      }
      result.add(word);
    }
    Files.write(outFile, result, StandardCharsets.UTF_8);
  }

  public static void countModelTokens() throws IOException {
    Path path = Paths.get("/home/aaa/projects/morfessor/model.txt");
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    System.out.println(lines.size());
    LinkedHashSet<String> tokens = new LinkedHashSet<>();
    for (String s : lines) {
      if (s.startsWith("#")) {
        continue;
      }
      List<String> strings = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(s);
      String root = strings.get(1);
      List<String> endingTokens = strings.subList(2, strings.size()).stream()
          .filter(k -> !k.equals("+")).collect(Collectors.toList());
      String ending = String.join("", endingTokens);
      tokens.add(root);
      if (ending.length() > 0) {
        tokens.add(ending);
      }
    }
    System.out.println(tokens.size());
  }

  private static boolean isCorrectAndContainsNoProper(WordAnalysis analysis) {
    if (!analysis.isCorrect()) {
      return false;
    }
    for (SingleAnalysis s : analysis) {
      if (s.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun &&
          s.getDictionaryItem().secondaryPos != SecondaryPos.Abbreviation) {
        return true;
      }
    }
    return false;
  }

  private static void generateAnnotationFileMultiSplit(Path vocab, Path annotationsPath)
      throws IOException {
    List<String> words = Files.readAllLines(vocab, StandardCharsets.UTF_8);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<String> annotations = new ArrayList<>();
    for (String word : words) {
      WordAnalysis analysis = morphology.analyze(word);
      if (!analysis.isCorrect()) {
        Log.warn("Cannot analyze %s", word);
        continue;
      }
      LinkedHashSet<String> stemEndings = new LinkedHashSet<>();
      for (SingleAnalysis s : analysis) {
        if (s.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun ||
            s.getDictionaryItem().secondaryPos == SecondaryPos.Abbreviation) {
          continue;
        }
        String surfaces = AnalysisFormatters.SURFACE_SEQUENCE.format(s);
        List<String> tokens = Splitter.on(" ").splitToList(surfaces);

        String stem = tokens.get(0);

        for (int i = 0; i < tokens.size(); i++) {
          String morpheme = tokens.get(i);
          if (i > 0) {
            stem = stem + morpheme;
          }
          List<String> morphemes =
              i == tokens.size() - 1 ? new ArrayList<>() : tokens.subList(i + 1, tokens.size());
          String ending = String.join(" ", morphemes);
          if (isCorrectAndContainsNoProper(morphology.analyze(stem))) {
            if (ending.length() > 0) {
              stemEndings.add(word + " " + stem + " " + ending);
            } /*else {
              stemEndings.add(word + " " + stem);
            }*/
          }
        }
      }
      annotations.add(String.join(",", stemEndings));
    }
    Files.write(annotationsPath, annotations, StandardCharsets.UTF_8);
  }

  private static void generateAnnotationFileSingleSplit(Path vocab) throws IOException {
    List<String> words = Files.readAllLines(vocab, StandardCharsets.UTF_8);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<String> annotations = new ArrayList<>();
    for (String word : words) {
      WordAnalysis analysis = morphology.analyze(word);
      if (!analysis.isCorrect()) {
        Log.warn("Cannot analyze %s", word);
        continue;
      }
      LinkedHashSet<String> stemEndings = new LinkedHashSet<>();
      for (SingleAnalysis s : analysis) {
        if (s.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun ||
            s.getDictionaryItem().secondaryPos == SecondaryPos.Abbreviation) {
          continue;
        }
        List<String> stems = s.getStems();
        for (String stem : stems) {
          String ending = word.substring(stem.length());
          if (!(stem + ending).equals(word)) {
            Log.warn("Stem + Ending %s+%s does not match word %s", stem, ending, word);
            continue;
          }
          if (ending.length() > 0) {
            stemEndings.add(word + " " + stem + " " + ending);
          } else {
            stemEndings.add(word + " " + stem);
          }
        }
      }
      annotations.add(String.join(",", stemEndings));
    }
    Files.write(Paths.get("data/vocabulary/annonations.txt"), annotations, StandardCharsets.UTF_8);
  }


}
