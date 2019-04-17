package zemberek.morphology.ambiguity;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.AmbiguityAnalysis;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.AnalysisDecision;
import zemberek.morphology.ambiguity.RuleBasedDisambiguator.ResultSentence;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

class GenerateDataWithRules extends AmbiguityScriptsBase {


  private static RuleBasedDisambiguator ruleBasedDisambiguator;

  private GenerateDataWithRules() throws IOException {
    super();
    RuleBasedDisambiguator.Rules rules = RuleBasedDisambiguator.Rules.fromResources();
    ruleBasedDisambiguator = new RuleBasedDisambiguator(morphology, rules);
    acceptWordPredicates.add(maxAnalysisCount(10));
    acceptWordPredicates.add(hasAnalysis());
    ignoreSentencePredicates.add(contains("\""));
    ignoreSentencePredicates.add(contains("…"));
    ignoreSentencePredicates.add(tooMuchNumberAndPunctuation(5));
    ignoreSentencePredicates.add(probablyNotTurkish());
    ignoreSentencePredicates.add(p -> p.split("[ ]+").length > 25);
    ignoreSentencePredicates.add(TextUtil::containsCombiningDiacritics);
  }

  public static void main(String[] args) throws IOException {
    Path corporaRoot = Paths.get("/media/ahmetaa/depo/corpora");
/*
    List<Path> roots = Lists.newArrayList(
        corporaRoot.resolve("www.aljazeera.com.tr"),
        corporaRoot.resolve("open-subtitles-tr-2018")
        corporaRoot.resolve("wowturkey.com"),
        corporaRoot.resolve("www.cnnturk.com"),
        corporaRoot.resolve("www.haberturk.com")
        );
*/
    List<Path> roots = Collections.singletonList(
        //corporaRoot.resolve("www.aljazeera.com.tr"),
        corporaRoot.resolve("open-subtitles-tr-2018")
        //corporaRoot.resolve("wowturkey.com"),
        //corporaRoot.resolve("www.cnnturk.com"),
        //corporaRoot.resolve("www.haberturk.com")
    );
    Path outRoot = Paths.get("/media/ahmetaa/depo/ambiguity");
    Files.createDirectories(outRoot);
    GenerateDataWithRules app = new GenerateDataWithRules();

    for (Path root : roots) {
      app.extractData(root, outRoot,  45000, 0);
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
      LinkedHashSet<String> sentences = getSentences(file);
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
      int resultLimit) {

    List<List<String>> group = group(new ArrayList<>(sentences), 5000);

    for (List<String> strings : group) {

      LinkedHashSet<String> toProcess = getAccpetableSentences(strings);

      Log.info("Processing.. %d found.", batchResult.acceptedSentences.size());
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

  static class BatchResult {

    LinkedHashSet<String> ignoredSentences = new LinkedHashSet<>();
    LinkedHashSet<String> acceptedSentences = new LinkedHashSet<>();
    List<ResultSentence> results = new ArrayList<>();
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
            SentenceAnalysis sentenceAnalysis = morphology.analyzeAndDisambiguate(sentence);
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


}

