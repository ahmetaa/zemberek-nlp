package zemberek.normalization.deasciifier.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler.InputGoldPair;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler.MixMode;
import zemberek.normalization.deasciifier.benchmark.CorpusMixSampler.StratifiedCorpus;
import zemberek.normalization.deasciifier.latticedeasciifier.DeasciifierModel;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifierTrainer;
import zemberek.normalization.deasciifier.latticedeasciifier.MorphologicalCandidateGenerator;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Production model trainer and 14-suite benchmark runner comparing Zemberek's legacy 2006 GPA
 * {@link Deasciifier} against {@link LatticeDeasciifier}.
 */
public final class LatticeDeasciifierBenchmark {

  private interface DeasciifyFn {
    String name();

    String deasciify(String input);
  }

  private record Metrics(
      String systemName,
      int sentences,
      int exactNoCirc,
      long totalWords,
      long correctNoCirc,
      long diacriticWords,
      long correctDiacriticWords,
      long strippedPinnedChars,
      double elapsedMs) {

    double sentAccNoCirc() {
      return 100.0 * exactNoCirc / Math.max(1, sentences);
    }

    double wordAccNoCirc() {
      return 100.0 * correctNoCirc / Math.max(1L, totalWords);
    }

    double diacWordAcc() {
      return 100.0 * correctDiacriticWords / Math.max(1L, diacriticWords);
    }

    double tokensPerSec() {
      return totalWords / Math.max(1e-6, elapsedMs / 1000.0);
    }
  }

  private LatticeDeasciifierBenchmark() {}

  public static void main(String[] args) throws Exception {
    int perceptronCount = args.length > 0 ? Integer.parseInt(args[0]) : 800_000;
    int lexiconMineCount = args.length > 1 ? Integer.parseInt(args[1]) : Math.max(perceptronCount, 2_000_000);
    Path modelPath =
        args.length > 2
            ? Paths.get(args[2])
            : Paths.get("normalization/src/main/resources/deasciifier/lattice_deasciifier.bin");
    String modeArg = args.length > 3 ? args[3] : "train";
    boolean reuseExisting = "reuse".equalsIgnoreCase(modeArg);

    Path dataRoot = Paths.get("/home/dndara/data/turkish");
    Path refinedTestDir = dataRoot.resolve("refined_test");
    Path gemini2500Path =
        Paths.get("/home/dndara/work/zemberek-ambiguity-data-backup/gemini_turkish_sentences_2500.txt");
    Path curatedDir = Paths.get("experiment/src/test/resources/deasciifier-benchmarks");

    MorphologicalCandidateGenerator sharedMorphGen = new MorphologicalCandidateGenerator();

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
            dataRoot, 2_500, baseForbidden, sharedMorphGen, 999_999L);

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

    DeasciifierModel model;
    if (reuseExisting && Files.exists(modelPath)) {
      model = DeasciifierModel.load(modelPath);
      System.out.printf(
          Locale.ROOT,
          "%n=== 2. Loaded Existing Model: %s (%.2f MB) ===%n",
          modelPath,
          Files.size(modelPath) / (1024.0 * 1024.0));
    } else {
      model =
          trainAndSaveProductionModel(
              dataRoot,
              lexiconMineCount,
              perceptronCount,
              allForbidden,
              sharedMorphGen,
              modelPath);
    }

    printModelMemorySummary(model, modelPath);

