package zemberek.morphology.ambiguity.distillation;

import com.beust.jcommander.Parameter;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import zemberek.apps.ConsoleApp;
import zemberek.core.data.WeightLookup;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.CandidateContext;
import zemberek.morphology.ambiguity.fast.FastPerceptronAmbiguityResolver.FastDecoder;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.tokenization.TurkishSentenceExtractor;

/**
 * Multi-Threaded Active Learning Corpus Distillation Pipeline for Turkish NLP.
 *
 * Implements:
 * Stage 1: Ultra-fast surface hygiene, sentence boundary detection, word count bounds,
 *          and homonym/polysemy pivot keyword filtering.
 * Stage 2: TurkishMorphology analysis, ambiguity rate filtering (&gt;= 40%),
 *          and Perceptron Decision Margin (Δ-sampling) uncertainty scoring.
 *
 * Distills multi-gigabyte raw Turkish text across all CPU cores into a balanced, high-uncertainty
 * dataset with complete candidate parse rankings and decision margins.
 */
public class ActiveLearningDistillation extends ConsoleApp {

  @Parameter(
      names = {"--rawDir", "-i"},
      description = "Directory containing raw Turkish corpus text files (.txt)")
  public Path rawDir = Paths.get("/home/dndara/data/turkish/raw");

  @Parameter(
      names = {"--file", "-f"},
      description = "Single corpus text file to process (optional, relative to rawDir or absolute)")
  public Path singleFile = null;

  @Parameter(
      names = {"--outDir", "-o"},
      description = "Output directory for distilled sentences, JSONL candidates, and reports")
  public Path outDir = Paths.get("/home/dndara/data/turkish/distilled");

  @Parameter(
      names = {"--model", "-m"},
      description = "Path to trained binary ambiguity model (.bin)")
  public Path modelPath = Paths.get("data/ambiguity/model-disambiguation.bin");

  @Parameter(
      names = {"--ambiguousWordsFile", "-aw"},
      description = "Path to ambiguous words vocabulary list")
  public Path ambiguousWordsPath = Paths.get("data/ambiguity/zemberek-ambigious-words.txt");

  @Parameter(
      names = {"--threads", "-t"},
      description = "Number of worker threads (default: 16)")
  public int threads = 16;

  @Parameter(
      names = {"--minWords"},
      description = "Minimum word count per sentence (default: 6)")
  public int minWords = 6;

  @Parameter(
      names = {"--maxWords"},
      description = "Maximum word count per sentence (default: 35)")
  public int maxWords = 35;

  @Parameter(
      names = {"--minAmbiguityRate"},
      description = "Minimum ratio of ambiguous lexical words for Stage 2 acceptance (default: 0.40)")
  public double minAmbiguityRate = 0.40;

  @Parameter(
      names = {"--targetPerDomain"},
      description = "Fallback fixed target number of distilled hardest sentences per domain (default: 2500)")
  public int targetPerDomain = 2500;

  @Parameter(
      names = {"--proportionalTarget"},
      description = "Scale distilled sentence target proportionally to domain line count (default: true)")
  public boolean proportionalTarget = true;

  @Parameter(
      names = {"--targetFloor"},
      description = "Minimum distilled sentences floor per domain under proportional scaling (default: 2500)")
  public int targetFloor = 2500;

  @Parameter(
      names = {"--targetCeiling"},
      description = "Maximum distilled sentences ceiling per domain under proportional scaling (default: 25000)")
  public int targetCeiling = 25000;

  @Parameter(
      names = {"--targetPerMillionLines"},
      description = "Target distilled sentences per 1,000,000 lines (default: 2500)")
  public int targetPerMillionLines = 2500;

  @Parameter(
      names = {"--maxMarginThreshold"},
      description = "Maximum uncertainty decision margin Δ_min to qualify as a hard sentence (default: 3.0)")
  public double maxMarginThreshold = 3.0;

  @Parameter(
      names = {"--chunkSize"},
      description = "Number of lines per text batch chunk (default: 5000)")
  public int chunkSize = 5000;

  @Parameter(
      names = {"--help", "-h"},
      help = true,
      description = "Display usage information and exit")
  public boolean help = false;

  // High-impact Turkish homonyms and polysemous pivot stems
  private static final Set<String> CORE_HOMONYMS = Set.of(
      "koyun", "ekmek", "yüz", "yan", "at", "çay", "yaz", "kaz", "bin",
      "dolu", "kara", "açık", "geç", "düş", "dil", "sağ", "sol", "dal",
      "güç", "soluk", "o", "doğru", "yalnız", "ben", "biz", "var", "al",
      "kır", "yaş", "göz", "el", "yıl", "baş", "et", "aç", "tok", "er",
      "gül", "dik", "kat", "yat", "in", "aş", "don", "arı", "tez", "bağ",
      "ot", "diz", "yurt", "ocak", "yol", "it", "kurt", "pazartesi", "salı",
      "çarşamba", "perşembe", "cuma", "cumartesi", "pazar", "ay", "iç",
      "karşı", "kadar", "gibi", "biri", "ara", "koca", "sık", "boy",
      "çan", "düz", "ak", "kan", "can", "şan", "ayrı", "belli", "beri", "artık"
  );

  public static void main(String[] args) {
    new ActiveLearningDistillation().execute(args);
    System.exit(0);
  }

  @Override
  public String description() {
    return "Multi-threaded Stage 1 & Stage 2 active learning corpus distillation for Turkish NLP.";
  }

