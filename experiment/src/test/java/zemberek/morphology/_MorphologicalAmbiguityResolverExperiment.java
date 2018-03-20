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
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.langid.LanguageIdentifier;
import zemberek.morphology._analyzer.AnalysisResult;
import zemberek.morphology._analyzer.InterpretingAnalyzer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class _MorphologicalAmbiguityResolverExperiment {

  LanguageIdentifier identifier;
  Map<String, List<AnalysisResult>> cache = new HashMap<>();
  Histogram<String> failedWords = new Histogram<>(100000);

  public _MorphologicalAmbiguityResolverExperiment() throws IOException {
    identifier = LanguageIdentifier.fromInternalModelGroup("tr_group");
  }

  public static void main(String[] args) throws IOException {
    Path p = Paths.get("/media/aaa/Data/corpora/final/open-subtitles");
    //Path p = Paths.get("/media/aaa/Data/corpora/final/www.kizlarsoruyor.com");
    Path outRoot = Paths.get("/home/aaa/projects/zemberek-nlp/data/ambiguity");
    Files.createDirectories(outRoot);

    new _MorphologicalAmbiguityResolverExperiment().extracData(p, outRoot);
  }

  public void extracData(Path p, Path outRoot) throws IOException {
    List<Path> files = Files.walk(p, 1).filter(s -> s.toFile().isFile()
        && s.toFile().getName().endsWith(".corpus")).collect(Collectors.toList());
    LinkedHashSet<SingleAnalysisSentence> result = new LinkedHashSet<>();

    int i = 0;

    for (Path file : files) {
      List<SingleAnalysisSentence> collect = collect(file);
      result.addAll(collect);
      i++;
      Log.info("%d of %d", i, files.size());
    }

    String s = p.toFile().getName();

    Path out = outRoot.resolve(s + "-unambigious.txt");

    try (PrintWriter pw = new PrintWriter(out.toFile(), "utf-8")) {
      for (SingleAnalysisSentence sentence : result) {
        pw.println(sentence.sentence);
        for (Single single : sentence.tokens) {
          pw.println(single.res.shortForm());
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

  private List<SingleAnalysisSentence> collect(Path p) throws IOException {
    List<String> sentences = getSentences(p);
    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    InterpretingAnalyzer analyzer = new InterpretingAnalyzer(lexicon);

    int tokenCount = 0;
    int sentenceCount = 0;

    List<SingleAnalysisSentence> result = new ArrayList<>();

    for (String sentence : sentences) {

/*
      if (!identifier.identify(sentence).equals("tr")) {
        continue;
      }
*/

      List<Single> singleAnalysisWords = new ArrayList<>();
      List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
      boolean failed = false;
      int i = 0;
      for (Token token : tokens) {
        tokenCount++;
        String rawWord = token.getText();
        String word = rawWord
            .toLowerCase(Turkish.LOCALE)
            .replaceAll("[']", "");

        List<AnalysisResult> results;
        if (cache.containsKey(word)) {
          results = cache.get(word);
        } else {
          results = analyzer.analyze(word);
          cache.put(word, results);
        }
        if (results.size() == 0) {
          failedWords.add(word);
        }
        if (results.size() != 1) {
          failed = true;
          break;
        } else {

          AnalysisResult an = results.get(0);
          if (an.dictionaryItem.secondaryPos == SecondaryPos.ProperNoun &&
              Character.isLowerCase(rawWord.charAt(0))) {
            failed = true;
            break;
          }
          singleAnalysisWords.add(new Single(word, i, an));
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
    List<Single> tokens = new ArrayList<>();

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
    AnalysisResult res;

    public Single(String input, int index, AnalysisResult res) {
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
