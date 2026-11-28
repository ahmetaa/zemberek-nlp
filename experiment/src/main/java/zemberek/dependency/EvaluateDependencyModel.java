package zemberek.dependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import zemberek.dependency.DependencyTree.EvaluationMetrics;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.format.ConlluSentence;

/**
 * Evaluates and benchmarks an exported {@code ZDEP} dependency model in pure Java.
 *
 * <p>Supports evaluating on an aligned CoNLL-U test set (measuring exact Java UAS/LAS/Root/Exact
 * Match and warm parsing throughput in tokens/sec), verifying PyTorch-vs-Java prediction parity,
 * and running live sample sentences end-to-end through {@link TurkishDependencyParser}.
 */
public final class EvaluateDependencyModel {

  public static void main(String[] args) throws IOException {
    Path modelPath = null;
    Path testConllu = null;
    Path expectedConllu = null;
    Path outputConllu = null;
    String sampleSentence = null;
    int warmupPasses = 1;
    int benchPasses = 3;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--model", "-m" -> modelPath = Paths.get(args[++i]);
        case "--test", "-t" -> testConllu = Paths.get(args[++i]);
        case "--expected" -> expectedConllu = Paths.get(args[++i]);
        case "--output", "-o" -> outputConllu = Paths.get(args[++i]);
        case "--sentence", "-s" -> sampleSentence = args[++i];
        case "--warmup" -> warmupPasses = Integer.parseInt(args[++i]);
        case "--passes" -> benchPasses = Integer.parseInt(args[++i]);
        case "--help", "-h" -> {
          printUsage();
          return;
        }
        default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
      }
    }

    if (modelPath == null) {
      printUsage();
      System.exit(1);
    }

    long loadStart = System.nanoTime();
    BiaffineDependencyModel model = BiaffineDependencyModel.load(modelPath);
    double loadMs = (System.nanoTime() - loadStart) / 1e6;
    long fileBytes = Files.size(modelPath);
    System.out.printf(Locale.ROOT,
        "Loaded ZDEP model: %s (%.2f MB) in %.1f ms%n",
        modelPath.getFileName(), fileBytes / (1024.0 * 1024.0), loadMs);

    if (sampleSentence != null) {
      TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
      TurkishDependencyParser parser =
          new TurkishDependencyParser(morphology, model, zemberek.morphology.analysis.format.ConlluFormatter.bounDefault());
      DependencyTree tree = parser.parse(sampleSentence);
      System.out.println("\n=== End-to-End Sample Parse ===");
      System.out.print(tree.toConllu());
    }

    if (testConllu != null) {
      List<ConlluSentence> gold = ConlluSentence.parseFile(testConllu);

      // Warmup
      for (int w = 0; w < warmupPasses; w++) {
        for (ConlluSentence s : gold) {
          model.parse(s);
        }
      }

      List<DependencyTree> preds = null;
      long bestNs = Long.MAX_VALUE;
      int totalTokens = gold.stream().mapToInt(ConlluSentence::size).sum();

      for (int p = 0; p < Math.max(1, benchPasses); p++) {
        List<DependencyTree> runPreds = new ArrayList<>(gold.size());
        long t0 = System.nanoTime();
        for (ConlluSentence s : gold) {
          runPreds.add(model.parse(s));
        }
        long elapsed = System.nanoTime() - t0;
        if (elapsed < bestNs) {
          bestNs = elapsed;
        }
        preds = runPreds;
      }

      EvaluationMetrics metrics = DependencyTree.evaluate(gold, preds);
      double sec = bestNs / 1e9;
      System.out.println("\n=== Pure-Java Evaluation (" + testConllu.getFileName() + ") ===");
      System.out.println(metrics);
      System.out.printf(Locale.ROOT,
          "Speed (best of %d): %.0f tok/s | %.0f sent/s (%.2f ms/sent)%n",
          Math.max(1, benchPasses),
          totalTokens / sec,
          gold.size() / sec,
          (sec * 1000.0) / Math.max(1, gold.size()));

      if (expectedConllu != null) {
        List<ConlluSentence> expected = ConlluSentence.parseFile(expectedConllu);
        EvaluationMetrics parity = DependencyTree.evaluate(expected, preds);
        System.out.println("\n=== PyTorch <-> Java Parity Check ===");
        System.out.println(parity);
      }

      if (outputConllu != null) {
        List<ConlluSentence> outSents = new ArrayList<>(preds.size());
        for (DependencyTree t : preds) {
          outSents.add(t.getConlluSentence());
        }
        if (outputConllu.getParent() != null) {
          Files.createDirectories(outputConllu.getParent());
        }
        Files.writeString(outputConllu, ConlluSentence.formatAll(outSents), StandardCharsets.UTF_8);
      }
    }
  }

  private static void printUsage() {
    System.out.println("""
        Usage:
          EvaluateDependencyModel --model <model.bin.gz> [--test <aligned_test.conllu>]
                                  [--expected <pytorch_preds.conllu>] [--output <java_preds.conllu>]
                                  [--sentence "Prof. Dr. Fethi Bey ..."] [--warmup 1] [--passes 3]
        """);
  }
}
