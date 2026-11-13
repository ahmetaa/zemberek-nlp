package zemberek.normalization.deasciifier.nimblebenchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import zemberek.core.turkish.PrimaryPos;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler.InputGoldPair;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler.MixMode;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler.StratifiedCorpus;
import zemberek.normalization.deasciifier.latticedeasciifier.Candidate;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.MorphologicalCandidateGenerator;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleDeasciifier;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleDeasciifierTrainer;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleModel;

/**
 * Production model trainer and 14-suite 3-way benchmark comparing legacy 2006 GPA {@link
 * Deasciifier}, standalone morphology-free {@link NimbleDeasciifier}, and {@link
 * LatticeDeasciifier}.
 */
public final class NimbleDeasciifierBenchmark {

  private interface DeasciifyFn {
    String name();

    String deasciify(String input);
  }

  private record Metrics(
      String systemName,
      int sentences,
      int exactNoCirc,
      int exactStrict,
      long totalWords,
      long correctNoCirc,
      long correctStrict,
      long diacriticWords,
      long correctDiacriticWords,
      long strippedPinnedChars,
      double elapsedMs) {

    double sentAccNoCirc() {
      return 100.0 * exactNoCirc / Math.max(1, sentences);
    }

    double sentAccStrict() {
      return 100.0 * exactStrict / Math.max(1, sentences);
    }

    double wordAccNoCirc() {
      return 100.0 * correctNoCirc / Math.max(1L, totalWords);
    }

    double wordAccStrict() {
      return 100.0 * correctStrict / Math.max(1L, totalWords);
    }

    double diacWordAcc() {
      return 100.0 * correctDiacriticWords / Math.max(1L, diacriticWords);
    }

    double tokensPerSec() {
      return totalWords / Math.max(1e-6, elapsedMs / 1000.0);
    }
  }

  private NimbleDeasciifierBenchmark() {}

