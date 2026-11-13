package zemberek.normalization;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.LmVocabulary;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;

/**
 * Tries to normalize a sentence by collecting candidate words from
 * <pre>
 * - lookup tables (manual and collected from a large context graph)
 * - split-combine heuristics
 * - ascii tolerant analysis
 * - informal morphological analysis
 * - spell checker
 * </pre>
 * It then finds the most likely sequence using Viterbi search algorithm over candidate lists, using
 * a compressed language model.
 */
public class TurkishSentenceNormalizer {

  TurkishMorphology morphology;
  private SmoothLm lm;
  private TurkishSpellChecker spellChecker;

  private ArrayListMultimap<String, String> lookupFromGraph;
  private ArrayListMultimap<String, String> lookupFromAscii;
  private ArrayListMultimap<String, String> lookupManual;
  private TurkishMorphology informalAsciiTolerantMorphology;
  private InformalAnalysisConverter analysisConverter;

  private Map<String, String> commonSplits = new HashMap<>();
  private Map<String, String> replacements = new HashMap<>();
  private HashSet<String> commonConnectedSuffixes = new HashSet<>();
  private HashSet<String> noSplitWords = new HashSet<>();
  boolean alwaysApplyDeasciifier = false;
  private Boolean spellCheckCapitalizedWords = null;
  private LatticeDeasciifier latticeDeasciifier;

  public TurkishSentenceNormalizer(
      TurkishMorphology morphology,
      Path dataRoot,
      Path languageModelPath) throws IOException {

    this.morphology = morphology;
    this.analysisConverter = new InformalAnalysisConverter(morphology.getWordGenerator());
    SmoothLm languageModel = SmoothLm.builder(languageModelPath).logBase(Math.E).build();
    Log.info("Language model = %s", languageModel.info());
    this.lm = languageModel;

    // TODO: spell checker should be an external parameter.
    StemEndingGraph graph = new StemEndingGraph(morphology);
    CharacterGraphDecoder decoder = new CharacterGraphDecoder(graph.stemGraph);
    this.spellChecker = new TurkishSpellChecker(
        morphology,
        decoder,
        CharacterGraphDecoder.DIACRITICS_IGNORING_MATCHER);

    this.lookupFromGraph = loadMultiMap(dataRoot.resolve("lookup-from-graph"));
    this.lookupFromAscii = loadMultiMap(dataRoot.resolve("ascii-map"));
    List<String> manualLookup =
        TextIO.loadLinesFromResource("normalization/candidates-manual");
    this.lookupManual = loadMultiMap(manualLookup);

    // remove words that exists in lookupManual from lookupFromGraph
    lookupManual.keySet().forEach(s -> lookupFromGraph.removeAll(s));

    this.informalAsciiTolerantMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .useInformalAnalysis()
        .ignoreDiacriticsInAnalysis()
        .build();
    this.latticeDeasciifier =
        LatticeDeasciifier.createWithDefaults(
            this.morphology, this.informalAsciiTolerantMorphology);

    List<String> splitLines = Files.readAllLines(dataRoot.resolve("split"), Charsets.UTF_8);
    for (String splitLine : splitLines) {
      String[] tokens = splitLine.split("=");
      commonSplits.put(tokens[0].trim(), tokens[1].trim());
    }

    this.commonConnectedSuffixes.addAll(TextIO.loadLinesFromResource(
        "normalization/question-suffixes"));
    this.commonConnectedSuffixes.addAll(Arrays.asList("de", "da", "ki"));

    this.noSplitWords.addAll(TextIO.loadLinesFromResource(
        "normalization/no-split"));

    List<String> replaceLines = TextIO.loadLinesFromResource(
        "normalization/multi-word-replacements");
    for (String replaceLine : replaceLines) {
      String[] tokens = replaceLine.split("=");
      replacements.put(tokens[0].trim(), tokens[1].trim());
    }
  }

  public void setAlwaysApplyDeasciifier(boolean alwaysApplyDeasciifier) {
    this.alwaysApplyDeasciifier = alwaysApplyDeasciifier;
  }