  @Override
  public void run() throws Exception {
    if (help) {
      System.out.println("Usage: ActiveLearningDistillation [options]");
      System.out.println("Description: " + description());
      System.out.println("\nOptions:");
      System.out.println("  --rawDir, -i             Directory containing raw Turkish corpus text files (.txt)");
      System.out.println("  --file, -f               Single corpus text file to process (optional)");
      System.out.println("  --outDir, -o             Output directory for distilled sentences, JSONL candidates, and reports");
      System.out.println("  --model, -m              Path to trained binary ambiguity model (.bin)");
      System.out.println("  --ambiguousWordsFile, -aw Path to ambiguous words vocabulary list");
      System.out.println("  --threads, -t            Number of worker threads (default: 16)");
      System.out.println("  --minWords               Minimum word count per sentence (default: 6)");
      System.out.println("  --maxWords               Maximum word count per sentence (default: 35)");
      System.out.println("  --minAmbiguityRate       Minimum ratio of ambiguous lexical words for Stage 2 (default: 0.40)");
      System.out.println("  --targetPerDomain        Fallback fixed target number of distilled hardest sentences per domain (default: 2500)");
      System.out.println("  --proportionalTarget     Scale distilled target proportionally to domain line count (default: true)");
      System.out.println("  --targetFloor            Minimum distilled sentences floor per domain under proportional scaling (default: 2500)");
      System.out.println("  --targetCeiling          Maximum distilled sentences ceiling per domain under proportional scaling (default: 25000)");
      System.out.println("  --targetPerMillionLines  Target distilled sentences per 1,000,000 lines (default: 2500)");
      System.out.println("  --maxMarginThreshold     Maximum uncertainty decision margin Δ_min (default: 3.0)");
      System.out.println("  --chunkSize              Number of lines per text batch chunk (default: 5000)");
      System.out.println("  --help, -h               Display this help message and exit");
      return;
    }

    Log.info("================================================================================");
    Log.info("Starting Multi-Threaded Active Learning Corpus Distillation");
    Log.info("  Corpora Directory:         %s", rawDir.toAbsolutePath());
    Log.info("  Output Directory:          %s", outDir.toAbsolutePath());
    Log.info("  Ambiguity Model:           %s", modelPath.toAbsolutePath());
    Log.info("  Worker Threads:            %d", threads);
    Log.info("  Sentence Length Bounds:    [%d, %d] words", minWords, maxWords);
    Log.info("  Min Ambiguity Rate:        %.1f%%", minAmbiguityRate * 100);
    if (proportionalTarget) {
      Log.info("  Target Yield Strategy:     Proportional (%,d/1M lines, floor: %,d, ceiling: %,d)",
          targetPerMillionLines, targetFloor, targetCeiling);
    } else {
      Log.info("  Target Yield Strategy:     Fixed (%,d sentences per domain)", targetPerDomain);
    }
    Log.info("  Uncertainty Margin Cutoff: Δ_min <= %.2f", maxMarginThreshold);
    Log.info("================================================================================");

    Files.createDirectories(outDir);
    Path byDomainDir = outDir.resolve("by_domain");
    Files.createDirectories(byDomainDir);

    // 1. Load Ambiguous Words Vocabulary
    Set<String> ambiguousVocab = new HashSet<>();
    if (Files.exists(ambiguousWordsPath)) {
      List<String> lines = Files.readAllLines(ambiguousWordsPath, StandardCharsets.UTF_8);
      for (String line : lines) {
        String trimmed = line.trim().toLowerCase(Turkish.LOCALE);
        if (!trimmed.isEmpty()) {
          ambiguousVocab.add(trimmed);
        }
      }
      Log.info("Loaded %d ambiguous surface words from: %s", ambiguousVocab.size(), ambiguousWordsPath);
    } else {
      Log.warn("Ambiguous words file not found at %s. Relying on core homonyms only.", ambiguousWordsPath);
    }

    // 2. Initialize Shared TurkishMorphology and FastPerceptronAmbiguityResolver
    Log.info("Initializing RootLexicon and TurkishMorphology...");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    Log.info("Loading FastPerceptronAmbiguityResolver from: %s", modelPath);
    FastPerceptronAmbiguityResolver resolver = FastPerceptronAmbiguityResolver.fromModelFile(modelPath);
    resolver.setBeamSize(8);

    // 3. Discover Raw Corpora Files
    List<Path> corpusFiles;
    if (singleFile != null) {
      Path target = singleFile;
      if (!Files.exists(target)) {
        target = rawDir.resolve(singleFile);
      }
      if (!Files.exists(target) && Files.exists(Paths.get(singleFile.toString()))) {
        target = Paths.get(singleFile.toString());
      }
      if (!Files.exists(target)) {
        Log.error("Specified single corpus file does not exist: %s", singleFile);
        return;
      }
      corpusFiles = Collections.singletonList(target);
    } else if (Files.isRegularFile(rawDir)) {
      corpusFiles = Collections.singletonList(rawDir);
    } else {
      corpusFiles = Files.list(rawDir)
          .filter(p -> p.toString().endsWith(".txt") && Files.isRegularFile(p))
          .sorted(Comparator.comparing(Path::getFileName))
          .collect(Collectors.toList());
    }

    if (corpusFiles.isEmpty()) {
      Log.error("No .txt files found in raw directory: %s", rawDir);
      return;
    }

    Log.info("Found %d corpus files for distillation across %d domains.", corpusFiles.size(), corpusFiles.size());

    DistillationRunReport globalReport = new DistillationRunReport();
    globalReport.startedAt = Instant.now().toString();
    globalReport.threads = threads;
    globalReport.minWords = minWords;
    globalReport.maxWords = maxWords;
    globalReport.minAmbiguityRate = minAmbiguityRate;
    globalReport.targetPerDomain = targetPerDomain;
    globalReport.proportionalTarget = proportionalTarget;
    globalReport.targetFloor = targetFloor;
    globalReport.targetCeiling = targetCeiling;
    globalReport.targetPerMillionLines = targetPerMillionLines;

    List<DistilledSentenceRecord> allDistilledRecords = new ArrayList<>();
    Set<String> globalSeenDistilled = ConcurrentHashMap.newKeySet();
    Stopwatch globalTimer = Stopwatch.createStarted();

    // 4. Process Each Domain File
    for (int fileIndex = 0; fileIndex < corpusFiles.size(); fileIndex++) {
      Path corpusFile = corpusFiles.get(fileIndex);
      String domainName = corpusFile.getFileName().toString().replace(".txt", "");
      long fileSizeMb = Files.size(corpusFile) / (1024 * 1024);

      Log.info("\n--------------------------------------------------------------------------------");
      Log.info("[%d/%d] Processing Domain: %s (%d MB)", fileIndex + 1, corpusFiles.size(), domainName, fileSizeMb);
      Log.info("--------------------------------------------------------------------------------");

      DomainDistillationStats domainStats = processDomain(
          corpusFile,
          domainName,
          morphology,
          resolver,
          ambiguousVocab,
          byDomainDir,
          globalSeenDistilled
      );

      globalReport.domainReports.add(domainStats);
      allDistilledRecords.addAll(domainStats.selectedSentences);

      Log.info("Domain [%s] Summary: Lines: %,d | Stg1 Valid: %,d | Stg2 Cand: %,d | Distilled Hard: %,d | Avg Δ_min: %.3f",
          domainName,
          domainStats.totalLinesScanned,
          domainStats.stage1PassCount,
          domainStats.stage2AnalyzedCount,
          domainStats.distilledCount,
          domainStats.avgMinMargin);
    }

    globalTimer.stop();
    long totalElapsedMs = globalTimer.elapsed(TimeUnit.MILLISECONDS);

    // 5. Global Ranking and Combined Deliverables Export
    Log.info("\n================================================================================");
    Log.info("Generating Combined Multi-Domain Distillation Deliverables");
    Log.info("================================================================================");

    // Sort all distilled sentences by uncertainty score ascending (hardest first)
    allDistilledRecords.sort(Comparator.comparingDouble(DistilledSentenceRecord::getUncertaintyScore));

    // Defense-in-depth deduplication
    List<DistilledSentenceRecord> uniqueDistilledRecords = new ArrayList<>(allDistilledRecords.size());
    Set<String> writtenSentences = new HashSet<>();
    for (DistilledSentenceRecord record : allDistilledRecords) {
      if (writtenSentences.add(normalizeSentence(record.text))) {
        uniqueDistilledRecords.add(record);
      }
    }

    Path combinedTxtPath = outDir.resolve("hard_uncertainty_sentences.txt");
    Path combinedJsonlPath = outDir.resolve("hard_uncertainty_candidates.jsonl");

    Gson jsonlGson = new Gson();
    try (BufferedWriter txtWriter = Files.newBufferedWriter(combinedTxtPath, StandardCharsets.UTF_8);
         BufferedWriter jsonlWriter = Files.newBufferedWriter(combinedJsonlPath, StandardCharsets.UTF_8)) {

      for (DistilledSentenceRecord record : uniqueDistilledRecords) {
        txtWriter.write(record.text);
        txtWriter.newLine();

        jsonlWriter.write(jsonlGson.toJson(record));
        jsonlWriter.newLine();
      }
    }

    Log.info("Wrote %,d combined hard sentences to: %s", uniqueDistilledRecords.size(), combinedTxtPath);
    Log.info("Wrote %,d combined candidate records to: %s", uniqueDistilledRecords.size(), combinedJsonlPath);

    // 6. Global Metrics & Report Generation
    globalReport.completedAt = Instant.now().toString();
    globalReport.totalElapsedSec = totalElapsedMs / 1000.0;
    globalReport.totalDistilledSentences = uniqueDistilledRecords.size();

    long globalLines = globalReport.domainReports.stream().mapToLong(d -> d.totalLinesScanned).sum();
    long globalWords = globalReport.domainReports.stream().mapToLong(d -> d.totalWordsScanned).sum();
    long globalStg1 = globalReport.domainReports.stream().mapToLong(d -> d.stage1PassCount).sum();
    long globalStg2 = globalReport.domainReports.stream().mapToLong(d -> d.stage2AnalyzedCount).sum();
    long globalTokens = globalReport.domainReports.stream().mapToLong(d -> d.totalTokensAnalyzed).sum();
    long globalLexTokens = globalReport.domainReports.stream().mapToLong(d -> d.totalLexicalTokensAnalyzed).sum();
    long globalUnrecTokens = globalReport.domainReports.stream().mapToLong(d -> d.totalUnrecognizedTokens).sum();
    long globalAmbTokens = uniqueDistilledRecords.stream().mapToLong(r -> r.ambiguous_tokens).sum();
    long globalDistTokens = uniqueDistilledRecords.stream().mapToLong(r -> r.total_tokens).sum();
    long globalFileMb = globalReport.domainReports.stream().mapToLong(d -> d.fileSizeMb).sum();

    globalReport.totalLinesScanned = globalLines;
    globalReport.totalWordsScanned = globalWords;
    globalReport.totalStage1Pass = globalStg1;
    globalReport.totalStage2Analyzed = globalStg2;
    globalReport.totalTokensAnalyzed = globalTokens;
    globalReport.totalUnrecognizedTokens = globalUnrecTokens;
    globalReport.overallUnrecognizedRate = globalLexTokens > 0 ? (double) globalUnrecTokens / globalLexTokens : 0.0;

    double elapsedSec = Math.max(0.001, totalElapsedMs / 1000.0);
    globalReport.globalThroughputLinesPerSec = globalLines / elapsedSec;
    globalReport.globalThroughputWordsPerSec = globalWords / elapsedSec;
    globalReport.globalThroughputTokensPerSec = globalTokens / elapsedSec;
    globalReport.globalThroughputMbPerSec = globalFileMb / elapsedSec;
    globalReport.overallAmbiguityRate = globalDistTokens > 0 ? (double) globalAmbTokens / globalDistTokens : 0.0;

    // Aggregate global unrecognized words across all domains
    Map<String, AggregatedUnrec> globalUnrecMap = new HashMap<>();
    for (DomainDistillationStats d : globalReport.domainReports) {
      if (d.unrecognizedMap != null) {
        for (UnrecognizedWordInfo u : d.unrecognizedMap.values()) {
          AggregatedUnrec agg = globalUnrecMap.computeIfAbsent(
              u.word, k -> new AggregatedUnrec(k, u.sampleSurface, u.sampleSentence, d.domain));
          agg.totalCount += u.count.sum();
        }
      }
    }
    globalReport.uniqueUnrecognizedWords = globalUnrecMap.size();

    List<AggregatedUnrec> sortedGlobalUnrec = new ArrayList<>(globalUnrecMap.values());
    sortedGlobalUnrec.sort((a, b) -> Long.compare(b.totalCount, a.totalCount));

    Path globalUnrecPath = outDir.resolve("unrecognized_words.tsv");
    try (BufferedWriter unrecWriter = Files.newBufferedWriter(globalUnrecPath, StandardCharsets.UTF_8)) {
      unrecWriter.write("Rank\tWord\tCount\tSampleSurface\tDomain\tSampleSentence\n");
      int rank = 1;
      for (AggregatedUnrec u : sortedGlobalUnrec) {
        String cleanSentence = u.sampleSentence.replace("\t", " ").replace("\n", " ").trim();
        unrecWriter.write(String.format("%d\t%s\t%d\t%s\t%s\t%s\n",
            rank, u.word, u.totalCount, u.sampleSurface, u.domain, cleanSentence));
        if (rank <= 100) {
          globalReport.topUnrecognizedWords.add(new UnrecognizedWordSummary(
              rank, u.word, u.totalCount, u.sampleSurface, u.domain, cleanSentence));
        }
        rank++;
      }
    }
    Log.info("Saved %,d global unique unrecognized words to: %s", sortedGlobalUnrec.size(), globalUnrecPath);

    // Margin percentiles
    if (!uniqueDistilledRecords.isEmpty()) {
      List<Double> sortedMargins = uniqueDistilledRecords.stream()
          .map(r -> r.min_margin)
          .sorted()
          .collect(Collectors.toList());
      globalReport.marginP05 = sortedMargins.get((int) (sortedMargins.size() * 0.05));
      globalReport.marginP10 = sortedMargins.get((int) (sortedMargins.size() * 0.10));
      globalReport.marginP25 = sortedMargins.get((int) (sortedMargins.size() * 0.25));
      globalReport.marginP50 = sortedMargins.get((int) (sortedMargins.size() * 0.50));
      globalReport.marginP75 = sortedMargins.get((int) (sortedMargins.size() * 0.75));
      globalReport.marginP90 = sortedMargins.get((int) (sortedMargins.size() * 0.90));
      globalReport.marginMin = sortedMargins.get(0);
      globalReport.marginMax = sortedMargins.get(sortedMargins.size() - 1);
    }

    // Save JSON Report
    Path reportJsonPath = outDir.resolve("distillation_report.json");
    Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    try (BufferedWriter reportWriter = Files.newBufferedWriter(reportJsonPath, StandardCharsets.UTF_8)) {
      prettyGson.toJson(globalReport, reportWriter);
    }
    Log.info("Saved detailed JSON metrics to: %s", reportJsonPath);

    // Save Markdown Report
    Path reportMdPath = outDir.resolve("distillation_report.md");
    writeMarkdownReport(globalReport, reportMdPath);
    Log.info("Saved Markdown summary report to: %s", reportMdPath);

    Log.info("================================================================================");
    Log.info("Distillation Successfully Completed in %.2f seconds!", globalReport.totalElapsedSec);
    Log.info("  Total Raw Lines Scanned:      %,d (%,.0f lines/sec)", globalLines, globalReport.globalThroughputLinesPerSec);
    Log.info("  Total Raw Words Scanned:      %,d (%,.0f words/sec)", globalWords, globalReport.globalThroughputWordsPerSec);
    Log.info("  Data Volume Processed:        %,d MB (%.2f MB/sec)", globalFileMb, globalReport.globalThroughputMbPerSec);
    Log.info("  Stage 1 Surface Candidates:   %,d (%.2f%%)", globalStg1, (globalLines > 0 ? (globalStg1 * 100.0 / globalLines) : 0));
    Log.info("  Stage 2 Analyzed Sentences:   %,d", globalStg2);
    Log.info("  Stage 2 Analyzed Tokens:      %,d (%,.0f tokens/sec)", globalTokens, globalReport.globalThroughputTokensPerSec);
    Log.info("  Distilled High-Uncertainty:   %,d sentences", uniqueDistilledRecords.size());
    Log.info("  Uncertainty Margin Med (p50): Δ_min = %.3f", globalReport.marginP50);
    Log.info("  Unrecognized Tokens (OOV):    %,d (%.2f%% of lexical) across %,d distinct words",
        globalReport.totalUnrecognizedTokens, globalReport.overallUnrecognizedRate * 100, globalReport.uniqueUnrecognizedWords);
    Log.info("================================================================================");
  }