  public static void main(String[] args) throws Exception {
    int perceptronCount = args.length > 0 ? Integer.parseInt(args[0]) : 650_000;
    int lexiconMineCount =
        args.length > 1 ? Integer.parseInt(args[1]) : Math.max(perceptronCount, 2_000_000);
    Path modelPath =
        args.length > 2
            ? Paths.get(args[2])
            : Paths.get("experiment/src/main/resources/deasciifier/nimble_deasciifier.bin");
    String modeArg = args.length > 3 ? args[3] : "train";
    boolean reuseExisting = "reuse".equalsIgnoreCase(modeArg);

    Path dataRoot = Paths.get("/home/dndara/data/turkish");
    Path refinedTestDir = dataRoot.resolve("refined_test");
    Path gemini2500Path =
        Paths.get("/home/dndara/work/zemberek-ambiguity-data-backup/gemini_turkish_sentences_2500.txt");
    Path curatedDir = Paths.get("experiment/src/test/resources/deasciifier-benchmarks");

    MorphologicalCandidateGenerator offlineMorph = new MorphologicalCandidateGenerator();

    System.out.println("=== 1. Loading Curated, Gold & 5-Domain Held-Out Benchmarks ===");
    List<String> gemini2500 = CorpusMixSampler.loadLines(gemini2500Path);
    List<String> refinedAll = CorpusMixSampler.loadDirectory(refinedTestDir);
    List<String> refinedClean = new ArrayList<>(refinedAll.size());
    for (String s : refinedAll) {
      if (CorpusMixSampler.isAuthenticTurkishSentence(s)) {
        refinedClean.add(s);
      }
    }

    List<String> homograph100 =
        CorpusMixSampler.loadLines(curatedDir.resolve("curated_homographs_100.txt"));
    List<String> oovTypo40 =
        CorpusMixSampler.loadLines(curatedDir.resolve("curated_oov_typos_numerals_40.txt"));
    List<String> foreignSpans36 =
        CorpusMixSampler.loadLines(curatedDir.resolve("curated_foreign_acronyms_spans_36.txt"));
    List<String> noisySlang36 =
        CorpusMixSampler.loadLines(curatedDir.resolve("curated_noisy_slang_clitics_36.txt"));
    List<InputGoldPair> allcapsLocale30 =
        CorpusMixSampler.loadInputGoldPairs(
            curatedDir.resolve("curated_allcaps_and_turkish_locale_lower_30.txt"));
    List<String> allcapsLocaleGolds =
        allcapsLocale30.stream().map(InputGoldPair::gold).toList();

    Set<Long> baseForbidden =
        CorpusMixSampler.buildExclusionSignatures(
            List.of(
                gemini2500,
                refinedAll,
                homograph100,
                oovTypo40,
                foreignSpans36,
                noisySlang36,
                allcapsLocaleGolds));

    Map<String, List<String>> domainHeldOutSuites =
        CorpusMixSampler.sampleHeldOutFiveDomainSuites(
            dataRoot, 2_500, baseForbidden, offlineMorph, 999_999L);

    List<List<String>> allBenchmarkLists = new ArrayList<>();
    allBenchmarkLists.add(gemini2500);
    allBenchmarkLists.add(refinedAll);
    allBenchmarkLists.add(homograph100);
    allBenchmarkLists.add(oovTypo40);
    allBenchmarkLists.add(foreignSpans36);
    allBenchmarkLists.add(noisySlang36);
    allBenchmarkLists.add(allcapsLocaleGolds);
    allBenchmarkLists.addAll(domainHeldOutSuites.values());

    Set<Long> allForbidden = CorpusMixSampler.buildExclusionSignatures(allBenchmarkLists);
    int totalDomainHeldOut =
        domainHeldOutSuites.values().stream().mapToInt(List::size).sum();

    System.out.printf(
        Locale.ROOT,
        "Benchmarks ready: Gemini=%d, RefinedClean=%d, RefinedRaw=%d, 5-DomainHeldOut=%d, Curated=%d (excluded hashes=%,d)%n",
        gemini2500.size(),
        refinedClean.size(),
        refinedAll.size(),
        totalDomainHeldOut,
        homograph100.size()
            + oovTypo40.size()
            + foreignSpans36.size()
            + noisySlang36.size()
            + allcapsLocale30.size(),
        allForbidden.size());

    NimbleModel nimbleModel;
    if (reuseExisting && Files.exists(modelPath)) {
      nimbleModel = NimbleModel.load(modelPath);
      System.out.printf(
          Locale.ROOT,
          "%n=== 2. Loaded Existing NimbleModel: %s (%.1f KB) ===%n",
          modelPath,
          Files.size(modelPath) / 1024.0);
    } else {
      nimbleModel =
          trainAndSaveModel(
              dataRoot, lexiconMineCount, perceptronCount, allForbidden, offlineMorph, modelPath);
    }

    printModelSummary(nimbleModel, modelPath);

    NimbleDeasciifier nimbleDeasciifier = new NimbleDeasciifier(nimbleModel);
    LatticeDeasciifier latticeDeasciifier = LatticeDeasciifier.createWithDefaults();

    List<DeasciifyFn> systems =
        List.of(
            new DeasciifyFn() {
              @Override
              public String name() {
                return "Legacy-2006-GPA";
              }

              @Override
              public String deasciify(String input) {
                return Deasciifier.deasciify(input, 10);
              }
            },
            new DeasciifyFn() {
              @Override
              public String name() {
                return "NimbleDeasciifier";
              }

              @Override
              public String deasciify(String input) {
                return nimbleDeasciifier.deasciify(input);
              }
            },
            new DeasciifyFn() {
              @Override
              public String name() {
                return "LatticeDeasciifier";
              }

              @Override
              public String deasciify(String input) {
                return latticeDeasciifier.deasciify(input);
              }
            });

    System.out.println("\n=== 3. 14-Suite 3-Way Evaluation ===");
    runSuite("1. Gemini-2500 (Gold Clean Diverse)", systems, gemini2500, false);
    runSuite(
        "2. Refined-Test Verified-Clean (" + refinedClean.size() + ")",
        systems,
        refinedClean,
        false);
    runSuite(
        "3. Refined-Test Raw-All (" + refinedAll.size() + ")", systems, refinedAll, false);

    int suiteIdx = 4;
    for (Map.Entry<String, List<String>> e : domainHeldOutSuites.entrySet()) {
      runSuite(suiteIdx + ". " + e.getKey(), systems, e.getValue(), false);
      suiteIdx++;
    }

    runSuite("9. Curated Homograph Minimal Pairs (100)", systems, homograph100, false);
    runSuite("10. Curated OOV, Informal, Typos & Numerals (40)", systems, oovTypo40, false);
    runSuite(
        "11. Curated Partial-ASCII Non-Destructive (500)",
        systems,
        gemini2500.subList(0, 500),
        true);
    runSuite("12. Curated Foreign, Acronyms & Spans (36)", systems, foreignSpans36, false);
    runSuite("13. Curated Noisy Slang, Clitics & Suffixes (36)", systems, noisySlang36, false);
    runPairSuite("14. Curated ALL-CAPS & Turkish.LOCALE Lower (30)", systems, allcapsLocale30);

    System.out.println("\n=== 4. Cold & Warm Throughput on Gemini-2500 (31,382 words) ===");
    benchmarkThroughput(nimbleModel, gemini2500);
  }

