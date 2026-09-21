package zemberek.morphology.ambiguity.dataset;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import zemberek.apps.ConsoleApp;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.SentenceRecord;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * High-precision performance and throughput benchmarking tool for Zemberek Morphological
 * Analysis and Perceptron Ambiguity Resolution.
 *
 * Measures:
 * 1. End-to-end throughput (words/sec, sentences/sec)
 * 2. Morphological analysis speed vs. Perceptron disambiguation speed
 * 3. Ambiguous token throughput (beam-search decoder efficiency)
 * 4. Latency distribution (mean, p50, p95, p99)
 * 5. Multi-threaded core scaling
 * 6. Side-by-side comparison between baseline and custom trained models
 */
public class BenchmarkAmbiguityResolver extends ConsoleApp {

  @Parameter(
      names = {"--corpus", "-c"},
      description = "Path to test corpus (.txt or .jsonl). If omitted, auto-discovers available datasets.")
  public Path corpusPath;

  @Parameter(
      names = {"--model", "-m"},
      description = "Path to custom binary model (.bin). If omitted, benchmarks default built-in resolver.")
  public Path modelPath;

  @Parameter(
      names = {"--compareBaseline", "-cb"},
      description = "If a custom model is specified, also benchmark default baseline model for side-by-side comparison.")
  public boolean compareBaseline = false;

  @Parameter(
      names = {"--warmup", "-w"},
      description = "Number of warmup iterations to allow HotSpot JIT optimization. Default is 2.")
  public int warmupIterations = 2;

  @Parameter(
      names = {"--iterations", "-it"},
      description = "Number of measured iterations. Default is 5.")
  public int iterations = 5;

  @Parameter(
      names = {"--maxSentences", "-max"},
      description = "Maximum number of sentences to benchmark. Default is -1 (all).")
  public int maxSentences = -1;

  @Parameter(
      names = {"--threads", "-t"},
      description = "Number of threads for multi-threaded throughput test. Default is 1.")
  public int threads = 1;

  @Parameter(
      names = {"--outputJson", "-o"},
      description = "Optional output path to save JSON benchmark metrics.")
  public Path outputJsonPath;

  @Parameter(
      names = {"--beamSize", "-b"},
      description = "Beam size for FastPerceptronAmbiguityResolver (default -1 for exact Viterbi).")
  public int beamSize = -1;

  @Parameter(
      names = {"--greedy", "-g"},
      description = "Enable greedy decoding mode for FastPerceptronAmbiguityResolver.")
  public boolean greedy = false;

  @Parameter(
      names = {"--benchmarkAllModes"},
      description = "Benchmark all decoding modes (Exact Viterbi, Beam-8, Greedy) for FastPerceptronAmbiguityResolver.")
  public boolean benchmarkAllModes = false;

  public static void main(String[] args) {
    new BenchmarkAmbiguityResolver().execute(args);
    System.exit(0);
  }

  @Override
  public String description() {
    return "Benchmarks performance and throughput (words/sec, sentences/sec, latency) of Zemberek Perceptron disambiguation.";
  }