  /**
   * Processes a single corpus domain file using multi-threaded chunk-based workers.
   */
  private DomainDistillationStats processDomain(
      Path corpusFile,
      String domainName,
      TurkishMorphology morphology,
      FastPerceptronAmbiguityResolver resolver,
      Set<String> ambiguousVocab,
      Path byDomainDir,
      Set<String> globalSeenDistilled) throws IOException, InterruptedException {

    Stopwatch domainTimer = Stopwatch.createStarted();
    DomainDistillationStats stats = new DomainDistillationStats();
    stats.domain = domainName;
    stats.fileSizeMb = Files.size(corpusFile) / (1024 * 1024);

    // Track sentences seen within this domain to prevent duplicate analysis & queue pollution
    Set<String> domainSeenSentences = ConcurrentHashMap.newKeySet();

    // Determine domain target (Option A: proportional scaling with floor and ceiling)
    int computedTarget = targetPerDomain;
    if (proportionalTarget) {
      long totalLinesEst = countLinesFast(corpusFile);
      if (totalLinesEst > 0) {
        long calculated = (totalLinesEst * targetPerMillionLines) / 1_000_000L;
        computedTarget = (int) Math.max(targetFloor, Math.min(targetCeiling, calculated));
        Log.info("Domain [%s]: %,d lines -> proportional target: %,d sentences (floor: %,d, ceiling: %,d)",
            domainName, totalLinesEst, computedTarget, targetFloor, targetCeiling);
      }
    } else {
      Log.info("Domain [%s]: fixed target: %,d sentences", domainName, computedTarget);
    }
    final int domainTarget = computedTarget;
    stats.targetCount = domainTarget;

    // Bounded max-heap by uncertainty score descending:
    // If heap size reaches domainTarget, the element with largest uncertainty score is at top and polled.
    PriorityQueue<DistilledSentenceRecord> domainQueue = new PriorityQueue<>(
        domainTarget + 1,
        (a, b) -> Double.compare(b.getUncertaintyScore(), a.getUncertaintyScore())
    );

    Object queueLock = new Object();
    AtomicLong linesScanned = new AtomicLong(0);
    AtomicLong wordsScanned = new AtomicLong(0);
    AtomicLong stage1Passes = new AtomicLong(0);
    AtomicLong stage2Analyzed = new AtomicLong(0);
    AtomicLong tokensAnalyzed = new AtomicLong(0);
    AtomicLong lexicalTokensAnalyzed = new AtomicLong(0);
    AtomicLong unrecognizedTokensCount = new AtomicLong(0);
    ConcurrentHashMap<String, UnrecognizedWordInfo> unrecognizedMap = new ConcurrentHashMap<>();
    long domainStartTime = System.currentTimeMillis();

    ExecutorService threadPool = Executors.newFixedThreadPool(threads);
    BlockingQueue<List<String>> chunkQueue = new LinkedBlockingQueue<>(threads * 3);
    CountDownLatch completionLatch = new CountDownLatch(threads);

    AtomicInteger sentenceIdSeq = new AtomicInteger(0);

    // Start Worker Threads
    for (int t = 0; t < threads; t++) {
      threadPool.submit(() -> {
        try {
          while (true) {
            List<String> chunk = chunkQueue.poll(500, TimeUnit.MILLISECONDS);
            if (chunk == null) {
              continue;
            }
            if (chunk.isEmpty()) {
              // Sentinel chunk indicating worker termination
              break;
            }

            for (String rawLine : chunk) {
              if (rawLine == null || rawLine.isEmpty() || rawLine.startsWith("<") || rawLine.length() < 20) {
                continue;
              }

              // Extract sentences using Turkish Sentence Boundary Segmenter
              List<String> sentences;
              try {
                sentences = TurkishSentenceExtractor.DEFAULT.fromParagraph(rawLine);
              } catch (Exception e) {
                continue;
              }

              for (String sentence : sentences) {
                String normalized = normalizeSentence(sentence);
                if (normalized.isEmpty() || !domainSeenSentences.add(normalized)) {
                  continue;
                }

                // Stage 1: Surface & Homonym Filter
                if (!passesStage1Filter(sentence, ambiguousVocab)) {
                  continue;
                }
                stage1Passes.incrementAndGet();

                // Stage 2: Morphological Analysis & Margin Sampling
                DistilledSentenceRecord record = evaluateStage2Uncertainty(
                    domainName,
                    sentenceIdSeq.incrementAndGet(),
                    sentence,
                    morphology,
                    resolver,
                    unrecognizedMap,
                    unrecognizedTokensCount,
                    tokensAnalyzed,
                    lexicalTokensAnalyzed
                );

                if (record == null) {
                  continue;
                }
                stage2Analyzed.incrementAndGet();

                if (record.min_margin > maxMarginThreshold) {
                  continue;
                }

                // Add to domain priority queue (thread-safe, deduplicating against globally distilled sentences)
                synchronized (queueLock) {
                  if (globalSeenDistilled.contains(normalized)) {
                    continue;
                  }
                  if (domainQueue.size() < domainTarget) {
                    domainQueue.add(record);
                  } else if (record.getUncertaintyScore() < domainQueue.peek().getUncertaintyScore()) {
                    domainQueue.poll();
                    domainQueue.add(record);
                  }
                }
              }
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (Throwable th) {
          Log.error("Error in worker thread: %s", th.getMessage(), th);
        } finally {
          completionLatch.countDown();
        }
      });
    }

    // Producer: Read Corpus Chunks from Disk
    try {
      try (BufferedReader reader = Files.newBufferedReader(corpusFile, StandardCharsets.UTF_8)) {
        List<String> currentChunk = new ArrayList<>(chunkSize);
        String line;
        long lastReportTime = System.currentTimeMillis();

        while ((line = reader.readLine()) != null) {
          currentChunk.add(line);
          long count = linesScanned.incrementAndGet();
          wordsScanned.addAndGet(countWords(line));

          if (currentChunk.size() >= chunkSize) {
            while (!chunkQueue.offer(currentChunk, 500, TimeUnit.MILLISECONDS)) {
              if (completionLatch.getCount() == 0) {
                break;
              }
            }
            currentChunk = new ArrayList<>(chunkSize);
          }

          if (System.currentTimeMillis() - lastReportTime > 5000) {
            int currentCollected;
            double currentWorstMargin = 0.0;
            synchronized (queueLock) {
              currentCollected = domainQueue.size();
              if (!domainQueue.isEmpty()) {
                currentWorstMargin = domainQueue.peek().min_margin;
              }
            }
            double curSec = (System.currentTimeMillis() - domainStartTime) / 1000.0;
            double curWordsPerSec = curSec > 0 ? (wordsScanned.get() / curSec) : 0;
            double curLinesPerSec = curSec > 0 ? (count / curSec) : 0;
            Log.info("  [%s] Lines: %,d (%,.0f l/s) | Words: %,d (%,.0f w/s) | Stg1: %,d | Stg2: %,d | OOV: %,d | In Queue: %,d (cutoff Δ: %.3f)",
                domainName, count, curLinesPerSec, wordsScanned.get(), curWordsPerSec,
                stage1Passes.get(), stage2Analyzed.get(), unrecognizedTokensCount.get(),
                currentCollected, currentWorstMargin);
            lastReportTime = System.currentTimeMillis();
          }
        }

        if (!currentChunk.isEmpty()) {
          while (!chunkQueue.offer(currentChunk, 500, TimeUnit.MILLISECONDS)) {
            if (completionLatch.getCount() == 0) {
              break;
            }
          }
        }
      }
    } finally {
      // Send poison pills to shut down workers safely
      for (int t = 0; t < threads; t++) {
        chunkQueue.offer(Collections.emptyList());
      }
      completionLatch.await(30, TimeUnit.SECONDS);
      threadPool.shutdown();
      if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
      }
    }

    domainTimer.stop();
    long elapsedMs = domainTimer.elapsed(TimeUnit.MILLISECONDS);
    double elapsedSec = Math.max(0.001, elapsedMs / 1000.0);

    stats.elapsedSec = elapsedSec;
    stats.totalLinesScanned = linesScanned.get();
    stats.totalWordsScanned = wordsScanned.get();
    stats.stage1PassCount = stage1Passes.get();
    stats.stage2AnalyzedCount = stage2Analyzed.get();
    stats.totalTokensAnalyzed = tokensAnalyzed.get();
    stats.totalLexicalTokensAnalyzed = lexicalTokensAnalyzed.get();
    stats.totalUnrecognizedTokens = unrecognizedTokensCount.get();
    stats.uniqueUnrecognizedWords = unrecognizedMap.size();
    stats.unrecognizedRate = lexicalTokensAnalyzed.get() > 0
        ? (double) unrecognizedTokensCount.get() / lexicalTokensAnalyzed.get()
        : 0.0;

    stats.linesPerSec = stats.totalLinesScanned / elapsedSec;
    stats.wordsPerSec = stats.totalWordsScanned / elapsedSec;
    stats.tokensPerSec = stats.totalTokensAnalyzed / elapsedSec;
    stats.mbPerSec = ((double) stats.fileSizeMb) / elapsedSec;

    // Extract sorted sentences from priority queue
    List<DistilledSentenceRecord> domainDistilled = new ArrayList<>(domainQueue);
    domainDistilled.sort(Comparator.comparingDouble(DistilledSentenceRecord::getUncertaintyScore));

    for (DistilledSentenceRecord r : domainDistilled) {
      globalSeenDistilled.add(normalizeSentence(r.text));
    }

    stats.selectedSentences = domainDistilled;
    stats.distilledCount = domainDistilled.size();
    stats.avgMinMargin = domainDistilled.stream().mapToDouble(r -> r.min_margin).average().orElse(0.0);
    stats.avgAmbiguityRate = domainDistilled.stream().mapToDouble(r -> r.ambiguity_rate).average().orElse(0.0);

    // Export per-domain deliverables
    Path domainTxtPath = byDomainDir.resolve(domainName + "_hard.txt");
    Path domainJsonlPath = byDomainDir.resolve(domainName + "_hard.jsonl");

    Gson gson = new Gson();
    try (BufferedWriter txtWriter = Files.newBufferedWriter(domainTxtPath, StandardCharsets.UTF_8);
         BufferedWriter jsonlWriter = Files.newBufferedWriter(domainJsonlPath, StandardCharsets.UTF_8)) {

      for (DistilledSentenceRecord r : domainDistilled) {
        txtWriter.write(r.text);
        txtWriter.newLine();

        jsonlWriter.write(gson.toJson(r));
        jsonlWriter.newLine();
      }
    }

    // Export per-domain unrecognized words TSV
    Path domainUnrecPath = byDomainDir.resolve(domainName + "_unrecognized.tsv");
    List<UnrecognizedWordInfo> sortedUnrec = new ArrayList<>(unrecognizedMap.values());
    sortedUnrec.sort((a, b) -> Long.compare(b.count.sum(), a.count.sum()));

    try (BufferedWriter unrecWriter = Files.newBufferedWriter(domainUnrecPath, StandardCharsets.UTF_8)) {
      unrecWriter.write("Rank\tWord\tCount\tSampleSurface\tSampleSentence\n");
      int rank = 1;
      for (UnrecognizedWordInfo u : sortedUnrec) {
        String cleanSentence = u.sampleSentence.replace("\t", " ").replace("\n", " ").trim();
        unrecWriter.write(String.format("%d\t%s\t%d\t%s\t%s\n",
            rank, u.word, u.count.sum(), u.sampleSurface, cleanSentence));
        if (rank <= 50) {
          stats.topUnrecognizedWords.add(new UnrecognizedWordSummary(
              rank, u.word, u.count.sum(), u.sampleSurface, domainName, cleanSentence));
        }
        rank++;
      }
    }
    stats.unrecognizedMap = unrecognizedMap;

    Log.info("Domain [%s] Benchmark Summary:", domainName);
    Log.info("  Time: %.2f s | Lines: %,d (%,.0f l/s) | Words: %,d (%,.0f w/s) | Size: %d MB (%.1f MB/s)",
        stats.elapsedSec, stats.totalLinesScanned, stats.linesPerSec,
        stats.totalWordsScanned, stats.wordsPerSec, stats.fileSizeMb, stats.mbPerSec);
    Log.info("  Stage 1: %,d (%.1f%%) | Stage 2 Sentences: %,d | Morph Tokens: %,d (%,.0f tok/s)",
        stats.stage1PassCount, (stats.totalLinesScanned > 0 ? (stats.stage1PassCount * 100.0 / stats.totalLinesScanned) : 0),
        stats.stage2AnalyzedCount, stats.totalTokensAnalyzed, stats.tokensPerSec);
    Log.info("  Distilled Hard: %,d sentences (avg Δ_min: %.3f, ambiguity: %.1f%%)",
        stats.distilledCount, stats.avgMinMargin, stats.avgAmbiguityRate * 100.0);
    Log.info("  Unrecognized (OOV): %,d tokens (%.2f%% of lexical) across %,d unique words",
        stats.totalUnrecognizedTokens, stats.unrecognizedRate * 100.0, stats.uniqueUnrecognizedWords);

    return stats;
  }

  /**
   * Ultra-fast Stage 1 surface and keyword filter.
   */
  private boolean passesStage1Filter(String sentence, Set<String> ambiguousVocab) {
    if (sentence == null) {
      return false;
    }
    sentence = sentence.trim();
    int len = sentence.length();
    if (len < 25 || len > 350) {
      return false;
    }

    char lastChar = sentence.charAt(len - 1);
    if (lastChar == '"' || lastChar == '\'' || lastChar == '”' || lastChar == '’' || lastChar == ')' || lastChar == ']') {
      if (len >= 2) {
        lastChar = sentence.charAt(len - 2);
      }
    }
    if (lastChar != '.' && lastChar != '?' && lastChar != '!' && lastChar != '…') {
      return false;
    }

    if (sentence.contains("http://") || sentence.contains("https://") || sentence.contains("www.")) {
      return false;
    }

    // Split words by whitespace
    String[] rawWords = sentence.split("[\\s\\u00a0\\u200b]+");
    int wordCount = rawWords.length;
    if (wordCount < minWords || wordCount > maxWords) {
      return false;
    }

    // Check letter density
    int letterCount = 0;
    for (int i = 0; i < len; i++) {
      char c = sentence.charAt(i);
      if (Character.isLetter(c)) {
        letterCount++;
      }
    }
    if ((double) letterCount / len < 0.65) {
      return false;
    }

    // Fast check for homonyms or ambiguous words
    int ambiguousWordMatches = 0;
    boolean hasCoreHomonym = false;

    for (String rw : rawWords) {
      String cleanWord = cleanToken(rw);
      if (cleanWord.isEmpty()) {
        continue;
      }
      int apostropheIdx = cleanWord.indexOf('\'');
      String stemCheck = apostropheIdx > 0 ? cleanWord.substring(0, apostropheIdx) : cleanWord.replace("'", "");

      if (CORE_HOMONYMS.contains(cleanWord) || CORE_HOMONYMS.contains(stemCheck)) {
        hasCoreHomonym = true;
        ambiguousWordMatches++;
      } else if (ambiguousVocab.contains(cleanWord) || ambiguousVocab.contains(stemCheck)) {
        ambiguousWordMatches++;
      }
    }

    // Stage 1 Selection: Must have at least 1 core homonym AND (surface ambiguity ratio >= 30% OR at least 3 ambiguous words)
    double ambRatio = (double) ambiguousWordMatches / wordCount;
    return hasCoreHomonym && (ambRatio >= 0.30 || ambiguousWordMatches >= 3);
  }

  private static String cleanToken(String s) {
    String lower = s.toLowerCase(Turkish.LOCALE);
    int start = 0;
    int end = lower.length();
    while (start < end && !Character.isLetterOrDigit(lower.charAt(start))) {
      start++;
    }
    while (end > start && !Character.isLetterOrDigit(lower.charAt(end - 1))) {
      end--;
    }
    return start < end ? lower.substring(start, end) : "";
  }

  private static String normalizeSentence(String sentence) {
    if (sentence == null) {
      return "";
    }
    return sentence.strip().replaceAll("[\\s\\u00a0\\u200b]+", " ");
  }

  private static int countWords(String s) {
    if (s == null || s.isEmpty()) {
      return 0;
    }
    int words = 0;
    boolean inWord = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isWhitespace(c)) {
        if (!inWord) {
          words++;
          inWord = true;
        }
      } else {
        inWord = false;
      }
    }
    return words;
  }

