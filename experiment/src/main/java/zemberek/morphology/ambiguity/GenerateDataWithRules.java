package zemberek.morphology.ambiguity;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.langid.LanguageIdentifier;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.AmbiguityAnalysis;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.AnalysisDecision;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.ResultSentence;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;

class GenerateDataWithRules {

  private static LanguageIdentifier identifier;
  private static RuleBasedDisambiguator ruleBasedDisambiguator;
  TurkishMorphology morphology;

  private GenerateDataWithRules() throws IOException {
    identifier = LanguageIdentifier.fromInternalModelGroup("tr_group");
    this.morphology = TurkishMorphology.createWithDefaults();
    RuleBasedDisambiguator.Rules rules = RuleBasedDisambiguator.Rules.fromResources();
    ruleBasedDisambiguator = new RuleBasedDisambiguator(morphology, rules);
  }

  private static Collection<Predicate<WordAnalysis>> acceptWordPredicates = new ArrayList<>();
  private static Collection<Predicate<String>> ignoreSentencePredicates = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    //Path p = Paths.get("/media/aaa/Data/corpora/final/www.aljazeera.com.tr");
    //Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/www.aljazeera.com.tr");
    //Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/open-subtitles");
    Path p = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/wowturkey.com");
    //Path p = Paths.get("/media/aaa/Data/corpora/final/open-subtitles");
    //Path p = Paths.get("/media/aaa/Data/corpora/final/wowturkey.com");
    Path outRoot = Paths.get("data/ambiguity");
    Files.createDirectories(outRoot);