    LatticeDeasciifier latticeDeasciifier = new LatticeDeasciifier(model);
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
                return "LatticeDeasciifier";
              }

              @Override
              public String deasciify(String input) {
                return latticeDeasciifier.deasciify(input);
              }
            });

    System.out.println("\n=== 3. 14-Suite Evaluation ===");
    runSuite("1. Gemini-2500 (Gold Clean Diverse)", systems, gemini2500, false);
    runSuite(
        "2. Refined-Test Verified-Clean (" + refinedClean.size() + ")",
        systems,
        refinedClean,
        false);
    runSuite(
        "3. Refined-Test Raw-All (" + refinedAll.size() + ")",
        systems,
        refinedAll,
        false);

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

    System.out.println("\n=== 4. Throughput & Analyzer-Injection Modes on Gemini-2500 (31,382 words) ===");
    benchmarkThroughputAndInjectionModes(model, gemini2500);
  }

  private static DeasciifierModel trainAndSaveProductionModel(
      Path dataRoot,
      int lexiconMineCount,
      int perceptronCount,
      Set<Long> forbidden,
      MorphologicalCandidateGenerator morphGen,
      Path modelPath)
      throws Exception {
    long tLoad0 = System.currentTimeMillis();
    StratifiedCorpus fullCorpus =
        CorpusMixSampler.sampleTrainingCorpus(
            dataRoot, lexiconMineCount, MixMode.BALANCED_FIVE_PILLAR, forbidden, 20261112L);
    StratifiedCorpus trainSub = fullCorpus.subCorpus(perceptronCount);
    System.out.printf(
        Locale.ROOT,
        "%n=== 2. Training Production LatticeDeasciifier: %,d lexicon sentences / %,d Perceptron sentences (sampled in %.2fs) ===%n",
        fullCorpus.sentences().size(),
        trainSub.sentences().size(),
        (System.currentTimeMillis() - tLoad0) / 1000.0);

    long tTrain0 = System.currentTimeMillis();
    LatticeDeasciifierTrainer.Config cfg = new LatticeDeasciifierTrainer.Config();
    DeasciifierModel trained =
        LatticeDeasciifierTrainer.train(
            fullCorpus.sentences(),
            fullCorpus.circumflexIntact(),
            trainSub.sentences(),
            trainSub.circumflexIntact(),
            morphGen,
            cfg);

    trained.save(modelPath);
    DeasciifierModel reloaded = DeasciifierModel.load(modelPath);
    System.out.printf(
        Locale.ROOT,
        "Trained & saved in %.2fs -> %s (%.2f MB)%n",
        (System.currentTimeMillis() - tTrain0) / 1000.0,
        modelPath,
        Files.size(modelPath) / (1024.0 * 1024.0));
    return reloaded;
  }

  private static void printModelMemorySummary(DeasciifierModel model, Path modelPath)
      throws Exception {
    long modelBytes = Files.exists(modelPath) ? Files.size(modelPath) : -1L;
    System.out.printf(
        Locale.ROOT,
        "Model Stats: commonConfusions=%,d | properConfusions=%,d | lemmaPriors=%,d | charFeats=%,d | sentFeats=%,d (sentTableRAM=%.2f MB) | disk=%.2f MB%n",
        model.lexiconPriors().commonConfusionSize(),
        model.lexiconPriors().properConfusionSize(),
        model.lexiconPriors().lemmaPriorSize(),
        model.characterModel().featureCount(),
        model.sentenceWeights().size(),
        model.sentenceWeights().ramBytesUsed() / (1024.0 * 1024.0),
        modelBytes > 0 ? modelBytes / (1024.0 * 1024.0) : 0.0);
  }

  private static void benchmarkThroughputAndInjectionModes(
      DeasciifierModel model, List<String> gemini2500) {
    List<String> geminiAscii = new ArrayList<>(gemini2500.size());
    for (String g : gemini2500) {
      geminiAscii.add(TurkishTextCodec.asciify(g));
    }
    long wordCount = 31_382L;

    LatticeDeasciifier selfContained = new LatticeDeasciifier(model);
    long tCold0 = System.nanoTime();
    for (String in : geminiAscii) {
      selfContained.deasciify(in);
    }
    double coldMs = (System.nanoTime() - tCold0) / 1_000_000.0;

    long tWarm0 = System.nanoTime();
    for (String in : geminiAscii) {
      selfContained.deasciify(in);
    }
    double warmMs = (System.nanoTime() - tWarm0) / 1_000_000.0;

    TurkishMorphology singleHostMorph = TurkishMorphology.createWithDefaults();
    LatticeDeasciifier singleInjected = new LatticeDeasciifier(model, singleHostMorph);
    for (String in : geminiAscii) {
      singleInjected.deasciify(in);
    }
    long tSingle0 = System.nanoTime();
    for (String in : geminiAscii) {
      singleInjected.deasciify(in);
    }
    double singleWarmMs = (System.nanoTime() - tSingle0) / 1_000_000.0;

    TurkishMorphology dualInformalMorph =
        TurkishMorphology.builder()
            .setLexicon(RootLexicon.getDefault())
            .ignoreDiacriticsInAnalysis()
            .useInformalAnalysis()
            .build();
    LatticeDeasciifier dualInjected =
        new LatticeDeasciifier(model, singleHostMorph, dualInformalMorph);
    for (String in : geminiAscii) {
      dualInjected.deasciify(in);
    }
    long tDual0 = System.nanoTime();
    for (String in : geminiAscii) {
      dualInjected.deasciify(in);
    }
    double dualWarmMs = (System.nanoTime() - tDual0) / 1_000_000.0;

    System.out.printf(
        Locale.ROOT,
        "Self-Contained          : Cold=%.1f ms (%,.0f tok/s) | Warm=%.1f ms (%,.0f tok/s) | cachedTokens=%,d%n",
        coldMs,
        wordCount / (coldMs / 1000.0),
        warmMs,
        wordCount / (warmMs / 1000.0),
        selfContained.cachedTokenCount());
    System.out.printf(
        Locale.ROOT,
        "Single-Injected Morph   : Warm=%.1f ms (%,.0f tok/s) | cachedTokens=%,d%n",
        singleWarmMs,
        wordCount / (singleWarmMs / 1000.0),
        singleInjected.cachedTokenCount());
    System.out.printf(
        Locale.ROOT,
        "Dual-Injected (Normaliz): Warm=%.1f ms (%,.0f tok/s) | cachedTokens=%,d%n",
        dualWarmMs,
        wordCount / (dualWarmMs / 1000.0),
        dualInjected.cachedTokenCount());
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
        "\n------------------------------------------------------------------------------------");
    System.out.println("SUITE: " + title + " (" + goldSentences.size() + " sentences)");
    System.out.println(
        "------------------------------------------------------------------------------------");
    System.out.printf(
        Locale.ROOT,
        "%-24s | %9s | %9s | %9s | %8s | %9s%n",
        "System",
        "Sent(No^)",
        "Word(No^)",
        "DiacWord",
        "Stripped",
        "Tok/sec");
    System.out.println(
        "------------------------------------------------------------------------------------");

    for (DeasciifyFn sys : systems) {
      Metrics m = evaluate(sys, inputs, goldSentences);
      System.out.printf(
          Locale.ROOT,
          "%-24s | %8.2f%% | %8.2f%% | %8.2f%% | %8d | %9.0f%n",
          m.systemName,
          m.sentAccNoCirc(),
          m.wordAccNoCirc(),
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
    long totalWords = 0;
    long corrNoCirc = 0;
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

      int minLen = Math.min(in.length(), p.length());
      for (int c = 0; c < minLen; c++) {
        char inCh = in.charAt(c);
        // Do not count corrupted dotless-'ı' -> 'i' recovery as "stripping a valid Turkish diacritic":
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
        boolean noCircMatch =
            TurkishTextCodec.stripCircumflex(predSlice)
                .equals(TurkishTextCodec.stripCircumflex(gt.original()));
        if (noCircMatch) {
          corrNoCirc++;
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
        totalWords,
        corrNoCirc,
        diacWords,
        corrDiac,
        stripped,
        elapsedMs);
  }
}