  /**
   * Enables {@link LatticeDeasciifier} (enabled by default) using its bundled default model while
   * sharing this normalizer's formal and informal/diacritic-tolerant {@link TurkishMorphology}
   * instances.
   */
  public TurkishSentenceNormalizer useLatticeDeasciifier() {
    this.latticeDeasciifier =
        LatticeDeasciifier.createWithDefaults(
            this.morphology, this.informalAsciiTolerantMorphology);
    return this;
  }

  /**
   * Switches to the legacy pattern-table {@link Deasciifier} instead of the default {@link
   * LatticeDeasciifier}.
   */
  public TurkishSentenceNormalizer useLegacyDeasciifier() {
    this.latticeDeasciifier = null;
    return this;
  }

  /**
   * Configures a custom {@link LatticeDeasciifier} instance (or {@code null} to use the legacy
   * pattern-table {@link Deasciifier}).
   */
  public TurkishSentenceNormalizer setLatticeDeasciifier(LatticeDeasciifier latticeDeasciifier) {
    this.latticeDeasciifier = latticeDeasciifier;
    return this;
  }

  /**
   * Controls whether out-of-vocabulary capitalized words are passed to the edit-distance spell
   * checker. When {@code false}, all capitalized OOV words in non-ALL-CAPS sentences are preserved
   * without edit-distance replacement (useful for mixed-language text and foreign proper nouns).
   * When {@code true}, all capitalized OOV words are spell-checked. When unset (default),
   * non-sentence-initial capitalized OOV words in mixed-case sentences are preserved while
   * sentence-initial words are still spell-checked.
   */
  public void setSpellCheckCapitalizedWords(boolean spellCheckCapitalizedWords) {
    this.spellCheckCapitalizedWords = spellCheckCapitalizedWords;
  }

  // load data with line format: "key=val1,val2"
  private ArrayListMultimap<String, String> loadMultiMap(Path path) throws IOException {
    List<String> lines = TextIO.loadLines(path);
    return loadMultiMap(lines);
  }

  private ArrayListMultimap<String, String> loadMultiMap(List<String> lines) {
    ArrayListMultimap<String, String> result = ArrayListMultimap.create();
    for (String line : lines) {
      int index = line.indexOf("=");
      if (index < 0) {
        throw new IllegalStateException("Line needs to have `=` symbol. But it is:" +
            line);
      }
      String key = line.substring(0, index).trim();
      String value = line.substring(index + 1).trim();
      if (value.indexOf(',') >= 0) {
        for (String token : Splitter.on(",").trimResults().split(value)) {
          result.put(key, token);
        }
      } else {
        result.put(key, value);
      }
    }
    return result;
  }

  /** Lightweight single-pass preprocessing token with cached type and word classification. */
  static final class SpanToken {
    final String text;
    final Token.Type type;
    final boolean word;

    SpanToken(Token token) {
      this(token.getText(), token.getType(), isWord(token));
    }

    SpanToken(String text, Token.Type type, boolean word) {
      this.text = text;
      this.type = type;
      this.word = word;
    }
  }

  static boolean isPureNonWord(Token.Type type) {
    return type == Type.Punctuation
        || type == Type.URL
        || type == Type.Email
        || type == Type.HashTag
        || type == Type.Mention
        || type == Type.Emoticon;
  }

  public String normalize(String sentence) {

    if (sentence.trim().length() == 0) {
      return sentence;
    }

    boolean requiresDeasciifier = alwaysApplyDeasciifier || probablyRequiresDeasciifier(sentence);
    List<SpanToken> tokens = preProcessTokens(sentence, requiresDeasciifier);
    if (tokens.isEmpty()) {
      return "";
    }

    List<Candidates> candidatesList = new ArrayList<>(tokens.size());
    Set<String> protectedCapitalizedWords = collectProtectedCapitalizedWords(sentence);

    for (int i = 0; i < tokens.size(); i++) {
      SpanToken currentToken = tokens.get(i);
      String current = currentToken.text;

      // Fast path for punctuation and pure non-word tokens.
      if (isPureNonWord(currentToken.type)) {
        candidatesList.add(
            new Candidates(current, Collections.singletonList(new Candidate(current))));
        continue;
      }

      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).text;
      String previous = i == 0 ? null : tokens.get(i - 1).text;

      LinkedHashSet<String> candidates = new LinkedHashSet<>(2);

      // add matches from manual lookup
      candidates.addAll(lookupManual.get(current));

      // add matches from random walk
      candidates.addAll(lookupFromGraph.get(current));

      // add matches from ascii equivalents.
      candidates.addAll(lookupFromAscii.get(current));

      // add matches from informal analysis to formal surface conversion.
      WordAnalysis analyses = informalAsciiTolerantMorphology.analyze(current);

      for (SingleAnalysis analysis : analyses) {
        if (analysis.containsInformalMorpheme()) {
          WordGenerator.Result result = analysisConverter.convert(current, analysis);
          if (result != null) {
            candidates.add(result.surface);
          }
        } else {
          List<WordGenerator.Result> results = morphology.getWordGenerator().generate(
              analysis.getDictionaryItem(),
              analysis.getMorphemes());
          String normalizedCurrent =
              TurkishMorphology.normalizeForAnalysis(current).replace("'", "");
          for (Result result : results) {
            if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(normalizedCurrent, result.surface)) {
              candidates.add(result.surface);
            }
          }
        }
      }

