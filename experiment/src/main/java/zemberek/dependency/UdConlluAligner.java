package zemberek.dependency;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.format.ConlluFormatter;
import zemberek.morphology.analysis.format.ConlluFormatter.Dialect;
import zemberek.morphology.analysis.format.ConlluFormatter.MwtMode;
import zemberek.morphology.analysis.format.ConlluSentence;
import zemberek.morphology.analysis.format.ConlluToken;
import zemberek.tokenization.TurkishSentenceExtractor;

/**
 * Projects Universal Dependencies v2 CoNLL-U treebanks (e.g. {@code UD_Turkish-BOUN}) and
 * unlabeled Turkish text corpora through Zemberek's morphological analyzer, disambiguator, and
 * {@link ConlluFormatter}.
 *
 * <p>Two modes are supported:
 * <ul>
 *   <li><b>Treebank alignment &amp; audit ({@code --input <gold.conllu>})</b>: Runs Zemberek over
 *       the gold surface tokens, measures MWT/UPOS/lemma/FEATS agreement against the gold
 *       treebank, and optionally writes an aligned CoNLL-U file pairing Zemberek-predicted
 *       morphology with gold syntactic {@code HEAD} and {@code DEPREL} columns (eliminating
 *       train/inference feature mismatch for dependency parser training).</li>
 *   <li><b>Raw text projection ({@code --project-text <sentences.txt>})</b>: Projects unlabeled
 *       Turkish sentences into CoNLL-U format for teacher–student distillation.</li>
 * </ul>
 */
public final class UdConlluAligner {

