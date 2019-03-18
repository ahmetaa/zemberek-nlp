package zemberek.morphology.ambiguity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import zemberek.langid.LanguageIdentifier;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class AmbiguityScriptsBase {

  LanguageIdentifier identifier;
  Collection<Predicate<WordAnalysis>> acceptWordPredicates = new ArrayList<>();
  Collection<Predicate<String>> ignoreSentencePredicates = new ArrayList<>();
  TurkishMorphology morphology;

  public AmbiguityScriptsBase() throws IOException {
    try {
      identifier = LanguageIdentifier.fromInternalModelGroup("tr_group");
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.morphology = TurkishMorphology.create(RootLexicon.builder()
        .addTextDictionaryResources(
            "tr/master-dictionary.dict",
            "tr/non-tdk.dict",
            "tr/proper.dict",
            "tr/proper-from-corpus.dict",
            "tr/abbreviations.dict",
            "tr/person-names.dict"
        ).build());
  }

  Predicate<WordAnalysis> hasAnalysis() {
    return WordAnalysis::isCorrect;
  }

  Predicate<WordAnalysis> maxAnalysisCount(int i) {
    return p -> p.analysisCount() <= i;
  }

  Predicate<String> contains(String s) {
    return p -> p.contains(s);
  }

  Predicate<String> probablyNotTurkish() {
    return p -> !identifier.identify(p).equals("tr");
  }

  Predicate<String> tooMuchNumberAndPunctuation(int k) {
    return p -> TurkishTokenizer.DEFAULT.tokenize(p).stream()
        .filter(s -> s.getType() == Token.Type.Punctuation || s.getType() == Token.Type.Number)
        .count() > k;
  }

  Predicate<String> tooLongSentence(int tokenCount) {
    return p -> p.split("[ ]+").length > tokenCount;
  }

  LinkedHashSet<String> getAccpetableSentences(List<String> strings) {
    List<String> normalized = new ArrayList<>();
    for (String sentence : strings) {
      sentence = sentence.replaceAll("\\s+|\\u00a0+|\\u200b+", " ");
      sentence = sentence.replaceAll("[\\u00ad]", "");
      normalized.add(sentence);
    }

    LinkedHashSet<String> toProcess = new LinkedHashSet<>();
    for (String s : normalized) {
      boolean ok = true;
      for (Predicate<String> ignorePredicate : ignoreSentencePredicates) {
        if (ignorePredicate.test(s)) {
          ok = false;
          break;
        }
      }
      if (ok) {
        toProcess.add(s);
      }
    }
    return toProcess;
  }

  List<List<String>> group(List<String> lines, int blockCount) {
    List<List<String>> result = new ArrayList<>();
    if (lines.size() <= blockCount) {
      result.add(new ArrayList<>(lines));
      return result;
    }
    int start = 0;
    int end = start + blockCount;
    while (end < lines.size()) {
      result.add(new ArrayList<>(lines.subList(start, end)));
      start = end;
      end = start + blockCount;
      if (end >= lines.size()) {
        end = lines.size();
        ArrayList<String> l = new ArrayList<>(lines.subList(start, end));
        if (l.size() > 0) {
          result.add(l);
        }
        break;
      }
    }
    return result;
  }

  LinkedHashSet<String> getSentences(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8).stream()
        .filter(s -> !s.startsWith("<"))
        .collect(Collectors.toList());
    return new LinkedHashSet<>(TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines));
  }
}