  private static NimbleModel trainAndSaveModel(
      Path dataRoot,
      int lexiconMineCount,
      int perceptronCount,
      Set<Long> forbidden,
      MorphologicalCandidateGenerator offlineMorph,
      Path modelPath)
      throws Exception {
    long tLoad0 = System.currentTimeMillis();
    StratifiedCorpus fullCorpus =
        CorpusMixSampler.sampleTrainingCorpus(
            dataRoot, lexiconMineCount, MixMode.BALANCED_FIVE_PILLAR, forbidden, 20261112L);
    StratifiedCorpus trainSub = fullCorpus.subCorpus(perceptronCount);
    System.out.printf(
        Locale.ROOT,
        "%n=== 2. Training NimbleDeasciifier: %,d lexicon sentences / %,d Perceptron sentences (sampled in %.2fs) ===%n",
        fullCorpus.sentences().size(),
        trainSub.sentences().size(),
        (System.currentTimeMillis() - tLoad0) / 1000.0);

    Predicate<String> offlineValidator =
        surf -> {
          Candidate c = offlineMorph.analyzeStrictBest(surf);
          return c != null && c.pos != PrimaryPos.Unknown;
        };

    long tTrain0 = System.currentTimeMillis();
    NimbleDeasciifierTrainer.Config cfg = new NimbleDeasciifierTrainer.Config();
    NimbleModel trained =
        NimbleDeasciifierTrainer.train(
            fullCorpus.sentences(),
            fullCorpus.circumflexIntact(),
            trainSub.sentences(),
            trainSub.circumflexIntact(),
            offlineValidator,
            cfg);

    trained.save(modelPath);
    NimbleModel reloaded = NimbleModel.load(modelPath);
    System.out.printf(
        Locale.ROOT,
        "Trained & saved in %.2fs -> %s (%.1f KB / %d bytes)%n",
        (System.currentTimeMillis() - tTrain0) / 1000.0,
        modelPath,
        Files.size(modelPath) / 1024.0,
        Files.size(modelPath));
    return reloaded;
  }