  public record AlignmentStats(
      int totalSentences,
      int exactMwtSentences,
      int exportedSentences,
      int totalSurfaceWords,
      int matchedSurfaceWords,
      int totalTokens,
      int uposMatches,
      int lemmaMatches,
      int exactFeatsMatches,
      int goldFeatPairs,
      int predFeatPairs,
      int matchedFeatPairs,
      Map<String, Integer> uposConfusions,
      Map<String, Integer> mwtMismatchExamples,
      Map<String, Integer> featMismatchCounts) {

    public double exactMwtSentenceRate() {
      return totalSentences == 0 ? 0.0 : (100.0 * exactMwtSentences) / totalSentences;
    }

    public double surfaceWordMwtRate() {
      return totalSurfaceWords == 0 ? 0.0 : (100.0 * matchedSurfaceWords) / totalSurfaceWords;
    }

    public double uposAccuracy() {
      return totalTokens == 0 ? 0.0 : (100.0 * uposMatches) / totalTokens;
    }

    public double lemmaAccuracy() {
      return totalTokens == 0 ? 0.0 : (100.0 * lemmaMatches) / totalTokens;
    }

    public double exactFeatsAccuracy() {
      return totalTokens == 0 ? 0.0 : (100.0 * exactFeatsMatches) / totalTokens;
    }

    public double featsMicroF1() {
      int denom = goldFeatPairs + predFeatPairs;
      return denom == 0 ? 100.0 : (200.0 * matchedFeatPairs) / denom;
    }

    public String formatReport(int topN) {
      StringBuilder sb = new StringBuilder(1024);
      sb.append("=== Zemberek <-> UD CoNLL-U Alignment Audit ===\n");
      sb.append(String.format(Locale.ROOT,
          "Sentences:           %d (exact MWT match: %d / %.2f%%, exported: %d)\n",
          totalSentences, exactMwtSentences, exactMwtSentenceRate(), exportedSentences));
      sb.append(String.format(Locale.ROOT,
          "Surface words:       %d (MWT-span match: %d / %.2f%%)\n",
          totalSurfaceWords, matchedSurfaceWords, surfaceWordMwtRate()));
      sb.append(String.format(Locale.ROOT,
          "Syntactic tokens:    %d\n", totalTokens));
      sb.append(String.format(Locale.ROOT,
          "UPOS accuracy:       %.2f%% (%d / %d)\n", uposAccuracy(), uposMatches, totalTokens));
      sb.append(String.format(Locale.ROOT,
          "Lemma accuracy:      %.2f%% (%d / %d)\n", lemmaAccuracy(), lemmaMatches, totalTokens));
      sb.append(String.format(Locale.ROOT,
          "FEATS exact match:   %.2f%% (%d / %d)\n",
          exactFeatsAccuracy(), exactFeatsMatches, totalTokens));
      sb.append(String.format(Locale.ROOT,
          "FEATS micro-F1:      %.2f%% (matched=%d, gold=%d, pred=%d)\n",
          featsMicroF1(), matchedFeatPairs, goldFeatPairs, predFeatPairs));

      appendTopEntries(sb, "\nTop UPOS confusions (gold -> pred):", uposConfusions, topN);
      appendTopEntries(sb, "\nTop MWT split mismatches (gold_count -> pred_count : form):",
          mwtMismatchExamples, topN);
      appendTopEntries(sb, "\nTop FEATS pair mismatches (+/-Key=Val):", featMismatchCounts, topN);
      return sb.toString();
    }

    private static void appendTopEntries(
        StringBuilder sb, String header, Map<String, Integer> counts, int topN) {
      if (counts.isEmpty()) {
        return;
      }
      sb.append(header).append('\n');
      counts.entrySet().stream()
          .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
              .thenComparing(Map.Entry.comparingByKey()))
          .limit(topN)
          .forEach(e -> sb.append(String.format(Locale.ROOT, "  %6d  %s\n", e.getValue(), e.getKey())));
    }
  }

  public record AlignmentResult(List<ConlluSentence> alignedSentences, AlignmentStats stats) {}

  private final TurkishMorphology morphology;
  private final ConlluFormatter formatter;

  public UdConlluAligner(TurkishMorphology morphology, ConlluFormatter formatter) {
    this.morphology = morphology;
    this.formatter = formatter;
  }

  /**
   * Aligns gold UD CoNLL-U sentences with Zemberek's morphological projection.
   *
   * <p>When a surface word's MWT split matches between gold and Zemberek, Zemberek's disambiguated
   * sentence-context projection is used directly. When an individual surface word splits
   * differently, each gold syntactic sub-token is projected individually via Zemberek unless
   * {@code strictOnly} is {@code true} (in which case mismatched sentences are omitted from
   * {@link AlignmentResult#alignedSentences()}).
   */
  public AlignmentResult alignTreebank(List<ConlluSentence> goldSentences, boolean strictOnly) {
    int totalSentences = 0;
    int exactMwtSentences = 0;
    int totalSurfaceWords = 0;
    int matchedSurfaceWords = 0;
    int totalTokens = 0;
    int uposMatches = 0;
    int lemmaMatches = 0;
    int exactFeatsMatches = 0;
    int goldFeatPairs = 0;
    int predFeatPairs = 0;
    int matchedFeatPairs = 0;

    Map<String, Integer> uposConfusions = new HashMap<>();
    Map<String, Integer> mwtMismatchExamples = new HashMap<>();
    Map<String, Integer> featMismatchCounts = new HashMap<>();
    List<ConlluSentence> aligned = new ArrayList<>(goldSentences.size());

    for (ConlluSentence gold : goldSentences) {
      totalSentences++;
      List<String> surfaceWords = gold.surfaceForms();
      String sentenceText = (gold.text() != null && !gold.text().isEmpty())
          ? gold.text()
          : String.join(" ", surfaceWords);

      List<WordAnalysis> analyzedWords = new ArrayList<>(surfaceWords.size());
      for (String w : surfaceWords) {
        analyzedWords.add(morphology.analyze(w));
      }
      SentenceAnalysis disambiguated = morphology.disambiguate(sentenceText, analyzedWords);
      ConlluSentence pred = formatter.projectSentence(gold.sentId(), disambiguated);

      Map<Integer, List<ConlluToken>> goldByWord = groupBySourceWord(gold.tokens());
      Map<Integer, List<ConlluToken>> predByWord = groupBySourceWord(pred.tokens());

      boolean sentenceMwtExact = true;
      List<ConlluToken> mergedTokens = new ArrayList<>(gold.size());

      for (int wi = 0; wi < surfaceWords.size(); wi++) {
        totalSurfaceWords++;
        List<ConlluToken> gGroup = goldByWord.getOrDefault(wi, Collections.emptyList());
        List<ConlluToken> pGroup = predByWord.getOrDefault(wi, Collections.emptyList());

        List<ConlluToken> chosenPred;
        if (gGroup.size() == pGroup.size()) {
          matchedSurfaceWords++;
          chosenPred = pGroup;
        } else {
          sentenceMwtExact = false;
          String mismatchKey = gGroup.size() + "->" + pGroup.size() + " : " + surfaceWords.get(wi);
          mwtMismatchExamples.merge(mismatchKey, 1, Integer::sum);
          chosenPred = projectGoldSubtokensFallback(gGroup);
        }

        for (int k = 0; k < gGroup.size(); k++) {
          ConlluToken gTok = gGroup.get(k);
          ConlluToken pTok = chosenPred.get(k);
          totalTokens++;

          if (gTok.upos().equals(pTok.upos())) {
            uposMatches++;
          } else {
            uposConfusions.merge(gTok.upos() + " -> " + pTok.upos(), 1, Integer::sum);
          }

          if (gTok.lemma().equalsIgnoreCase(pTok.lemma())) {
            lemmaMatches++;
          }

          if (gTok.feats().equals(pTok.feats())) {
            exactFeatsMatches++;
          }

          Set<String> gFeats = new HashSet<>(gTok.featPairs());
          Set<String> pFeats = new HashSet<>(pTok.featPairs());
          goldFeatPairs += gFeats.size();
          predFeatPairs += pFeats.size();
          for (String gf : gFeats) {
            if (pFeats.contains(gf)) {
              matchedFeatPairs++;
            } else {
              featMismatchCounts.merge("-" + gf, 1, Integer::sum);
            }
          }
          for (String pf : pFeats) {
            if (!gFeats.contains(pf)) {
              featMismatchCounts.merge("+" + pf, 1, Integer::sum);
            }
          }

          mergedTokens.add(new ConlluToken(
              gTok.id(),
              gTok.form(),
              pTok.lemma(),
              pTok.upos(),
              pTok.xpos(),
              pTok.feats(),
              gTok.head(),
              gTok.deprel(),
              gTok.deps(),
              gTok.misc(),
              gTok.sourceWordIndex()));
        }
      }

      if (sentenceMwtExact) {
        exactMwtSentences++;
      }
      if (!strictOnly || sentenceMwtExact) {
        aligned.add(new ConlluSentence(gold.sentId(), sentenceText, mergedTokens, gold.mwtSpans()));
      }
    }

    AlignmentStats stats = new AlignmentStats(
        totalSentences,
        exactMwtSentences,
        aligned.size(),
        totalSurfaceWords,
        matchedSurfaceWords,
        totalTokens,
        uposMatches,
        lemmaMatches,
        exactFeatsMatches,
        goldFeatPairs,
        predFeatPairs,
        matchedFeatPairs,
        uposConfusions,
        mwtMismatchExamples,
        featMismatchCounts);
    return new AlignmentResult(aligned, stats);
  }

  /**
   * Projects raw Turkish text lines into CoNLL-U format (with unassigned {@code HEAD}/{@code DEPREL})
   * for unlabeled corpus distillation.
   */
  public int projectRawTextFile(
      Path inputText, Path outputConllu, boolean segmentParagraphs, int maxSentences)
      throws IOException {
    if (outputConllu.getParent() != null) {
      Files.createDirectories(outputConllu.getParent());
    }
    TurkishSentenceExtractor extractor =
        segmentParagraphs ? TurkishSentenceExtractor.DEFAULT : null;

    int count = 0;
    try (BufferedReader reader = Files.newBufferedReader(inputText, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(outputConllu, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        List<String> sentences = extractor != null
            ? extractor.fromParagraph(trimmed)
            : Collections.singletonList(trimmed);
        for (String sentence : sentences) {
          String s = sentence.trim();
          if (s.isEmpty()) {
            continue;
          }
          SentenceAnalysis sa = morphology.analyzeAndDisambiguate(s);
          if (sa.size() == 0) {
            continue;
          }
          count++;
          ConlluSentence projected = formatter.projectSentence("u_" + count, sa);
          if (count > 1) {
            writer.write('\n');
          }
          writer.write(projected.toConllu());
          if (maxSentences > 0 && count >= maxSentences) {
            return count;
          }
        }
      }
    }
    return count;
  }

  private List<ConlluToken> projectGoldSubtokensFallback(List<ConlluToken> goldSubtokens) {
    if (goldSubtokens.isEmpty()) {
      return Collections.emptyList();
    }
    List<WordAnalysis> subAnalyses = new ArrayList<>(goldSubtokens.size());
    StringBuilder joined = new StringBuilder();
    for (int i = 0; i < goldSubtokens.size(); i++) {
      if (i > 0) {
        joined.append(' ');
      }
      String form = goldSubtokens.get(i).form();
      joined.append(form);
      subAnalyses.add(morphology.analyze(form));
    }
    SentenceAnalysis disamb = morphology.disambiguate(joined.toString(), subAnalyses);
    List<ConlluToken> projected = new ArrayList<>(goldSubtokens.size());
    for (int i = 0; i < goldSubtokens.size(); i++) {
      ConlluToken gTok = goldSubtokens.get(i);
      SingleAnalysis best = disamb.getWordAnalyses().get(i).getBestAnalysis();
      projected.add(formatter.projectSingle(gTok.id(), gTok.form(), best));
    }
    return projected;
  }

  private static Map<Integer, List<ConlluToken>> groupBySourceWord(List<ConlluToken> tokens) {
    Map<Integer, List<ConlluToken>> map = new LinkedHashMap<>();
    for (ConlluToken token : tokens) {
      map.computeIfAbsent(token.sourceWordIndex(), k -> new ArrayList<>(2)).add(token);
    }
    return map;
  }

  public static void main(String[] args) throws IOException {
    Path inputConllu = null;
    Path projectText = null;
    Path outputConllu = null;
    Path reportPath = null;
    Dialect dialect = Dialect.UD_TURKIC_BOUN;
    MwtMode mwtMode = MwtMode.COPULA_AND_KI;
    boolean strictOnly = false;
    boolean segmentParagraphs = false;
    int maxSentences = -1;
    int topN = 20;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--input", "-i" -> inputConllu = Paths.get(args[++i]);
        case "--project-text" -> projectText = Paths.get(args[++i]);
        case "--output", "-o" -> outputConllu = Paths.get(args[++i]);
        case "--report" -> reportPath = Paths.get(args[++i]);
        case "--dialect" -> {
          String d = args[++i].toLowerCase(Locale.ROOT);
          dialect = "imst".equals(d) ? Dialect.UD_IMST : Dialect.UD_TURKIC_BOUN;
        }
        case "--mwt" -> {
          String m = args[++i].toLowerCase(Locale.ROOT);
          mwtMode = switch (m) {
            case "none" -> MwtMode.NONE;
            case "copula_only" -> MwtMode.COPULA_ONLY;
            default -> MwtMode.COPULA_AND_KI;
          };
        }
        case "--strict-only" -> strictOnly = true;
        case "--segment" -> segmentParagraphs = true;
        case "--max-sentences" -> maxSentences = Integer.parseInt(args[++i]);
        case "--top" -> topN = Integer.parseInt(args[++i]);
        case "--help", "-h" -> {
          printUsage();
          return;
        }
        default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
      }
    }

    if (inputConllu == null && projectText == null) {
      printUsage();
      System.exit(1);
    }

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    ConlluFormatter formatter = ConlluFormatter.builder()
        .dialect(dialect)
        .mwtMode(mwtMode)
        .includeDerivedFrom(false)
        .build();
    UdConlluAligner aligner = new UdConlluAligner(morphology, formatter);

    if (projectText != null) {
      if (outputConllu == null) {
        throw new IllegalArgumentException("--output is required with --project-text");
      }
      int projected = aligner.projectRawTextFile(
          projectText, outputConllu, segmentParagraphs, maxSentences);
      Log.info("Projected %d sentences from %s -> %s", projected, projectText, outputConllu);
      return;
    }

    List<ConlluSentence> gold = ConlluSentence.parseFile(inputConllu);
    if (maxSentences > 0 && gold.size() > maxSentences) {
      gold = gold.subList(0, maxSentences);
    }
    AlignmentResult result = aligner.alignTreebank(gold, strictOnly);
    String report = result.stats().formatReport(topN);
    System.out.print(report);

    if (reportPath != null) {
      if (reportPath.getParent() != null) {
        Files.createDirectories(reportPath.getParent());
      }
      Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }
    if (outputConllu != null) {
      if (outputConllu.getParent() != null) {
        Files.createDirectories(outputConllu.getParent());
      }
      Files.writeString(
          outputConllu, ConlluSentence.formatAll(result.alignedSentences()), StandardCharsets.UTF_8);
      Log.info("Wrote %d aligned sentences to %s",
          result.alignedSentences().size(), outputConllu);
    }
  }

  private static void printUsage() {
    System.out.println("""
        Usage:
          Align & audit a gold UD CoNLL-U treebank:
            UdConlluAligner --input <gold.conllu> [--output <aligned.conllu>] [--report <audit.txt>]
                            [--dialect boun|imst] [--mwt copula_and_ki|copula_only|none]
                            [--strict-only] [--max-sentences N] [--top 20]

          Project unlabeled Turkish text into CoNLL-U (for distillation):
            UdConlluAligner --project-text <sentences.txt> --output <projected.conllu>
                            [--segment] [--dialect boun|imst] [--max-sentences N]
        """);
  }
}