      // if there is no formal analysis and length is larger than 3,
      // get top 3 1-distance matches unless protected as a capitalized OOV word.
      if ((analyses.analysisCount() == 0)
          && current.length() > 3
          && !protectedCapitalizedWords.contains(current)) {

        List<String> spellCandidates = spellChecker
            .suggestForWord(current, previous, next, lm);
        int limit = Math.min(3, spellCandidates.size());
        for (int j = 0; j < limit; j++) {
          candidates.add(spellCandidates.get(j));
        }
      }

      // if still there is no match, add the word itself.
      if (candidates.isEmpty() || morphology.analyze(current).isCorrect()) {
        candidates.add(current);
      }

      List<Candidate> candObjs = new ArrayList<>(candidates.size());
      for (String cand : candidates) {
        candObjs.add(new Candidate(cand));
      }
      candidatesList.add(new Candidates(current, candObjs));
    }

    // Apply primitive-trellis Viterbi decoding and return result.
    return String.join(" ", decode(candidatesList));
  }

  private boolean hasAnalysis(WordAnalysis w) {
    for (SingleAnalysis s : w) {
      if (!s.isRuntime() && !s.isUnknown()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Represents a candidate word.
   */
  private static class Candidate {

    final String content;
    final float score;

    Candidate(String content) {
      this.content = content;
      this.score = 0f;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Candidate candidate = (Candidate) o;
      return content.equals(candidate.content);
    }

    @Override
    public int hashCode() {
      return content.hashCode();
    }

    @Override
    public String toString() {
      return "Candidate{" +
          "content='" + content + '\'' +
          ", score=" + score +
          '}';
    }
  }

  private static class Candidates {

    final String word;
    final List<Candidate> candidates;

    Candidates(String word, List<Candidate> candidates) {
      this.word = word;
      this.candidates = candidates;
    }

    @Override
    public String toString() {
      return "Candidates{" +
          "word='" + word + '\'' +
          ", candidates=" + candidates +
          '}';
    }
  }

  private static final int NGRAM_BEAM_SIZE = 5;

  private List<String> decode(List<Candidates> candidatesList) {
    if (candidatesList.isEmpty()) {
      return Collections.emptyList();
    }
    if (lm.getOrder() == 2) {
      return decodeBigramTrellis(candidatesList);
    }
    return decodeNgramBeamTrellis(candidatesList);
  }

  private List<String> decodeBigramTrellis(List<Candidates> candidatesList) {
    LmVocabulary vocab = lm.getVocabulary();
    int startIdx = vocab.indexOf(vocab.getSentenceStart());
    int endIdx = vocab.indexOf(vocab.getSentenceEnd());

    int tLen = candidatesList.size();
    int[][] backPointers = new int[tLen][];

    int[] prevVocabIdx = new int[] {startIdx};
    float[] prevScores = new float[] {0f};
    boolean[] prevIsIdentity = new boolean[] {true};

    for (int t = 0; t < tLen; t++) {
      Candidates step = candidatesList.get(t);
      String stepWord = step.word;
      List<Candidate> cands = step.candidates;
      int k = cands.size();
      int[] currVocabIdx = new int[k];
      float[] currScores = new float[k];
      boolean[] currIsIdentity = new boolean[k];
      int[] bp = new int[k];
      int prevLen = prevVocabIdx.length;

      for (int c = 0; c < k; c++) {
        Candidate cand = cands.get(c);
        int cVocab = vocab.indexOf(cand.content);
        currVocabIdx[c] = cVocab;
        currIsIdentity[c] = cand.content.equals(stepWord);

        float bestScore = Float.NEGATIVE_INFINITY;
        int bestPrev = 0;
        for (int p = 0; p < prevLen; p++) {
          float score = prevScores[p] + lm.getProbability(prevVocabIdx[p], cVocab) + cand.score;
          if (score > bestScore
              || (score == bestScore && prevIsIdentity[p] && !prevIsIdentity[bestPrev])) {
            bestScore = score;
            bestPrev = p;
          }
        }
        currScores[c] = bestScore;
        bp[c] = bestPrev;
      }

      backPointers[t] = bp;
      prevVocabIdx = currVocabIdx;
      prevScores = currScores;
      prevIsIdentity = currIsIdentity;
    }

    int bestLast = 0;
    float bestEndScore = Float.NEGATIVE_INFINITY;
    for (int p = 0; p < prevVocabIdx.length; p++) {
      float score = prevScores[p] + lm.getProbability(prevVocabIdx[p], endIdx);
      if (score > bestEndScore
          || (score == bestEndScore && prevIsIdentity[p] && !prevIsIdentity[bestLast])) {
        bestEndScore = score;
        bestLast = p;
      }
    }

    String[] decoded = new String[tLen];
    int idx = bestLast;
    for (int t = tLen - 1; t >= 0; t--) {
      decoded[t] = candidatesList.get(t).candidates.get(idx).content;
      idx = backPointers[t][idx];
    }
    return Arrays.asList(decoded);
  }

  private List<String> decodeNgramBeamTrellis(List<Candidates> candidatesList) {
    LmVocabulary vocab = lm.getVocabulary();
    int lmOrder = lm.getOrder();
    int histLen = lmOrder - 1;
    int startIdx = vocab.indexOf(vocab.getSentenceStart());
    int endIdx = vocab.indexOf(vocab.getSentenceEnd());

    int tLen = candidatesList.size();
    int[][] chosenCand = new int[tLen][];
    int[][] backState = new int[tLen][];

    int prevCount = 1;
    int[] prevHist = new int[histLen];
    Arrays.fill(prevHist, startIdx);
    float[] prevScores = new float[] {0f};
    boolean[] prevIsIdentity = new boolean[] {true};
    int[] ngramBuf = new int[lmOrder];

    for (int t = 0; t < tLen; t++) {
      Candidates step = candidatesList.get(t);
      String stepWord = step.word;
      List<Candidate> cands = step.candidates;
      int k = cands.size();
      int[] candVocab = new int[k];
      boolean[] candIsIdentity = new boolean[k];
      for (int c = 0; c < k; c++) {
        Candidate cand = cands.get(c);
        candVocab[c] = vocab.indexOf(cand.content);
        candIsIdentity[c] = cand.content.equals(stepWord);
      }

      int maxExpand = prevCount * k;
      int[] tmpHist = new int[maxExpand * histLen];
      float[] tmpScores = new float[maxExpand];
      boolean[] tmpIsIdentity = new boolean[maxExpand];
      int[] tmpCandIdx = new int[maxExpand];
      int[] tmpPrevIdx = new int[maxExpand];
      int nextCount = 0;

      for (int p = 0; p < prevCount; p++) {
        int hBase = p * histLen;
        System.arraycopy(prevHist, hBase, ngramBuf, 0, histLen);
        float baseScore = prevScores[p];

        for (int c = 0; c < k; c++) {
          int cVocab = candVocab[c];
          ngramBuf[histLen] = cVocab;
          float score = baseScore + lm.getProbability(ngramBuf) + cands.get(c).score;
          boolean isIdentity = prevIsIdentity[p] && candIsIdentity[c];

          // Deduplicate identical n-gram histories within the step (keep highest score).
          int dup = -1;
          for (int u = 0; u < nextCount; u++) {
            int uBase = u * histLen;
            boolean same = true;
            for (int h = 0; h < histLen - 1; h++) {
              if (tmpHist[uBase + h] != prevHist[hBase + 1 + h]) {
                same = false;
                break;
              }
            }
            if (same && (histLen == 0 || tmpHist[uBase + histLen - 1] == cVocab)) {
              dup = u;
              break;
            }
          }

          if (dup >= 0) {
            if (score > tmpScores[dup]
                || (score == tmpScores[dup] && isIdentity && !tmpIsIdentity[dup])) {
              tmpScores[dup] = score;
              tmpIsIdentity[dup] = isIdentity;
              tmpCandIdx[dup] = c;
              tmpPrevIdx[dup] = p;
            }
          } else {
            int dstBase = nextCount * histLen;
            if (histLen > 1) {
              System.arraycopy(prevHist, hBase + 1, tmpHist, dstBase, histLen - 1);
            }
            if (histLen > 0) {
              tmpHist[dstBase + histLen - 1] = cVocab;
            }
            tmpScores[nextCount] = score;
            tmpIsIdentity[nextCount] = isIdentity;
            tmpCandIdx[nextCount] = c;
            tmpPrevIdx[nextCount] = p;
            nextCount++;
          }
        }
      }

      int keepCount = Math.min(NGRAM_BEAM_SIZE, nextCount);
      int[] topOrder = topKIndices(tmpScores, nextCount, keepCount);
      int[] nextHist = new int[keepCount * histLen];
      float[] nextScores = new float[keepCount];
      boolean[] nextIsIdentity = new boolean[keepCount];
      int[] stepCand = new int[keepCount];
      int[] stepPrev = new int[keepCount];
      for (int rank = 0; rank < keepCount; rank++) {
        int src = topOrder[rank];
        System.arraycopy(tmpHist, src * histLen, nextHist, rank * histLen, histLen);
        nextScores[rank] = tmpScores[src];
        nextIsIdentity[rank] = tmpIsIdentity[src];
        stepCand[rank] = tmpCandIdx[src];
        stepPrev[rank] = tmpPrevIdx[src];
      }

      chosenCand[t] = stepCand;
      backState[t] = stepPrev;
      prevCount = keepCount;
      prevHist = nextHist;
      prevScores = nextScores;
      prevIsIdentity = nextIsIdentity;
    }

    int bestFinal = 0;
    float bestEndScore = Float.NEGATIVE_INFINITY;
    ngramBuf[histLen] = endIdx;
    for (int p = 0; p < prevCount; p++) {
      System.arraycopy(prevHist, p * histLen, ngramBuf, 0, histLen);
      float score = prevScores[p] + lm.getProbability(ngramBuf);
      if (score > bestEndScore
          || (score == bestEndScore && prevIsIdentity[p] && !prevIsIdentity[bestFinal])) {
        bestEndScore = score;
        bestFinal = p;
      }
    }

    String[] decoded = new String[tLen];
    int state = bestFinal;
    for (int t = tLen - 1; t >= 0; t--) {
      int candIndex = chosenCand[t][state];
      decoded[t] = candidatesList.get(t).candidates.get(candIndex).content;
      state = backState[t][state];
    }
    return Arrays.asList(decoded);
  }

  private static int[] topKIndices(float[] scores, int len, int k) {
    int[] top = new int[k];
    boolean[] used = new boolean[len];
    for (int r = 0; r < k; r++) {
      int best = -1;
      float bestVal = Float.NEGATIVE_INFINITY;
      for (int i = 0; i < len; i++) {
        if (!used[i] && (best < 0 || scores[i] > bestVal)) {
          best = i;
          bestVal = scores[i];
        }
      }
      top[r] = best;
      used[best] = true;
    }
    return top;
  }

  String preProcess(String sentence) {
    if (sentence.trim().isEmpty()) {
      return "";
    }
    boolean requiresDeasciifier = alwaysApplyDeasciifier || probablyRequiresDeasciifier(sentence);
    return joinTokens(preProcessTokens(sentence, requiresDeasciifier));
  }

  List<SpanToken> preProcessTokens(String sentence, boolean requiresDeasciifier) {
    if (requiresDeasciifier && latticeDeasciifier != null) {
      // Deasciify before Turkish.LOCALE lowercasing so uppercase ASCII 'I' ("Istanbul", "Isik",
      // "TUIK", "BIOS") and TitleCase proper-noun cues are not destroyed into dotless 'ı'.
      // Known-word mode leaves OOV typos and foreign words untouched for downstream spell-checking.
      sentence = latticeDeasciifier.deasciifyKnownWords(sentence);
    }
    String lower = sentence.toLowerCase(Turkish.LOCALE);
    List<Token> rawTokens = TurkishTokenizer.DEFAULT.tokenize(lower);
    if (rawTokens.isEmpty()) {
      return Collections.emptyList();
    }
    List<SpanToken> initialTokens = new ArrayList<>(rawTokens.size());
    for (Token t : rawTokens) {
      initialTokens.add(new SpanToken(t));
    }
    List<SpanToken> tokens = replaceCommonTokens(initialTokens);
    tokens = combineNecessaryTokens(tokens);
    tokens = splitNecessaryTokens(tokens, false);
    if (requiresDeasciifier) {
      if (latticeDeasciifier == null) {
        tokens = deasciifyValidTokens(tokens);
      } else if (!sameTokenTexts(initialTokens, tokens)) {
        String joined = joinTokens(tokens);
        String deasciified = latticeDeasciifier.deasciifyKnownWords(joined);
        if (!deasciified.equals(joined)) {
          List<Token> reTokenized = TurkishTokenizer.DEFAULT.tokenize(deasciified);
          List<SpanToken> updated = new ArrayList<>(reTokenized.size());
          for (Token t : reTokenized) {
            updated.add(new SpanToken(t));
          }
          tokens = updated;
        }
      }
    }
    tokens = combineNecessaryTokens(tokens);
    return splitNecessaryTokens(tokens, true);
  }

  private static boolean sameTokenTexts(List<SpanToken> a, List<SpanToken> b) {
    if (a.size() != b.size()) {
      return false;
    }
    for (int i = 0; i < a.size(); i++) {
      if (!a.get(i).text.equals(b.get(i).text)) {
        return false;
      }
    }
    return true;
  }

  private static String joinTokens(List<SpanToken> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(tokens.size() * 8);
    for (int i = 0; i < tokens.size(); i++) {
      if (i > 0) {
        sb.append(' ');
      }
      sb.append(tokens.get(i).text);
    }
    return sb.toString();
  }

  private List<SpanToken> replaceCommonTokens(List<SpanToken> tokens) {
    List<SpanToken> result = new ArrayList<>(tokens.size());
    for (SpanToken token : tokens) {
      String rep = replacements.get(token.text);
      if (rep != null) {
        appendTokenizedParts(rep, result);
      } else {
        result.add(token);
      }
    }
    return result;
  }

  private List<SpanToken> combineNecessaryTokens(List<SpanToken> tokens) {
    if (tokens.isEmpty()) {
      return tokens;
    }
    List<SpanToken> result = new ArrayList<>(tokens.size());
    boolean combined = false;
    int last = tokens.size() - 1;
    for (int i = 0; i < last; i++) {
      SpanToken first = tokens.get(i);
      SpanToken second = tokens.get(i + 1);
      if (!first.word || !second.word) {
        combined = false;
        result.add(first);
        continue;
      }
      if (combined) {
        combined = false;
        continue;
      }
      String c = combineCommon(first.text, second.text);
      if (c.length() > 0) {
        result.add(new SpanToken(c, first.type, true));
        combined = true;
      } else {
        result.add(first);
        combined = false;
      }
    }
    if (!combined) {
      result.add(tokens.get(last));
    }
    return result;
  }

  private List<SpanToken> splitNecessaryTokens(List<SpanToken> tokens, boolean useLookup) {
    List<SpanToken> result = new ArrayList<>(tokens.size());
    for (SpanToken token : tokens) {
      if (token.word) {
        String split = separateCommon(token.text, useLookup);
        if (split.equals(token.text)) {
          result.add(token);
        } else {
          appendTokenizedParts(split, result);
        }
      } else {
        result.add(token);
      }
    }
    return result;
  }

  private List<SpanToken> deasciifyValidTokens(List<SpanToken> tokens) {
    if (tokens.isEmpty()) {
      return tokens;
    }
    int n = tokens.size();
    int[] starts = new int[n];
    int[] ends = new int[n];
    StringBuilder joined = new StringBuilder(n * 8);
    for (int i = 0; i < n; i++) {
      if (i > 0) {
        joined.append(' ');
      }
      starts[i] = joined.length();
      joined.append(tokens.get(i).text);
      ends[i] = joined.length();
    }
    String source = joined.toString();
    String deasciified = Deasciifier.deasciify(source);
    if (deasciified.equals(source) || deasciified.length() != source.length()) {
      return tokens;
    }
    List<SpanToken> updated = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      SpanToken token = tokens.get(i);
      String deascWord = deasciified.substring(starts[i], ends[i]);
      if (token.word
          && !deascWord.equals(token.text)
          && morphology.analyze(deascWord).isCorrect()) {
        updated.add(new SpanToken(deascWord, token.type, true));
      } else {
        updated.add(token);
      }
    }
    return updated;
  }

  private static void appendTokenizedParts(String text, List<SpanToken> out) {
    List<Token> subTokens = TurkishTokenizer.DEFAULT.tokenize(text);
    for (Token t : subTokens) {
      out.add(new SpanToken(t));
    }
  }

  String deasciifyValidWords(String s) {
    String deasciified = Deasciifier.deasciify(s);
    if (deasciified.equals(s) || deasciified.length() != s.length()) {
      return deasciified;
    }
    List<Token> origTokens = TurkishTokenizer.DEFAULT.tokenize(s);
    StringBuilder sb = new StringBuilder(s);
    for (Token token : origTokens) {
      int start = token.getStart();
      int end = token.getEnd() + 1;
      if (start < 0 || end > deasciified.length()) {
        continue;
      }
      String origWord = s.substring(start, end);
      String deascWord = deasciified.substring(start, end);
      if (!origWord.equals(deascWord) && isWord(token) && morphology.analyze(deascWord).isCorrect()) {
        sb.replace(start, end, deascWord);
      }
    }
    return sb.toString();
  }

  /**
   * Tries to combine words that are written separately using heuristics. If it cannot combine,
   * returns empty string.
   *
   * Such as:
   * <pre>
   * göndere bilirler -> göndere bilirler
   * elma lar -> elmalar
   * ankara 'ya -> ankara'ya
   * </pre>
   */
  String combineCommon(String i1, String i2) {
    String combined = i1 + i2;
    if (i2.startsWith("'") || i2.startsWith("bil") || isAlwaysSolidCompound(i1, i2)) {
      WordAnalysis w = morphology.analyze(combined);
      if (hasAnalysis(w)) {
        return combined;
      }
    }
    if (!hasRegularAnalysis(i2)) {
      WordAnalysis w = morphology.analyze(combined);
      if (hasAnalysis(w)) {
        return combined;
      }
    }
    return "";
  }

  private static boolean isAlwaysSolidCompound(String i1, String i2) {
    return (("uluslar".equals(i1) || "milletler".equals(i1)) && i2.startsWith("arası"))
        || ("hiç".equals(i1) && i2.startsWith("bir"))
        || ("bir".equals(i1) && i2.startsWith("kaç"))
        || ("her".equals(i1) && i2.startsWith("hangi"))
        || ("vaz".equals(i1) && i2.startsWith("geç"));
  }

  /**
   * Returns true if only word is analysed with internal dictionary and analysis dictionary item is
   * not proper noun.
   */
  boolean hasRegularAnalysis(String s) {
    WordAnalysis a = morphology.analyze(s);
    return a.stream().anyMatch(k -> !k.isUnknown() && !k.isRuntime() &&
        k.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun &&
        k.getDictionaryItem().secondaryPos != SecondaryPos.Abbreviation
    );
  }

  /**
   * Tries to separate question words, conjunctions and common mistakes by looking from a lookup or
   * using heuristics. Such as:
   * <pre>
   * gelecekmisin -> gelecek misin
   * tutupda -> tutup da
   * öyleki -> öyle ki
   * olurya -> olur ya
   * </pre>
   */
  String separateCommon(String input, boolean useLookup) {
    if (noSplitWords.contains(input)) {
      return input;
    }
    if (useLookup && commonSplits.containsKey(input)) {
      return commonSplits.get(input);
    }
    if (!hasRegularAnalysis(input)) {
      for (int i = 1; i < input.length() - 1; i++) {
        String tail = input.substring(i);
        if (commonConnectedSuffixes.contains(tail)) {
          String head = input.substring(0, i);
          if (tail.length() < 3) {
            if (!lm.ngramExists(lm.getVocabulary().toIndexes(head, tail))) {
              return input;
            }
          }
          if (hasRegularAnalysis(head)) {
            return head + " " + tail;
          } else {
            return input;
          }
        }
      }
    }
    return input;
  }

  String separateBrute(String input, int minSize) {
    if (!hasRegularAnalysis(input)) {
      for (int i = minSize; i < input.length() - minSize; i++) {
        String head = input.substring(0, i);
        String tail = input.substring(i);
        if (hasRegularAnalysis(head) && hasRegularAnalysis(tail)) {
          return head + " " + tail;
        }
      }
    }
    return input;
  }


  /**
   * Makes a guess if input sentence requires deasciifier.
   */
  static boolean probablyRequiresDeasciifier(String sentence) {
    if (sentence.isEmpty()) {
      return false;
    }
    int turkishSpecCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (c == 'I') {
        continue;
      }
      if (TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        turkishSpecCount++;
      }
    }
    double ratio = turkishSpecCount * 1d / sentence.length();
    return ratio < 0.1;
  }

  String combineNecessaryWords(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    List<String> result = new ArrayList<>();
    boolean combined = false;
    for (int i = 0; i < tokens.size() - 1; i++) {
      Token first = tokens.get(i);
      Token second = tokens.get(i + 1);
      String firstS = first.getText();
      String secondS = second.getText();
      if (!isWord(first) || !isWord(second)) {
        combined = false;
        result.add(firstS);
        continue;
      }
      if (combined) {
        combined = false;
        continue;
      }
      String c = combineCommon(firstS, secondS);
      if (c.length() > 0) {
        result.add(c);
        combined = true;
      } else {
        result.add(first.getText());
        combined = false;
      }
    }
    if (!combined) {
      result.add(tokens.get(tokens.size() - 1).getText());
    }
    return String.join(" ", result);
  }

  static boolean isWord(Token token) {
    Token.Type type = token.getType();
    return type == Type.Word
        || type == Type.WordWithSymbol
        || type == Type.WordAlphanumerical
        || type == Type.UnknownWord;
  }

  String splitNecessaryWords(List<Token> tokens, boolean useLookup) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      if (isWord(token)) {
        result.add(separateCommon(text, useLookup));
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  String splitBruteForce(List<Token> tokens, int minSize) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      if (isWord(token)) {
        result.add(separateBrute(text, minSize));
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  String replaceCommon(List<Token> tokens) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      result.add(replacements.getOrDefault(text, text));
    }
    return String.join(" ", result);
  }

  private Set<String> collectProtectedCapitalizedWords(String sentence) {
    if (Boolean.TRUE.equals(spellCheckCapitalizedWords)
        || !containsUppercaseLetter(sentence)
        || !containsLowercaseLetter(sentence)) {
      return Collections.emptySet();
    }
    List<Token> rawTokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    if (Boolean.FALSE.equals(spellCheckCapitalizedWords)) {
      Set<String> result = new HashSet<>();
      for (Token token : rawTokens) {
        String text = token.getText();
        if (isWord(token) && !text.isEmpty() && Character.isUpperCase(text.charAt(0))) {
          result.add(text.toLowerCase(Turkish.LOCALE));
        }
      }
      return result;
    }
    boolean hasLowercaseInitialWord = false;
    for (Token token : rawTokens) {
      String text = token.getText();
      if (isWord(token) && !text.isEmpty() && Character.isLowerCase(text.charAt(0))) {
        hasLowercaseInitialWord = true;
        break;
      }
    }
    if (!hasLowercaseInitialWord) {
      return Collections.emptySet();
    }
    Set<String> result = new HashSet<>();
    boolean atSentenceStart = true;
    for (Token token : rawTokens) {
      String text = token.getText();
      if (isWord(token) && !text.isEmpty()) {
        if (!atSentenceStart && Character.isUpperCase(text.charAt(0))) {
          result.add(text.toLowerCase(Turkish.LOCALE));
        }
        atSentenceStart = false;
      } else if (isSentenceBoundary(text)) {
        atSentenceStart = true;
      }
    }
    return result;
  }

  private static boolean containsUppercaseLetter(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isUpperCase(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsLowercaseLetter(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLowerCase(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSentenceBoundary(String tokenText) {
    return ".".equals(tokenText)
        || "!".equals(tokenText)
        || "?".equals(tokenText)
        || "...".equals(tokenText);
  }
}