  private static void printModelSummary(NimbleModel model, Path modelPath) throws Exception {
    long modelBytes = Files.exists(modelPath) ? Files.size(modelPath) : -1L;
    long totalRam =
        model.lexicon().ramBytesUsed()
            + model.charModel().ramBytesUsed()
            + model.sentenceWeights().ramBytesUsed();
    System.out.printf(
        Locale.ROOT,
        "NimbleModel Stats: lowerConf=%,d | residualSingles=%,d | properConf=%,d | charFeats=%,d | sentFeats=%,d | estRAM=%.2f MB | disk=%.1f KB%n",
        model.lexicon().lowerConfusionCount(),
        model.lexicon().residualSingleCount(),
        model.lexicon().properConfusionCount(),
        model.charModel().featureCount(),
        model.sentenceWeights().size(),
        totalRam / (1024.0 * 1024.0),
        modelBytes > 0 ? modelBytes / 1024.0 : 0.0);
  }

  private static void benchmarkThroughput(NimbleModel nimbleModel, List<String> gemini2500) {
    List<String> geminiAscii = new ArrayList<>(gemini2500.size());
    for (String g : gemini2500) {
      geminiAscii.add(TurkishTextCodec.asciify(g));
    }
    long wordCount = 31_382L;

    NimbleDeasciifier freshNimble = new NimbleDeasciifier(nimbleModel);
    long tCold0 = System.nanoTime();
    for (String in : geminiAscii) {
      freshNimble.deasciify(in);
    }
    double coldMs = (System.nanoTime() - tCold0) / 1_000_000.0;

    long tWarm0 = System.nanoTime();
    for (String in : geminiAscii) {
      freshNimble.deasciify(in);
    }
    double warmMs = (System.nanoTime() - tWarm0) / 1_000_000.0;

    LatticeDeasciifier freshLattice = LatticeDeasciifier.createWithDefaults();
    long lCold0 = System.nanoTime();
    for (String in : geminiAscii) {
      freshLattice.deasciify(in);
    }
    double lColdMs = (System.nanoTime() - lCold0) / 1_000_000.0;

    long lWarm0 = System.nanoTime();
    for (String in : geminiAscii) {
      freshLattice.deasciify(in);
    }
    double lWarmMs = (System.nanoTime() - lWarm0) / 1_000_000.0;

    System.out.printf(
        Locale.ROOT,
        "NimbleDeasciifier   : Cold=%.1f ms (%,.0f tok/s) | Warm=%.1f ms (%,.0f tok/s)%n",
        coldMs,
        wordCount / (coldMs / 1000.0),
        warmMs,
        wordCount / (warmMs / 1000.0));
    System.out.printf(
        Locale.ROOT,
        "LatticeDeasciifier  : Cold=%.1f ms (%,.0f tok/s) | Warm=%.1f ms (%,.0f tok/s)%n",
        lColdMs,
        wordCount / (lColdMs / 1000.0),
        lWarmMs,
        wordCount / (lWarmMs / 1000.0));
  }

  private static void runSuite(
      String title, List<DeasciifyFn> systems, List<String> goldSentences, boolean partialAscii) {
    List<String> inputs = new ArrayList<>(goldSentences.size());
    if (partialAscii) {
      Random rng = new Random(777L);
      for (String g : goldSentences) {
        char[] chars = g.toCharArray();
        for (int i = 0; i < chars.length; i++) {
          if (rng.nextBoolean()) {
            chars[i] = TurkishTextCodec.toAsciiPreserveCase(chars[i]);
          }
        }
        inputs.add(new String(chars));
      }
    } else {
      for (String g : goldSentences) {
        inputs.add(TurkishTextCodec.asciify(g));
      }
    }
    printEvaluationTable(title, systems, inputs, goldSentences);
  }

  private static void runPairSuite(
      String title, List<DeasciifyFn> systems, List<InputGoldPair> pairs) {
    List<String> inputs = pairs.stream().map(InputGoldPair::input).toList();
    List<String> golds = pairs.stream().map(InputGoldPair::gold).toList();
    printEvaluationTable(title, systems, inputs, golds);
  }