    acceptWordPredicates.add(maxAnalysisCount(10));
    acceptWordPredicates.add(hasAnalysis());
    ignoreSentencePredicates.add(contains("\""));
    ignoreSentencePredicates.add(contains("…"));
    ignoreSentencePredicates.add(probablyNotTurkish());
    ignoreSentencePredicates.add(tooLongSentence(25));

/*    new GenerateDataWithRules()
        .extractData(p, outRoot, 150000, 0);*/
    new GenerateDataWithRules()
        .extractHighlyAmbigiousWordSentences(p, outRoot, 3, 1000);

  }

  private static Predicate<WordAnalysis> hasAnalysis() {
    return WordAnalysis::isCorrect;
  }

  private static Predicate<WordAnalysis> maxAnalysisCount(int i) {
    return p -> p.analysisCount() <= i;
  }

  private static Predicate<String> contains(String s) {
    return p -> p.contains(s);
  }

  private static Predicate<String> probablyNotTurkish() {
    return p -> !identifier.identify(p).equals("tr");
  }

  private static Predicate<String> tooLongSentence(int tokenCount) {
    return p -> p.split("[ ]+").length > tokenCount;
  }

  private void extractHighlyAmbigiousWordSentences(
      Path inputRoot,
      Path outRoot,
      int minCount,
      int wordCount)
      throws IOException {
    List<Path> files = Files.walk(inputRoot, 1).filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());

    Histogram<WordAnalysis> wordAnalyses = new Histogram<>();

    for (Path file : files) {
      Log.info("Processing %s", file);
      LinkedHashSet<String> sentences = getSentences(file);

      List<List<String>> group = group(new ArrayList<>(sentences), 5000);

      for (List<String> lines : group) {
        Log.info("Collected %d words.", wordAnalyses.size());
        LinkedHashSet<String> toProcess = getAccpetableSentences(lines);
        for (String sentence : toProcess) {
          try {
            SentenceAnalysis sentenceAnalysis = morphology.analyzeAndResolveAmbiguity(sentence);
            for (SentenceWordAnalysis analysis : sentenceAnalysis) {
              HashSet<String> stems = new HashSet<>(4);
              for (SingleAnalysis s : analysis.getWordAnalysis()) {
                stems.add(s.getStem());
                if (stems.size() > minCount) {
                  wordAnalyses.add(analysis.getWordAnalysis());
                  break;
                }
              }
            }
          } catch (Exception e) {
            Log.warn("Error in sentence %s", sentence);
          }
        }
      }
      if (wordAnalyses.size() > wordCount) {
        break;
      }
    }

    String s = inputRoot.toFile().getName();
    Path amb = outRoot.resolve(s + "-amb.txt");
    try (PrintWriter pwa = new PrintWriter(amb.toFile(), "utf-8")) {
      for (WordAnalysis wa : wordAnalyses.getSortedList()) {
        pwa.println(wa.getInput());
        for (SingleAnalysis analysis : wa) {
          pwa.println(analysis.formatLong());
        }
        pwa.println();
      }
    }
  }

  private void extractData(Path p, Path outRoot, int resultLimit, int maxAmbigiousWordCount)
      throws IOException {
    List<Path> files = Files.walk(p, 1).filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());

    BatchResult result = new BatchResult();

    int i = 0;

    for (Path file : files) {
      Log.info("Processing %s", file);
      LinkedHashSet<String> sentences = getSentences(p);
      collect(result, sentences, maxAmbigiousWordCount, resultLimit);
      i++;
      Log.info("%d of %d", i, files.size());
      if (resultLimit > 0 && result.results.size() > resultLimit) {
        break;
      }
    }

    String s = p.toFile().getName();

    Log.info("Saving.");
    Path out = outRoot.resolve(s + "-rule-result.txt");
    Path amb = outRoot.resolve(s + "-rule-result-amb.txt");

    try (
        PrintWriter pwu = new PrintWriter(out.toFile(), "utf-8");
        PrintWriter pwa = new PrintWriter(amb.toFile(), "utf-8")
    ) {
      for (ResultSentence sentence : result.results) {
        pwu.println("S:" + sentence.sentence);
        pwa.println("S:" + sentence.sentence);
        for (AmbiguityAnalysis analysis : sentence.results) {

          List<String> forTrain = analysis.getForTrainingOutput();
          forTrain.forEach(pwu::println);

          pwa.println(analysis.token);
          for (AnalysisDecision r : analysis.choices) {
            pwa.println(r.analysis.formatLong());
          }
        }
        pwu.println();
        pwa.println();
      }
    }
  }

  private void collect(
      BatchResult batchResult,
      Collection<String> sentences,
      int maxAmbigiousWordCount,
      int resultLimit)
      throws IOException {

    List<List<String>> group = group(new ArrayList<>(sentences), 5000);

    for (List<String> strings : group) {

      LinkedHashSet<String> toProcess = getAccpetableSentences(strings);

      Log.info("Processing.. %d found.", batchResult.acceptedSentences.size());
      Log.info(morphology.getCache().toString());
      for (String sentence : toProcess) {

        ResultSentence r = ruleBasedDisambiguator.disambiguate(sentence);

        if (r.ambiguousWordCount() > maxAmbigiousWordCount) {
          continue;
        }

        if (r.zeroAnalysisCount() > 0) {
          continue;
        }

        if (r.allIgnoredCount() > 0) {
          Log.warn("Sentence [%s] contains word(s) that all analyses are ignored.",
              r.sentence);
          continue;
        }

        boolean sentenceOk = true;

        for (WordAnalysis an : r.sentenceAnalysis) {
          boolean ok = true;
          for (Predicate<WordAnalysis> predicate : acceptWordPredicates) {
            if (!predicate.test(an)) {
              ok = false;
              break;
            }
          }
          if (!ok) {
            batchResult.ignoredSentences.add(sentence);
            sentenceOk = false;
            break;
          }
        }

        if (sentenceOk) {
          batchResult.acceptedSentences.add(sentence);
          batchResult.results.add(r);
          if (resultLimit > 0 && batchResult.results.size() > resultLimit) {
            return;
          }
        }
      }
    }
  }

  private LinkedHashSet<String> getAccpetableSentences(List<String> strings) {
    List<String> normalized = new ArrayList<>();
    for (String sentence : strings) {
      sentence = sentence.replaceAll("\\s+|\\u00a0", " ");
      sentence = sentence.replaceAll("[\\u00ad]", "");
      sentence = sentence.replaceAll("[…]", "...");
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

  static class BatchResult {

    LinkedHashSet<String> ignoredSentences = new LinkedHashSet<>();
    LinkedHashSet<String> acceptedSentences = new LinkedHashSet<>();
    List<ResultSentence> results = new ArrayList<>();
  }

  private LinkedHashSet<String> getSentences(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8).stream()
        .filter(s -> !s.startsWith("<")).collect(Collectors.toList());
    return new LinkedHashSet<>(TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines));
  }


}

