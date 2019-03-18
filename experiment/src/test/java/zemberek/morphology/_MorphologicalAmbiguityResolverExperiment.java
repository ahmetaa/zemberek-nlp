package zemberek.morphology;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.langid.LanguageIdentifier;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.core.turkish.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class _MorphologicalAmbiguityResolverExperiment {

  LanguageIdentifier identifier;
  Map<String, WordAnalysis> cache = new HashMap<>();
  Histogram<String> failedWords = new Histogram<>(100000);

  public _MorphologicalAmbiguityResolverExperiment() throws IOException {
    identifier = LanguageIdentifier.fromInternalModelGroup("tr_group");
  }

  public static void main(String[] args) throws IOException {
    Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/open-subtitles");
    Path outRoot = Paths.get("data/ambiguity");
    Files.createDirectories(outRoot);

    new _MorphologicalAmbiguityResolverExperiment()
        .extractData(p, outRoot, 2, 10000);
  }

  public void extractData(Path p, Path outRoot, int maxAnalysisCount, int resultLimit)
      throws IOException {
    List<Path> files = Files.walk(p, 1).filter(s -> s.toFile().isFile()
        && s.toFile().getName().endsWith(".corpus")).collect(Collectors.toList());
    LinkedHashSet<SingleAnalysisSentence> result = new LinkedHashSet<>();

    int i = 0;

    for (Path file : files) {
      List<SingleAnalysisSentence> collect = collect(file, maxAnalysisCount);
      result.addAll(collect);
      i++;
      Log.info("%d of %d", i, files.size());
      if (resultLimit > 0 && result.size() > resultLimit) {
        break;
      }
    }

    String s = p.toFile().getName();

    Path out = outRoot.resolve(s + "-ambigious.txt");

    try (PrintWriter pw = new PrintWriter(out.toFile(), "utf-8")) {
      for (SingleAnalysisSentence sentence : result) {
        pw.println(sentence.sentence);
        for (Single single : sentence.tokens) {
          for (SingleAnalysis r : single.res) {
            pw.println(single.input);
            pw.println(r.formatLong());
          }
        }
        pw.println();
      }
    }

    // saving failed words.
    failedWords.saveSortedByKeys(outRoot.resolve(s + "-failed.txt"), " ",
        Turkish.STRING_COMPARATOR_ASC);
    // saving failed words by frequency.
    failedWords.saveSortedByCounts(outRoot.resolve(s + "-failed.freq.txt"), " ");
  }

  Pattern ignore = Pattern.compile("[0-9.\\-]+");

  private List<SingleAnalysisSentence> collect(Path p, int maxAnalysisCount) throws IOException {
    List<String> sentences = getSentences(p);
    TurkishMorphology analyzer = TurkishMorphology.createWithDefaults();

    int tokenCount = 0;
    int sentenceCount = 0;

    List<SingleAnalysisSentence> result = new ArrayList<>();

    for (String sentence : sentences) {

      sentence = sentence.replaceAll("\\s+|\\u00a0", " ");
      sentence = sentence.replaceAll("[\\u00ad]", "");
      sentence = sentence.replaceAll("[…]", "...");
      List<Single> singleAnalysisWords = new ArrayList<>();
      List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
      boolean failed = false;
      int i = 0;
      for (Token token : tokens) {
        tokenCount++;
        String rawWord = token.getText();
        String word = Character.isUpperCase(rawWord.charAt(0)) ?
            Turkish.capitalize(rawWord) : rawWord.toLowerCase(Turkish.LOCALE);
        WordAnalysis results;
        if (cache.containsKey(word)) {
          results = cache.get(word);
        } else {
          results = analyzer.analyze(word);
          cache.put(word, results);
        }
        if (results.analysisCount() == 0) {
          if (Strings.containsNone(word, "0123456789-.")) {
            failedWords.add(word);
          }
        }
        if (results.analysisCount() < 1 || results.analysisCount() > maxAnalysisCount) {
          failed = true;
          break;
        } else {
          List<SingleAnalysis> filtered = results.stream()
              .filter(s -> !(s.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun &&
                  Character.isLowerCase(rawWord.charAt(0)))).collect(Collectors.toList());

          if (filtered.size() == 0) {
            failed = true;
            break;
          }
          singleAnalysisWords.add(new Single(word, i, results.copyFor(filtered)));
          i++;
        }
      }
      if (!failed) {
        result.add(new SingleAnalysisSentence(sentence, singleAnalysisWords));
      }
      sentenceCount++;
      if (sentenceCount % 2000 == 0) {
        Log.info("%d sentences %d tokens analyzed. %d found", sentenceCount, tokenCount,
            result.size());
      }
    }
    return result;
  }

  class SingleAnalysisSentence {

    String sentence;
    List<Single> tokens;

    SingleAnalysisSentence(String sentence,
        List<Single> tokens) {
      this.sentence = sentence;
      this.tokens = tokens;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SingleAnalysisSentence sentence1 = (SingleAnalysisSentence) o;
      return Objects.equals(sentence, sentence1.sentence);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sentence);
    }
  }

  class Single {

    String input;
    int index;
    WordAnalysis res;

    public Single(String input, int index, WordAnalysis res) {
      this.input = input;
      this.index = index;
      this.res = res;
    }
  }

  private List<String> getSentences(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8).stream()
        .filter(s -> !s.startsWith("<")).collect(Collectors.toList());
    return TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);
  }

}