  private static long countLinesFast(Path file) {
    try (InputStream is = new BufferedInputStream(Files.newInputStream(file), 65536)) {
      byte[] c = new byte[65536];
      long count = 0;
      int readChars;
      boolean empty = true;
      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n') {
            ++count;
          }
        }
      }
      return (count == 0 && !empty) ? 1 : count;
    } catch (IOException e) {
      return 0;
    }
  }

  private static void collectUnrecognizedToken(
      String surface,
      String sentence,
      String domain,
      ConcurrentHashMap<String, UnrecognizedWordInfo> unrecognizedMap,
      AtomicLong unrecognizedTokensCounter) {
    if (surface == null || surface.isEmpty()) {
      return;
    }
    String clean = cleanToken(surface);
    if (clean.length() < 2) {
      return;
    }
    boolean hasLetter = false;
    for (int i = 0; i < clean.length(); i++) {
      if (Character.isLetter(clean.charAt(i))) {
        hasLetter = true;
        break;
      }
    }
    if (!hasLetter) {
      return;
    }

    unrecognizedTokensCounter.incrementAndGet();
    UnrecognizedWordInfo info = unrecognizedMap.get(clean);
    if (info != null) {
      info.count.increment();
    } else {
      UnrecognizedWordInfo newInfo = new UnrecognizedWordInfo(clean, surface, sentence, domain);
      UnrecognizedWordInfo prev = unrecognizedMap.putIfAbsent(clean, newInfo);
      if (prev != null) {
        prev.count.increment();
      }
    }
  }

  /**
   * Evaluates morphological ambiguity and computes Perceptron decision margins (Δ-sampling).
   */
  private DistilledSentenceRecord evaluateStage2Uncertainty(
      String domain,
      int seqId,
      String sentence,
      TurkishMorphology morphology,
      FastPerceptronAmbiguityResolver resolver,
      ConcurrentHashMap<String, UnrecognizedWordInfo> unrecognizedMap,
      AtomicLong unrecognizedTokensCounter,
      AtomicLong tokensAnalyzedCounter,
      AtomicLong lexicalTokensCounter) {

    String normalized = TextUtil.normalizeQuotesHyphens(sentence);
    List<WordAnalysis> analyses;
    try {
      analyses = morphology.analyzeSentence(normalized);
    } catch (Exception e) {
      return null;
    }

    if (analyses.isEmpty()) {
      return null;
    }

    int totalTokens = analyses.size();
    int lexicalTokens = 0;
    int ambiguousTokens = 0;

    for (WordAnalysis wa : analyses) {
      if (wa.analysisCount() > 0) {
        SingleAnalysis first = wa.getAnalysisResults().get(0);
        if (first.getDictionaryItem() != null && first.getDictionaryItem().primaryPos != PrimaryPos.Punctuation) {
          lexicalTokens++;
        }
      } else if (!wa.getInput().trim().isEmpty() && Character.isLetterOrDigit(wa.getInput().charAt(0))) {
        lexicalTokens++;
      }
      if (wa.getAnalysisResults().size() > 1) {
        ambiguousTokens++;
      }
      if (!wa.isCorrect()) {
        collectUnrecognizedToken(wa.getInput(), sentence, domain, unrecognizedMap, unrecognizedTokensCounter);
      }
    }

    tokensAnalyzedCounter.addAndGet(totalTokens);
    lexicalTokensCounter.addAndGet(lexicalTokens);

    if (lexicalTokens == 0) {
      return null;
    }

    double lexicalAmbiguityRate = (double) ambiguousTokens / lexicalTokens;
    if (lexicalAmbiguityRate < minAmbiguityRate) {
      return null;
    }

    // Decode global Viterbi best parse
    SentenceAnalysis sentenceAnalysis = resolver.disambiguate(normalized, analyses);
    List<SingleAnalysis> bestParse = sentenceAnalysis.bestAnalysis();

    FastDecoder decoder = resolver.getDecoder();
    WeightLookup model = resolver.getModel();

    CandidateContext cSentenceBegin = new CandidateContext(
        FastPerceptronAmbiguityResolver.sentenceBegin, model, false, 0);

    // Precompute candidate contexts for best parse sequence to avoid repeated object creation
    CandidateContext[] bestParseContexts = new CandidateContext[totalTokens];
    for (int i = 0; i < totalTokens; i++) {
      bestParseContexts[i] = new CandidateContext(bestParse.get(i), model, i == 0, 0);
    }

    List<TokenDistillationRecord> tokenRecords = new ArrayList<>(totalTokens);
    double minMargin = Double.MAX_VALUE;
    double sumMargin = 0.0;
    int scoredAmbiguousCount = 0;
    int charOffset = 0;

    for (int t = 0; t < totalTokens; t++) {
      WordAnalysis wa = analyses.get(t);
      SingleAnalysis chosen = bestParse.get(t);
      List<SingleAnalysis> candidates = wa.getAnalysisResults();

      TokenDistillationRecord tr = new TokenDistillationRecord();
      tr.index = t;
      tr.surface = wa.getInput();
      tr.is_ambiguous = candidates.size() > 1;

      // Calculate token character offsets in sentence
      int spanStart = sentence.indexOf(tr.surface, charOffset);
      if (spanStart >= 0) {
        tr.char_start = spanStart;
        tr.char_end = spanStart + tr.surface.length();
        charOffset = tr.char_end;
      } else {
        tr.char_start = charOffset;
        tr.char_end = charOffset + tr.surface.length();
      }

      if (!tr.is_ambiguous) {
        CandidateDistillationRecord cr = new CandidateDistillationRecord();
        cr.id = 0;
        cr.zemberek_key = chosen.formatLong();
        DictionaryItem item = chosen.getDictionaryItem();
        cr.lemma = item != null ? item.lemma : chosen.getStem();
        cr.pos = (item != null && item.primaryPos != null) ? item.primaryPos.shortForm : "Unk";
        cr.secondary_pos = (item != null && item.secondaryPos != null && item.secondaryPos != SecondaryPos.None)
            ? item.secondaryPos.shortForm : "None";
        cr.oflazer_style = AnalysisFormatters.OFLAZER_STYLE.format(chosen);
        cr.score = 0.0f;
        cr.rank = 1;
        cr.is_informal = chosen.containsInformalMorpheme();
        tr.candidates.add(cr);
        tr.selected_candidate_id = 0;
        tokenRecords.add(tr);
        continue;
      }

      // Candidate Context at t
      CandidateContext prevPrevCtx = (t >= 2) ? bestParseContexts[t - 2] : cSentenceBegin;
      CandidateContext prevCtx = (t >= 1) ? bestParseContexts[t - 1] : cSentenceBegin;

      boolean isInitial = (t == 0);

      // Score all competing candidates in context
      List<ScoredCandidate> scoredList = new ArrayList<>(candidates.size());
      for (int i = 0; i < candidates.size(); i++) {
        SingleAnalysis sa = candidates.get(i);
        CandidateContext cCurr = new CandidateContext(sa, model, isInitial, i);
        float stepScore = cCurr.uniScore
            + decoder.computeBigramScore(prevCtx, cCurr)
            + decoder.computeTrigramScore(prevPrevCtx, prevCtx, cCurr);
        scoredList.add(new ScoredCandidate(i, sa, stepScore));
      }

      scoredList.sort((a, b) -> Float.compare(b.score, a.score));

      float topScore = scoredList.get(0).score;
      float secondScore = scoredList.size() > 1 ? scoredList.get(1).score : topScore;
      float deltaMargin = Math.max(0.0f, topScore - secondScore);

      tr.margin = (double) Math.round(deltaMargin * 1000.0) / 1000.0;
      minMargin = Math.min(minMargin, tr.margin);
      sumMargin += tr.margin;
      scoredAmbiguousCount++;

      int rank = 1;
      for (ScoredCandidate sc : scoredList) {
        CandidateDistillationRecord cr = new CandidateDistillationRecord();
        cr.id = sc.id;
        cr.zemberek_key = sc.sa.formatLong();
        DictionaryItem item = sc.sa.getDictionaryItem();
        cr.lemma = item != null ? item.lemma : sc.sa.getStem();
        cr.pos = (item != null && item.primaryPos != null) ? item.primaryPos.shortForm : "Unk";
        cr.secondary_pos = (item != null && item.secondaryPos != null && item.secondaryPos != SecondaryPos.None)
            ? item.secondaryPos.shortForm : "None";
        cr.oflazer_style = AnalysisFormatters.OFLAZER_STYLE.format(sc.sa);
        cr.score = (float) (Math.round(sc.score * 1000.0) / 1000.0);
        cr.rank = rank++;
        cr.is_informal = sc.sa.containsInformalMorpheme();
        tr.candidates.add(cr);

        if (sc.sa.equals(chosen)) {
          tr.selected_candidate_id = sc.id;
        }
      }

      if (tr.selected_candidate_id == null && !tr.candidates.isEmpty()) {
        tr.selected_candidate_id = tr.candidates.get(0).id;
      }

      tokenRecords.add(tr);
    }

    if (scoredAmbiguousCount == 0 || minMargin == Double.MAX_VALUE) {
      return null;
    }

    DistilledSentenceRecord record = new DistilledSentenceRecord();
    record.sentence_id = String.format("distill_%s_%06d", domain.replace("-", "_"), seqId);
    record.domain = domain;
    record.text = normalizeSentence(sentence);
    record.total_tokens = totalTokens;
    record.lexical_tokens = lexicalTokens;
    record.ambiguous_tokens = ambiguousTokens;
    record.ambiguity_rate = (double) Math.round(lexicalAmbiguityRate * 10000.0) / 10000.0;
    record.min_margin = (double) Math.round(minMargin * 1000.0) / 1000.0;
    record.mean_margin = (double) Math.round((sumMargin / scoredAmbiguousCount) * 1000.0) / 1000.0;
    record.tokens = tokenRecords;

    return record;
  }

  private static class ScoredCandidate {
    final int id;
    final SingleAnalysis sa;
    final float score;

    ScoredCandidate(int id, SingleAnalysis sa, float score) {
      this.id = id;
      this.sa = sa;
      this.score = score;
    }
  }

  private void writeMarkdownReport(DistillationRunReport rep, Path mdPath) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("# Active Learning Corpus Distillation Report\n\n");
    sb.append("**Timestamp**: ").append(rep.completedAt).append("\n");
    sb.append("**Execution Time**: ").append(String.format("%.2f s", rep.totalElapsedSec))
        .append(" (").append(String.format("%.0f lines/sec", rep.globalThroughputLinesPerSec))
        .append(", ").append(String.format("%.0f words/sec", rep.globalThroughputWordsPerSec)).append(")\n");
    sb.append("**Worker Threads**: ").append(rep.threads).append("\n");
    sb.append("**Sentence Length Bounds**: [").append(rep.minWords).append(", ").append(rep.maxWords).append("] words\n");
    sb.append("**Min Lexical Ambiguity Rate**: ").append(String.format("%.1f%%", rep.minAmbiguityRate * 100)).append("\n\n");

    sb.append("## Global Yield & Benchmark Summary\n\n");
    sb.append("| Metric | Value |\n");
    sb.append("| :--- | :--- |\n");
    sb.append("| Total Raw Lines Scanned | ").append(String.format("%,d", rep.totalLinesScanned)).append(" |\n");
    sb.append("| Total Raw Words Scanned | ").append(String.format("%,d", rep.totalWordsScanned)).append(" |\n");
    sb.append("| Scanning Throughput | ").append(String.format("%,.0f lines/sec (%,.0f words/sec)", rep.globalThroughputLinesPerSec, rep.globalThroughputWordsPerSec)).append(" |\n");
    sb.append("| Stage 1 Surface Filter Pass | ").append(String.format("%,d (%.2f%%)", rep.totalStage1Pass, (rep.totalLinesScanned > 0 ? rep.totalStage1Pass * 100.0 / rep.totalLinesScanned : 0))).append(" |\n");
    sb.append("| Stage 2 Analyzed Sentences | ").append(String.format("%,d", rep.totalStage2Analyzed)).append(" |\n");
    sb.append("| Stage 2 Analyzed Tokens | ").append(String.format("%,d (%,.0f tokens/sec)", rep.totalTokensAnalyzed, rep.globalThroughputTokensPerSec)).append(" |\n");
    sb.append("| **Distilled High-Uncertainty Sentences** | **").append(String.format("%,d", rep.totalDistilledSentences)).append("** |\n");
    sb.append("| Overall Lexical Ambiguity Density | ").append(String.format("%.2f%%", rep.overallAmbiguityRate * 100)).append(" |\n");
    sb.append("| **Unrecognized Tokens (OOV)** | **").append(String.format("%,d (%.2f%% of lexical)", rep.totalUnrecognizedTokens, rep.overallUnrecognizedRate * 100)).append("** |\n");
    sb.append("| **Unique Unrecognized Words** | **").append(String.format("%,d distinct words", rep.uniqueUnrecognizedWords)).append("** |\n\n");

    sb.append("## Uncertainty Margin Distribution (Δ_min)\n\n");
    sb.append("Lower margin indicates greater model hesitation between top-1 and runner-up candidate parses.\n\n");
    sb.append("| Min | p05 | p10 | p25 | p50 (Median) | p75 | p90 | Max |\n");
    sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");
    sb.append(String.format("| %.3f | %.3f | %.3f | %.3f | **%.3f** | %.3f | %.3f | %.3f |\n\n",
        rep.marginMin, rep.marginP05, rep.marginP10, rep.marginP25, rep.marginP50, rep.marginP75, rep.marginP90, rep.marginMax));

    sb.append("## Per-Domain Yield & Benchmark Performance\n\n");
    sb.append("| Domain | File Size | Lines Scanned | Words Scanned | Words/s | Stg1 Valid | Stg2 Cand | Distilled | Avg Δ_min | Ambiguity % | OOV Tokens (Rate) | Elapsed |\n");
    sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");

    for (DomainDistillationStats d : rep.domainReports) {
      sb.append(String.format("| `%s` | %d MB | %,d | %,d | %,.0f | %,d | %,d | **%,d** | %.3f | %.1f%% | %,d (%.2f%%) | %.1f s |\n",
          d.domain, d.fileSizeMb, d.totalLinesScanned, d.totalWordsScanned, d.wordsPerSec,
          d.stage1PassCount, d.stage2AnalyzedCount, d.distilledCount, d.avgMinMargin,
          d.avgAmbiguityRate * 100, d.totalUnrecognizedTokens, d.unrecognizedRate * 100, d.elapsedSec));
    }
    sb.append("\n");

    if (!rep.topUnrecognizedWords.isEmpty()) {
      sb.append("## Unrecognized Words (Out-of-Vocabulary Analysis)\n\n");
      sb.append("Lexical surface words not recognized by Zemberek's morphological dictionary or morphotactics rules.\n\n");
      sb.append("| Rank | Word | Occurrences | Sample Surface | Domain | Context Sentence |\n");
      sb.append("| :---: | :--- | :---: | :--- | :--- | :--- |\n");
      int maxShown = Math.min(rep.topUnrecognizedWords.size(), 30);
      for (int i = 0; i < maxShown; i++) {
        UnrecognizedWordSummary u = rep.topUnrecognizedWords.get(i);
        sb.append(String.format("| %d | `%s` | %,d | `%s` | `%s` | *\"%s\"* |\n",
            u.rank, u.word, u.count, u.sampleSurface, u.domain, u.sampleSentence));
      }
      sb.append("\n> Full dictionary of unrecognized words exported to: `unrecognized_words.tsv`\n\n");
    }

    sb.append("## Output Deliverables\n\n");
    sb.append("- **Combined Distilled Sentences**: `hard_uncertainty_sentences.txt`\n");
    sb.append("- **Full Candidate Annotations (JSONL)**: `hard_uncertainty_candidates.jsonl`\n");
    sb.append("- **Unrecognized Words TSV**: `unrecognized_words.tsv`\n");
    sb.append("- **Per-Domain Deliverables**: Directory `by_domain/` with separate `.txt`, `.jsonl`, and `_unrecognized.tsv` for each domain.\n");

    Files.writeString(mdPath, sb.toString(), StandardCharsets.UTF_8);
  }

  // --- Output Data Structures ---

  public static class DistillationRunReport {
    public String startedAt;
    public String completedAt;
    public double totalElapsedSec;
    public int threads;
    public int minWords;
    public int maxWords;
    public double minAmbiguityRate;
    public int targetPerDomain;
    public boolean proportionalTarget;
    public int targetFloor;
    public int targetCeiling;
    public int targetPerMillionLines;
    public long totalLinesScanned;
    public long totalWordsScanned;
    public long totalStage1Pass;
    public long totalStage2Analyzed;
    public long totalTokensAnalyzed;
    public long totalUnrecognizedTokens;
    public int uniqueUnrecognizedWords;
    public double overallUnrecognizedRate;
    public int totalDistilledSentences;
    public double globalThroughputLinesPerSec;
    public double globalThroughputWordsPerSec;
    public double globalThroughputTokensPerSec;
    public double globalThroughputMbPerSec;
    public double overallAmbiguityRate;
    public double marginMin;
    public double marginP05;
    public double marginP10;
    public double marginP25;
    public double marginP50;
    public double marginP75;
    public double marginP90;
    public double marginMax;
    public List<DomainDistillationStats> domainReports = new ArrayList<>();
    public List<UnrecognizedWordSummary> topUnrecognizedWords = new ArrayList<>();
  }

  public static class DomainDistillationStats {
    public String domain;
    public long fileSizeMb;
    public int targetCount;
    public long totalLinesScanned;
    public long totalWordsScanned;
    public long stage1PassCount;
    public long stage2AnalyzedCount;
    public long totalTokensAnalyzed;
    public long totalLexicalTokensAnalyzed;
    public long totalUnrecognizedTokens;
    public int uniqueUnrecognizedWords;
    public double unrecognizedRate;
    public int distilledCount;
    public double avgMinMargin;
    public double avgAmbiguityRate;
    public double elapsedSec;
    public double linesPerSec;
    public double wordsPerSec;
    public double tokensPerSec;
    public double mbPerSec;
    public transient List<DistilledSentenceRecord> selectedSentences = new ArrayList<>();
    public transient Map<String, UnrecognizedWordInfo> unrecognizedMap = new HashMap<>();
    public List<UnrecognizedWordSummary> topUnrecognizedWords = new ArrayList<>();
  }

  public static class UnrecognizedWordSummary {
    public int rank;
    public String word;
    public long count;
    public String sampleSurface;
    public String domain;
    public String sampleSentence;

    public UnrecognizedWordSummary(int rank, String word, long count, String sampleSurface, String domain, String sampleSentence) {
      this.rank = rank;
      this.word = word;
      this.count = count;
      this.sampleSurface = sampleSurface;
      this.domain = domain;
      this.sampleSentence = sampleSentence;
    }
  }

  public static class UnrecognizedWordInfo {
    public final String word;
    public final LongAdder count = new LongAdder();
    public final String sampleSurface;
    public final String sampleSentence;
    public final String domain;

    public UnrecognizedWordInfo(String word, String sampleSurface, String sampleSentence, String domain) {
      this.word = word;
      this.count.increment();
      this.sampleSurface = sampleSurface;
      this.sampleSentence = sampleSentence;
      this.domain = domain;
    }
  }

  private static class AggregatedUnrec {
    final String word;
    final String sampleSurface;
    final String sampleSentence;
    final String domain;
    long totalCount = 0;

    AggregatedUnrec(String word, String sampleSurface, String sampleSentence, String domain) {
      this.word = word;
      this.sampleSurface = sampleSurface;
      this.sampleSentence = sampleSentence;
      this.domain = domain;
    }
  }

  public static class DistilledSentenceRecord {
    public String sentence_id;
    public String domain;
    public String text;
    public int total_tokens;
    public int lexical_tokens;
    public int ambiguous_tokens;
    public double ambiguity_rate;
    public double min_margin;
    public double mean_margin;
    public List<TokenDistillationRecord> tokens = new ArrayList<>();

    public double getUncertaintyScore() {
      return min_margin - (0.1 * ambiguity_rate) + (0.01 * mean_margin);
    }
  }

  public static class TokenDistillationRecord {
    public int index;
    public String surface;
    public int char_start;
    public int char_end;
    public boolean is_ambiguous;
    public Double margin;
    public Integer selected_candidate_id;
    public List<CandidateDistillationRecord> candidates = new ArrayList<>();
  }

  public static class CandidateDistillationRecord {
    public int id;
    public String zemberek_key;
    public String lemma;
    public String pos;
    public String secondary_pos;
    public String oflazer_style;
    public float score;
    public int rank;
    public boolean is_informal;
  }
}