  @Override
  public void run() throws Exception {
    Path effectiveCorpus = resolveCorpusPath();
    Log.info("Loading benchmark corpus from: %s", effectiveCorpus);
    List<String> sentences = loadCorpus(effectiveCorpus, maxSentences);
    if (sentences.isEmpty()) {
      Log.error("Corpus is empty. Aborting benchmark.");
      return;
    }
    Log.info("Loaded %d sentences for benchmarking.", sentences.size());

    BenchmarkSuiteReport suiteReport = new BenchmarkSuiteReport();
    suiteReport.corpusPath = effectiveCorpus.toString();
    suiteReport.sentenceCount = sentences.size();

    // 1. Benchmark custom model (if specified)
    if (modelPath != null) {
      Log.info("Initializing custom model: %s", modelPath);
      AmbiguityResolver customResolver = FastPerceptronAmbiguityResolver.fromModelFile(modelPath);

      TurkishMorphology customMorphology = TurkishMorphology.builder()
          .setLexicon(RootLexicon.getDefault())
          .setAmbiguityResolver(customResolver)
          .build();

      String modelLabel = modelPath.getFileName().toString();
      if (customResolver instanceof FastPerceptronAmbiguityResolver && benchmarkAllModes) {
        FastPerceptronAmbiguityResolver fastResolver = (FastPerceptronAmbiguityResolver) customResolver;

        // Mode 1: Exact Viterbi
        fastResolver.setDecodeMode(FastPerceptronAmbiguityResolver.DecodeMode.VITERBI);
        BenchmarkResult rExact = runBenchmark(
            modelLabel + " [Exact Viterbi]",
            customMorphology,
            sentences,
            warmupIterations,
            iterations,
            threads);
        suiteReport.results.add(rExact);
        printResult(rExact);

        // Mode 2: Beam Search (beam = 8)
        fastResolver.setDecodeMode(FastPerceptronAmbiguityResolver.DecodeMode.BEAM);
        fastResolver.setBeamSize(8);
        BenchmarkResult rBeam = runBenchmark(
            modelLabel + " [Beam-8]",
            customMorphology,
            sentences,
            warmupIterations,
            iterations,
            threads);
        suiteReport.results.add(rBeam);
        printResult(rBeam);

        // Mode 3: Greedy
        fastResolver.setDecodeMode(FastPerceptronAmbiguityResolver.DecodeMode.GREEDY);
        BenchmarkResult rGreedy = runBenchmark(
            modelLabel + " [Greedy]",
            customMorphology,
            sentences,
            warmupIterations,
            iterations,
            threads);
        suiteReport.results.add(rGreedy);
        printResult(rGreedy);
      } else {
        if (customResolver instanceof FastPerceptronAmbiguityResolver) {
          FastPerceptronAmbiguityResolver fastResolver = (FastPerceptronAmbiguityResolver) customResolver;
          fastResolver.setBeamSize(beamSize);
          fastResolver.setGreedy(greedy);
          if (greedy) {
            modelLabel += " [Greedy]";
          } else if (beamSize > 0) {
            modelLabel += " [Beam-" + beamSize + "]";
          } else {
            modelLabel += " [Exact Viterbi]";
          }
        }
        BenchmarkResult customResult = runBenchmark(
            modelLabel,
            customMorphology,
            sentences,
            warmupIterations,
            iterations,
            threads);
        suiteReport.results.add(customResult);
        printResult(customResult);
      }
    }

    // 2. Benchmark baseline (if requested or if no custom model provided)
    BenchmarkResult baselineResult = null;
    if (compareBaseline || modelPath == null) {
      Log.info("Initializing default baseline Zemberek morphology...");
      TurkishMorphology baselineMorphology = TurkishMorphology.createWithDefaults();

      baselineResult = runBenchmark(
          "Baseline (Built-in Zemberek)",
          baselineMorphology,
          sentences,
          warmupIterations,
          iterations,
          threads);
      suiteReport.results.add(baselineResult);
      printResult(baselineResult);
    }

    // Print comparison if baseline exists
    if (baselineResult != null) {
      for (BenchmarkResult res : suiteReport.results) {
        if (res != baselineResult) {
          printComparison(baselineResult, res);
        }
      }
    }

    // Export JSON if requested
    if (outputJsonPath != null) {
      if (outputJsonPath.getParent() != null) {
        Files.createDirectories(outputJsonPath.getParent());
      }
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      try (BufferedWriter writer = Files.newBufferedWriter(outputJsonPath, StandardCharsets.UTF_8)) {
        gson.toJson(suiteReport, writer);
      }
      Log.info("Saved benchmark report to: %s", outputJsonPath);
    }
  }

  private Path resolveCorpusPath() {
    if (corpusPath != null && Files.exists(corpusPath)) {
      return corpusPath;
    }
    // Candidate default paths
    List<Path> candidates = Arrays.asList(
        Paths.get("data/ambiguity/gemini_turkish_sentences_2500.txt"),
        Paths.get("data/ambiguity/contrastive_sentences.txt"),
        Paths.get("morphology/src/test/resources/corpora/cnn-turk-10k"),
        Paths.get("data/ambiguity/gemini_turkish_sentences_500.txt"),
        Paths.get("data/ambiguity/contrastive_annotated.jsonl")
    );
    for (Path c : candidates) {
      if (Files.exists(c)) {
        return c;
      }
    }
    throw new IllegalArgumentException("No corpus specified and no default datasets found.");
  }