  private static void printEvaluationTable(
      String title, List<DeasciifyFn> systems, List<String> inputs, List<String> goldSentences) {
    System.out.println(
        "\n---------------------------------------------------------------------------------------------------");
    System.out.println("SUITE: " + title + " (" + goldSentences.size() + " sentences)");
    System.out.println(
        "---------------------------------------------------------------------------------------------------");
    System.out.printf(
        Locale.ROOT,
        "%-20s | %9s | %9s | %9s | %9s | %9s | %8s | %9s%n",
        "System",
        "Sent(No^)",
        "Sent(Str)",
        "Word(No^)",
        "Word(Str)",
        "DiacWord",
        "Stripped",
        "Tok/sec");
    System.out.println(
        "---------------------------------------------------------------------------------------------------");

    for (DeasciifyFn sys : systems) {
      Metrics m = evaluate(sys, inputs, goldSentences);
      System.out.printf(
          Locale.ROOT,
          "%-20s | %8.2f%% | %8.2f%% | %8.2f%% | %8.2f%% | %8.2f%% | %8d | %9.0f%n",
          m.systemName,
          m.sentAccNoCirc(),
          m.sentAccStrict(),
          m.wordAccNoCirc(),
          m.wordAccStrict(),
          m.diacWordAcc(),
          m.strippedPinnedChars,
          m.tokensPerSec());
    }
  }

  private static Metrics evaluate(DeasciifyFn sys, List<String> inputs, List<String> gold) {
    int warm = Math.min(60, inputs.size());
    for (int i = 0; i < warm; i++) {
      sys.deasciify(inputs.get(i));
    }

    int n = gold.size();
    String[] preds = new String[n];
    long t0 = System.nanoTime();
    for (int i = 0; i < n; i++) {
      preds[i] = sys.deasciify(inputs.get(i));
    }
    double elapsedMs = (System.nanoTime() - t0) / 1_000_000.0;

    int exactNoCirc = 0;
    int exactStrict = 0;
    long totalWords = 0;
    long corrNoCirc = 0;
    long corrStrict = 0;
    long diacWords = 0;
    long corrDiac = 0;
    long stripped = 0;

    for (int i = 0; i < n; i++) {
      String g = gold.get(i);
      String p = preds[i];
      String in = inputs.get(i);
      if (TurkishTextCodec.stripCircumflex(p).equals(TurkishTextCodec.stripCircumflex(g))) {
        exactNoCirc++;
      }
      if (p.equals(g)) {
        exactStrict++;
      }

      int minLen = Math.min(in.length(), p.length());
      for (int c = 0; c < minLen; c++) {
        char inCh = in.charAt(c);
        if (TurkishTextCodec.isPinnedTurkishDiacritic(inCh)
            && !(inCh == 'ı' && (p.charAt(c) == 'i' || p.charAt(c) == 'î'))
            && p.charAt(c) != inCh) {
          stripped++;
        }
      }

      List<Token> goldTokens = TurkishTextCodec.tokenize(g);
      for (Token gt : goldTokens) {
        if (gt.immutable() && Character.isDigit(gt.original().charAt(0)) && !gt.numeralApostrophe()) {
          continue;
        }
        totalWords++;
        String predSlice = (gt.end() <= p.length()) ? p.substring(gt.start(), gt.end()) : "";
        boolean strictMatch = predSlice.equals(gt.original());
        boolean noCircMatch =
            TurkishTextCodec.stripCircumflex(predSlice)
                .equals(TurkishTextCodec.stripCircumflex(gt.original()));
        if (noCircMatch) {
          corrNoCirc++;
        }
        if (strictMatch) {
          corrStrict++;
        }
        if (!gt.original().equals(TurkishTextCodec.asciify(gt.original()))) {
          diacWords++;
          if (noCircMatch) {
            corrDiac++;
          }
        }
      }
    }

    return new Metrics(
        sys.name(),
        n,
        exactNoCirc,
        exactStrict,
        totalWords,
        corrNoCirc,
        corrStrict,
        diacWords,
        corrDiac,
        stripped,
        elapsedMs);
  }
}