  public static List<String> loadCorpus(Path path, int maxSentences) throws IOException {
    List<String> sentences = new ArrayList<>();
    String pathStr = path.toString();
    boolean isJsonl = pathStr.endsWith(".jsonl");
    Gson gson = new Gson();

    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        if (isJsonl) {
          try {
            SentenceRecord rec = gson.fromJson(line, SentenceRecord.class);
            if (rec != null && rec.text != null && !rec.text.trim().isEmpty()) {
              sentences.add(rec.text.trim());
            }
          } catch (Exception ignored) {
          }
        } else {
          sentences.add(line);
        }
        if (maxSentences > 0 && sentences.size() >= maxSentences) {
          break;
        }
      }
    }
    return sentences;
  }

  public static BenchmarkResult runBenchmark(
      String modelName,
      TurkishMorphology morphology,
      List<String> sentences,
      int warmupRuns,
      int measuredRuns,
      int threadCount) throws Exception {

    Log.info("[%s] Running %d warmup passes...", modelName, warmupRuns);
    for (int w = 0; w < warmupRuns; w++) {
      for (String s : sentences) {
        morphology.analyzeAndDisambiguate(s);
      }
    }
    System.gc();
    Thread.sleep(100);

    Log.info("[%s] Profiling corpus statistics & Stage 1 (Analysis alone)...", modelName);
    int totalTokens = 0;
    int lexicalWords = 0;
    int ambiguousTokens = 0;

    List<List<WordAnalysis>> preAnalyzed = new ArrayList<>(sentences.size());
    long analysisStart = System.nanoTime();
    for (String s : sentences) {
      List<WordAnalysis> waList = morphology.analyzeSentence(s);
      preAnalyzed.add(waList);
      for (WordAnalysis wa : waList) {
        totalTokens++;
        if (wa.analysisCount() > 1) {
          ambiguousTokens++;
        }
        SingleAnalysis first = wa.getAnalysisResults().isEmpty() ? null : wa.getAnalysisResults().get(0);
        if (first != null && first.getDictionaryItem().primaryPos != null) {
          if (!first.getDictionaryItem().primaryPos.name().equalsIgnoreCase("Punc")) {
            lexicalWords++;
          }
        }
      }
    }
    long analysisElapsedNs = System.nanoTime() - analysisStart;
    double analysisSec = analysisElapsedNs / 1_000_000_000.0;
    double analysisWordsPerSec = totalTokens / analysisSec;
    double analysisSentencesPerSec = sentences.size() / analysisSec;

    Log.info("[%s] Profiling Stage 2 (Disambiguation alone)...", modelName);
    long disambigStart = System.nanoTime();
    for (int it = 0; it < measuredRuns; it++) {
      for (int i = 0; i < sentences.size(); i++) {
        morphology.disambiguate(sentences.get(i), preAnalyzed.get(i));
      }
    }
    long disambigElapsedNs = System.nanoTime() - disambigStart;
    double disambigSec = (disambigElapsedNs / (double) measuredRuns) / 1_000_000_000.0;
    double disambigWordsPerSec = totalTokens / disambigSec;
    double disambigAmbiguousWordsPerSec = ambiguousTokens / disambigSec;
    double disambigSentencesPerSec = sentences.size() / disambigSec;

    Log.info("[%s] Profiling End-to-End Pipeline (analyze + disambiguate across %d iterations)...",
        modelName, measuredRuns);

    List<Double> sentenceLatenciesMs = new ArrayList<>(sentences.size() * measuredRuns);
    long e2eTotalStart = System.nanoTime();

    for (int it = 0; it < measuredRuns; it++) {
      for (String s : sentences) {
        long sStart = System.nanoTime();
        SentenceAnalysis sa = morphology.analyzeAndDisambiguate(s);
        long sElapsed = System.nanoTime() - sStart;
        sentenceLatenciesMs.add(sElapsed / 1_000_000.0);
      }
    }
    long e2eTotalElapsedNs = System.nanoTime() - e2eTotalStart;
    double e2eSec = (e2eTotalElapsedNs / (double) measuredRuns) / 1_000_000_000.0;
    double e2eWordsPerSec = totalTokens / e2eSec;
    double e2eLexicalWordsPerSec = lexicalWords / e2eSec;
    double e2eSentencesPerSec = sentences.size() / e2eSec;

    // Latency percentiles
    Collections.sort(sentenceLatenciesMs);
    double latMean = sentenceLatenciesMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double latP50 = sentenceLatenciesMs.get((int) (sentenceLatenciesMs.size() * 0.50));
    double latP95 = sentenceLatenciesMs.get((int) (sentenceLatenciesMs.size() * 0.95));
    double latP99 = sentenceLatenciesMs.get((int) (sentenceLatenciesMs.size() * 0.99));
    double latMin = sentenceLatenciesMs.get(0);
    double latMax = sentenceLatenciesMs.get(sentenceLatenciesMs.size() - 1);

    // Multi-threaded benchmark
    Double multiThreadWordsPerSec = null;
    Double multiThreadSpeedup = null;
    if (threadCount > 1) {
      Log.info("[%s] Running multi-threaded benchmark with %d threads...", modelName, threadCount);
      ExecutorService pool = Executors.newFixedThreadPool(threadCount);
      int chunkSize = (sentences.size() + threadCount - 1) / threadCount;
      List<List<String>> chunks = new ArrayList<>();
      for (int i = 0; i < sentences.size(); i += chunkSize) {
        chunks.add(sentences.subList(i, Math.min(i + chunkSize, sentences.size())));
      }

      long mtStart = System.nanoTime();
      CountDownLatch latch = new CountDownLatch(chunks.size());
      for (List<String> chunk : chunks) {
        pool.submit(() -> {
          try {
            for (String s : chunk) {
              morphology.analyzeAndDisambiguate(s);
            }
          } finally {
            latch.countDown();
          }
        });
      }
      latch.await();
      pool.shutdown();

      long mtElapsedNs = System.nanoTime() - mtStart;
      double mtSec = mtElapsedNs / 1_000_000_000.0;
      multiThreadWordsPerSec = totalTokens / mtSec;
      multiThreadSpeedup = multiThreadWordsPerSec / e2eWordsPerSec;
    }

    BenchmarkResult result = new BenchmarkResult();
    result.modelName = modelName;
    result.sentenceCount = sentences.size();
    result.totalTokens = totalTokens;
    result.lexicalWords = lexicalWords;
    result.ambiguousTokens = ambiguousTokens;
    result.ambiguityRate = totalTokens > 0 ? (ambiguousTokens * 100.0 / totalTokens) : 0.0;
    result.avgTokensPerSentence = sentences.size() > 0 ? (totalTokens / (double) sentences.size()) : 0.0;

    result.analysisWordsPerSec = analysisWordsPerSec;
    result.analysisSentencesPerSec = analysisSentencesPerSec;
    result.analysisTimeMs = analysisSec * 1000.0;

    result.disambigWordsPerSec = disambigWordsPerSec;
    result.disambigAmbiguousWordsPerSec = disambigAmbiguousWordsPerSec;
    result.disambigSentencesPerSec = disambigSentencesPerSec;
    result.disambigTimeMs = disambigSec * 1000.0;

    result.e2eWordsPerSec = e2eWordsPerSec;
    result.e2eLexicalWordsPerSec = e2eLexicalWordsPerSec;
    result.e2eSentencesPerSec = e2eSentencesPerSec;
    result.e2eTimeMs = e2eSec * 1000.0;

    result.latencyMeanMs = latMean;
    result.latencyP50Ms = latP50;
    result.latencyP95Ms = latP95;
    result.latencyP99Ms = latP99;
    result.latencyMinMs = latMin;
    result.latencyMaxMs = latMax;

    result.threads = threadCount;
    result.multiThreadWordsPerSec = multiThreadWordsPerSec;
    result.multiThreadSpeedup = multiThreadSpeedup;

    return result;
  }

  private static void printResult(BenchmarkResult r) {
    System.out.println("\n================================================================================");
    System.out.println("  PERFORMANCE BENCHMARK: " + r.modelName);
    System.out.println("================================================================================");
    System.out.printf("  Corpus Profile:        %d sentences | %d tokens (%d lexical words)\n",
        r.sentenceCount, r.totalTokens, r.lexicalWords);
    System.out.printf("  Ambiguity Density:     %d ambiguous tokens (%.2f%% density | avg %.1f tokens/sent)\n",
        r.ambiguousTokens, r.ambiguityRate, r.avgTokensPerSentence);
    System.out.println("--------------------------------------------------------------------------------");
    System.out.println("  PIPELINE THROUGHPUT (Single Core):");
    System.out.printf("    ▶ Stage 1 (Analysis):       %,10.0f words/sec  | %,8.1f sent/sec  (%6.1f ms)\n",
        r.analysisWordsPerSec, r.analysisSentencesPerSec, r.analysisTimeMs);
    System.out.printf("    ▶ Stage 2 (Disambiguation): %,10.0f words/sec  | %,8.1f sent/sec  (%6.1f ms)\n",
        r.disambigWordsPerSec, r.disambigSentencesPerSec, r.disambigTimeMs);
    System.out.printf("      └─ Ambiguous Tokens:     %,10.0f amb-words/sec (pure decoder speed)\n",
        r.disambigAmbiguousWordsPerSec);
    System.out.println("    ----------------------------------------------------------------------------");
    System.out.printf("    ★ END-TO-END PIPELINE:      %,10.0f words/sec  | %,8.1f sent/sec  (%6.1f ms)\n",
        r.e2eWordsPerSec, r.e2eSentencesPerSec, r.e2eTimeMs);
    System.out.printf("      (Lexical words only:      %,10.0f lexical-words/sec)\n",
        r.e2eLexicalWordsPerSec);
    System.out.println("--------------------------------------------------------------------------------");
    System.out.println("  LATENCY PER SENTENCE (Microseconds/Milliseconds):");
    System.out.printf("    Mean: %6.3f ms  |  p50 (Median): %6.3f ms  |  p95: %6.3f ms  |  p99: %6.3f ms\n",
        r.latencyMeanMs, r.latencyP50Ms, r.latencyP95Ms, r.latencyP99Ms);
    System.out.printf("    Min:  %6.3f ms  |  Max:          %6.3f ms\n", r.latencyMinMs, r.latencyMaxMs);
    if (r.threads > 1 && r.multiThreadWordsPerSec != null) {
      System.out.println("--------------------------------------------------------------------------------");
      System.out.printf("  MULTI-THREADED SCALING (%d threads):\n", r.threads);
      System.out.printf("    Aggregate Throughput:       %,10.0f words/sec (%.2fx speedup)\n",
          r.multiThreadWordsPerSec, r.multiThreadSpeedup);
    }
    System.out.println("================================================================================\n");
  }

  private static void printComparison(BenchmarkResult baseline, BenchmarkResult custom) {
    System.out.println("================================================================================");
    System.out.println("  SIDE-BY-SIDE PERFORMANCE COMPARISON: Baseline vs. Custom Model");
    System.out.println("================================================================================");
    System.out.printf("  Metric                         %-25s %-25s Overhead\n", "Baseline", custom.modelName);
    System.out.println("  ------------------------------------------------------------------------------");
    System.out.printf("  End-to-End Words/sec:          %,10.0f               %,10.0f               %+6.2f%%\n",
        baseline.e2eWordsPerSec, custom.e2eWordsPerSec,
        ((custom.e2eWordsPerSec - baseline.e2eWordsPerSec) / baseline.e2eWordsPerSec) * 100.0);
    System.out.printf("  Disambiguation Words/sec:      %,10.0f               %,10.0f               %+6.2f%%\n",
        baseline.disambigWordsPerSec, custom.disambigWordsPerSec,
        ((custom.disambigWordsPerSec - baseline.disambigWordsPerSec) / baseline.disambigWordsPerSec) * 100.0);
    System.out.printf("  Ambiguous Words/sec:           %,10.0f               %,10.0f               %+6.2f%%\n",
        baseline.disambigAmbiguousWordsPerSec, custom.disambigAmbiguousWordsPerSec,
        ((custom.disambigAmbiguousWordsPerSec - baseline.disambigAmbiguousWordsPerSec) / baseline.disambigAmbiguousWordsPerSec) * 100.0);
    System.out.printf("  Mean Sentence Latency:         %8.3f ms              %8.3f ms              %+6.2f%%\n",
        baseline.latencyMeanMs, custom.latencyMeanMs,
        ((custom.latencyMeanMs - baseline.latencyMeanMs) / baseline.latencyMeanMs) * 100.0);
    System.out.printf("  p95 Sentence Latency:          %8.3f ms              %8.3f ms              %+6.2f%%\n",
        baseline.latencyP95Ms, custom.latencyP95Ms,
        ((custom.latencyP95Ms - baseline.latencyP95Ms) / baseline.latencyP95Ms) * 100.0);
    System.out.println("================================================================================\n");
  }

  // --- Report Data Models ---

  public static class BenchmarkSuiteReport {
    public String corpusPath;
    public int sentenceCount;
    public List<BenchmarkResult> results = new ArrayList<>();
  }

  public static class BenchmarkResult {
    public String modelName;
    public int sentenceCount;
    public int totalTokens;
    public int lexicalWords;
    public int ambiguousTokens;
    public double ambiguityRate;
    public double avgTokensPerSentence;

    public double analysisWordsPerSec;
    public double analysisSentencesPerSec;
    public double analysisTimeMs;

    public double disambigWordsPerSec;
    public double disambigAmbiguousWordsPerSec;
    public double disambigSentencesPerSec;
    public double disambigTimeMs;

    public double e2eWordsPerSec;
    public double e2eLexicalWordsPerSec;
    public double e2eSentencesPerSec;
    public double e2eTimeMs;

    public double latencyMeanMs;
    public double latencyP50Ms;
    public double latencyP95Ms;
    public double latencyP99Ms;
    public double latencyMinMs;
    public double latencyMaxMs;

    public int threads;
    public Double multiThreadWordsPerSec;
    public Double multiThreadSpeedup;
  }
}
