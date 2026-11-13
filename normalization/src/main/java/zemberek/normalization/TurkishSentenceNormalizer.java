package zemberek.normalization;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SentenceAnalysis;
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

  private ImmutableListMultimap<String, String> lookupFromGraph;
  private ImmutableListMultimap<String, String> lookupManual;
  private TurkishMorphology informalAsciiTolerantMorphology;
  private InformalAnalysisConverter analysisConverter;
  private final InformalContractionExpander contractionExpander;

  private static final Pattern ASCII_DIGITS_PATTERN = Pattern.compile("\\d{1,6}");

  private static final Set<String> UNAMBIGUOUS_NUMERAL_SUFFIXES = new HashSet<>(Arrays.asList(
      "te", "den", "dan", "ten", "tan",
      "inci", "ıncı", "uncu", "üncü", "nci", "ncı", "ncu", "ncü",
      "deki", "daki", "teki", "taki",
      "leri", "ları", "lerde", "larda", "lerden", "lardan", "lerin", "ların",
      "li", "lı", "lu", "lü", "lik", "lık", "luk", "lük",
      "nin", "nın", "nun", "nün", "yle", "yla",
      "sında", "sinde", "sından", "sinden", "sına", "sine", "sını", "sini"));

  private static final Set<String> NUMERAL_LOCATIVE_SUFFIXES = new HashSet<>(Arrays.asList(
      "de", "da", "ta"));

  private static final Set<String> NUMERAL_LOCATIVE_ANCHORS = new HashSet<>(Arrays.asList(
      "saat", "sayfa", "madde", "bölüm", "numara", "no", "kat", "kanal", "peron", "hat"));

  private Map<String, String> commonSplits = new HashMap<>();
  private Map<String, String> replacements = new HashMap<>();
  private HashSet<String> commonConnectedSuffixes = new HashSet<>();
  private HashSet<String> noSplitWords = new HashSet<>();
  private final ElongationNormalizer elongationNormalizer;
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

    List<String> manualLookup =
        TextIO.loadLinesFromResource("normalization/candidates-manual");
    this.lookupManual = loadMultiMap(manualLookup);
    this.lookupFromGraph =
        loadMultiMap(dataRoot.resolve("lookup-from-graph"), lookupManual.keySet());

    this.informalAsciiTolerantMorphology = TurkishMorphology.builder()
        .setLexicon(morphology.getLexicon())
        .useInformalAnalysis()
        .ignoreDiacriticsInAnalysis()
        .setCache(AnalysisCache.builder().dynamicCacheSize(256, 1_024).build())
        .setAmbiguityResolver((s, a) -> new SentenceAnalysis(s, Collections.emptyList()))
        .build();
    this.latticeDeasciifier =
        LatticeDeasciifier.createWithDefaults(
            this.morphology, this.informalAsciiTolerantMorphology);
    this.contractionExpander = new InformalContractionExpander(
        morphology, informalAsciiTolerantMorphology, analysisConverter);

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

    this.elongationNormalizer = new ElongationNormalizer(
        this::isValidLexicalWord,
        this::unigramScore);
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
  private ImmutableListMultimap<String, String> loadMultiMap(
      Path path, Set<String> excludedKeys) throws IOException {
    return loadMultiMap(TextIO.loadLines(path), excludedKeys);
  }

  private ImmutableListMultimap<String, String> loadMultiMap(List<String> lines) {
    return loadMultiMap(lines, Collections.emptySet());
  }

  private ImmutableListMultimap<String, String> loadMultiMap(
      List<String> lines, Set<String> excludedKeys) {
    ImmutableListMultimap.Builder<String, String> result =
        ImmutableListMultimap.builderWithExpectedKeys(lines.size());
    result.expectedValuesPerKey(1);
    for (String line : lines) {
      int index = line.indexOf("=");
      if (index < 0) {
        throw new IllegalStateException("Line needs to have `=` symbol. But it is:" +
            line);
      }
      String key = line.substring(0, index).trim();
      if (excludedKeys.contains(key)) {
        continue;
      }
      String value = line.substring(index + 1).trim();
      if (value.indexOf(',') >= 0) {
        for (String token : Splitter.on(",").trimResults().split(value)) {
          result.put(key, token);
        }
      } else {
        result.put(key, value);
      }
    }
    return result.build();
  }

  public String normalize(String sentence) {

    if(sentence.trim().length() == 0) {
      return sentence;
    }

    String processed = preProcess(sentence);

    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(processed);
    if (tokens.isEmpty()) {
      return "";
    }

    List<Candidates> candidatesList = new ArrayList<>();
    Set<String> protectedCapitalizedWords = collectProtectedCapitalizedWords(sentence);

    for (int i = 0; i < tokens.size(); i++) {

      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();

      LinkedHashSet<String> candidates = new LinkedHashSet<>(2);

      // add matches from manual lookup
      candidates.addAll(lookupManual.get(current));

      // add matches from random walk
      candidates.addAll(lookupFromGraph.get(current));

      boolean currentHasRegularAnalysis = hasRegularAnalysis(current);
      if (currentHasRegularAnalysis
          && (!lookupManual.containsKey(current) || lookupManual.get(current).contains(current))) {
        // Place valid input ahead of ASCII/diacritic distractors so LM tie-breaking preserves it.
        candidates.add(current);
      }

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
              if (currentHasRegularAnalysis
                  && stripsExplicitTurkishDiacritic(normalizedCurrent, result.surface)) {
                continue;
              }
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
        if (spellCandidates.size() > 3) {
          spellCandidates = new ArrayList<>(spellCandidates.subList(0, 3));
        }
        candidates.addAll(spellCandidates);
      }

      // if still there is no match, add the word itself.
      if (candidates.isEmpty() || morphology.analyze(current).isCorrect()) {
        candidates.add(current);
      }

      Candidates result = new Candidates(
          currentToken.getText(),
          candidates.stream().map(Candidate::new).collect(Collectors.toList()));

      candidatesList.add(result);
    }
    // Apply Viterbi decoding and return result.
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

  private static class Hypothesis implements Scorable {

    // for a three gram model, holds the 2 history words.
    Candidate[] history;
    Candidate current;

    // required for back tracking.
    Hypothesis previous;

    float score;

    @Override
    public float getScore() {
      return score;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Hypothesis that = (Hypothesis) o;

      if (!Arrays.equals(history, that.history)) {
        return false;
      }
      return current.equals(that.current);
    }

    @Override
    public int hashCode() {
      int result = Arrays.hashCode(history);
      result = 31 * result + current.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Hypothesis{" +
          "history=" + Arrays.toString(history) +
          ", current=" + current +
          ", score=" + score +
          '}';
    }
  }

  /**
   * Represents a candidate word.
   */
  private static class Candidate {

    final String content;
    final float score;

    Candidate(String content) {
      this.content = content;
      score = 1f;
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

    String word;
    List<Candidate> candidates;

    Candidates(String word,
        List<Candidate> candidates) {
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

  private static Candidate START = new Candidate("<s>");
  private static Candidate END = new Candidate("</s>");
  private static Candidates END_CANDIDATES =
      new Candidates("</s>", Collections.singletonList(END));

  private List<String> decode(List<Candidates> candidatesList) {

    ActiveList<Hypothesis> current = new ActiveList<>();
    ActiveList<Hypothesis> next = new ActiveList<>();

    // Pad with END tokens.
    candidatesList.add(END_CANDIDATES);

    Hypothesis initial = new Hypothesis();
    int lmOrder = lm.getOrder();
    initial.history = new Candidate[lmOrder - 1];
    Arrays.fill(initial.history, START);
    initial.current = START;
    initial.score = 0f;
    current.add(initial);

    for (Candidates candidates : candidatesList) {

      for (Hypothesis h : current) {
        for (Candidate c : candidates.candidates) {
          Hypothesis newHyp = new Hypothesis();
          Candidate[] hist = new Candidate[lmOrder - 1];
          if (lmOrder > 2) {
            System.arraycopy(h.history, 1, hist, 0, lmOrder - 2);
          }
          if (hist.length > 0) {
            hist[hist.length - 1] = h.current;
          }
          newHyp.current = c;
          newHyp.history = hist;
          newHyp.previous = h;

          // score calculation.
          int[] indexes = new int[lmOrder];
          for (int j = 0; j < lmOrder - 1; j++) {
            indexes[j] = lm.getVocabulary().indexOf(hist[j].content);
          }
          indexes[lmOrder - 1] = lm.getVocabulary().indexOf(c.content);
          float score = lm.getProbability(indexes);

          newHyp.score = h.score + score;
          next.add(newHyp);
        }
      }
      current = next;
      next = new ActiveList<>();
    }

    // back track to find best sequence.
    Hypothesis best = current.getBest();
    List<String> seq = new ArrayList<>();
    Hypothesis h = best;
    // skip </s>
    h = h.previous;
    while (h != null && h.current != START) {
      seq.add(h.current.content);
      h = h.previous;
    }
    Collections.reverse(seq);
    return seq;
  }

  String preProcess(String sentence) {
    boolean requiresDeasciifier = alwaysApplyDeasciifier || probablyRequiresDeasciifier(sentence);
    boolean preserveValidWords = !alwaysApplyDeasciifier && hasTurkishSpecificChars(sentence);
    if (requiresDeasciifier && latticeDeasciifier != null) {
      // Deasciify before Turkish.LOCALE lowercasing so uppercase ASCII 'I' ("Istanbul", "Isik",
      // "TUIK", "BIOS") and TitleCase proper-noun cues are not destroyed into dotless 'ı'.
      // Known-word mode leaves OOV typos and foreign words untouched for downstream spell-checking.
      sentence = latticeDeasciifier.deasciifyKnownWords(sentence);
    }
    sentence = sentence.toLowerCase(Turkish.LOCALE);
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    String initial = joinTokens(tokens);
    String prev = initial;
    String s = replaceCommon(tokens);
    tokens = reTokenizeIfChanged(prev, s, tokens);
    prev = s;
    s = normalizeElongations(tokens);
    tokens = reTokenizeIfChanged(prev, s, tokens);
    prev = s;
    s = combineNecessaryWords(tokens);
    String afterFirstCombine = s;
    tokens = reTokenizeIfChanged(prev, s, tokens);
    prev = s;
    s = splitNecessaryWords(tokens, false);
    if (requiresDeasciifier) {
      if (latticeDeasciifier == null) {
        s = deasciifyValidWords(s, preserveValidWords);
      } else if (!s.equals(initial)) {
        s = latticeDeasciifier.deasciifyKnownWords(s);
      }
    }
    tokens = reTokenizeIfChanged(prev, s, tokens);
    if (!s.equals(afterFirstCombine)) {
      prev = s;
      s = combineNecessaryWords(tokens);
      tokens = reTokenizeIfChanged(prev, s, tokens);
    }
    return splitNecessaryWords(tokens, true);
  }

  private static String joinTokens(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(tokens.size() * 6);
    sb.append(tokens.get(0).getText());
    for (int i = 1; i < tokens.size(); i++) {
      sb.append(' ').append(tokens.get(i).getText());
    }
    return sb.toString();
  }

  private static List<Token> reTokenizeIfChanged(String prev, String next, List<Token> current) {
    return next.equals(prev) ? current : TurkishTokenizer.DEFAULT.tokenize(next);
  }

  String deasciifyValidWords(String s) {
    return deasciifyValidWords(s, false);
  }

  String deasciifyValidWords(String s, boolean preserveValidWords) {
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
      if (!origWord.equals(deascWord)
          && isWord(token)
          && !(preserveValidWords && hasRegularAnalysis(origWord))
          && morphology.analyze(deascWord).isCorrect()) {
        sb.replace(start, end, deascWord);
      }
    }
    return sb.toString();
  }

  static boolean hasTurkishSpecificChars(String sentence) {
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (c != 'I' && TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        return true;
      }
    }
    return false;
  }

  private static boolean stripsExplicitTurkishDiacritic(String src, String cand) {
    if (src.length() != cand.length()) {
      return false;
    }
    for (int i = 0; i < src.length(); i++) {
      char s = src.charAt(i);
      char c = cand.charAt(i);
      if ((s == 'ç' && c == 'c')
          || (s == 'ğ' && c == 'g')
          || (s == 'ö' && c == 'o')
          || (s == 'ş' && c == 's')
          || (s == 'ü' && c == 'u')) {
        return true;
      }
    }
    return false;
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
    int letterCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (Character.isLetter(c)) {
        letterCount++;
      }
      if (c == 'I') {
        continue;
      }
      if (TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        turkishSpecCount++;
      }
    }
    if (letterCount == 0) {
      return false;
    }
    double ratio = turkishSpecCount * 1d / letterCount;
    return ratio < 0.05;
  }

  String normalizeElongations(List<Token> tokens) {
    List<String> result = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      String text = token.getText();
      result.add(isWord(token) ? elongationNormalizer.normalizeWord(text) : text);
    }
    return String.join(" ", result);
  }

  private boolean isValidLexicalWord(String word) {
    WordAnalysis analysis = morphology.analyze(word);
    return analysis.stream().anyMatch(
        k -> !k.isUnknown() && !k.isRuntime()
            && k.getDictionaryItem().secondaryPos != SecondaryPos.Abbreviation);
  }

  private double unigramScore(String word) {
    if (!lm.getVocabulary().contains(word)) {
      return Double.NEGATIVE_INFINITY;
    }
    return lm.getUnigramProbability(lm.getVocabulary().indexOf(word));
  }

  String combineNumeralSuffix(String prevWord, String number, String suffix) {
    if (suffix.length() < 2) {
      return "";
    }
    boolean allowed = UNAMBIGUOUS_NUMERAL_SUFFIXES.contains(suffix)
        || (NUMERAL_LOCATIVE_SUFFIXES.contains(suffix)
            && NUMERAL_LOCATIVE_ANCHORS.contains(prevWord));
    return allowed ? resolveNumeralSuffix(number, suffix) : "";
  }

  private String normalizeInflectedNumeral(String text) {
    int apo = text.indexOf('\'');
    if (apo < 0) {
      apo = text.indexOf('’');
    }
    if (apo <= 0 || apo == text.length() - 1) {
      return text;
    }
    String number = text.substring(0, apo);
    String suffix = text.substring(apo + 1);
    if (!ASCII_DIGITS_PATTERN.matcher(number).matches()
        || (!UNAMBIGUOUS_NUMERAL_SUFFIXES.contains(suffix)
            && !NUMERAL_LOCATIVE_SUFFIXES.contains(suffix))) {
      return text;
    }
    if (!tryNumeralAnalysis(number + "'" + suffix).isEmpty()) {
      return text;
    }
    String resolved = resolveNumeralSuffix(number, suffix);
    return resolved.isEmpty() ? text : resolved;
  }

  private String resolveNumeralSuffix(String number, String suffix) {
    String result = tryNumeralAnalysis(number + "'" + suffix);
    if (!result.isEmpty()) {
      return result;
    }
    char firstChar = suffix.charAt(0);
    if (firstChar == 'd' || firstChar == 't') {
      char toggled = (firstChar == 'd') ? 't' : 'd';
      result = tryNumeralAnalysis(number + "'" + toggled + suffix.substring(1));
      if (!result.isEmpty()) {
        return result;
      }
    }
    if ("inci".equals(suffix) || "ıncı".equals(suffix)
        || "uncu".equals(suffix) || "üncü".equals(suffix)) {
      return tryNumeralAnalysis(number + "'" + suffix.substring(1));
    }
    return switch (suffix) {
      case "nci" -> tryNumeralAnalysis(number + "'inci");
      case "ncı" -> tryNumeralAnalysis(number + "'ıncı");
      case "ncu" -> tryNumeralAnalysis(number + "'uncu");
      case "ncü" -> tryNumeralAnalysis(number + "'üncü");
      default -> "";
    };
  }

  private String tryNumeralAnalysis(String candidate) {
    WordAnalysis analysis = morphology.analyze(candidate);
    boolean validNumeral = analysis.stream().anyMatch(
        k -> !k.isUnknown() && k.getDictionaryItem().primaryPos == PrimaryPos.Numeral);
    return validNumeral ? candidate : "";
  }

  String combineNecessaryWords(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    List<String> result = new ArrayList<>();
    boolean combined = false;
    for (int i = 0; i < tokens.size() - 1; i++) {
      if (combined) {
        combined = false;
        continue;
      }
      Token first = tokens.get(i);
      Token second = tokens.get(i + 1);
      String firstS = first.getText();
      String secondS = second.getText();
      if (first.getType() == Type.Number
          && isWord(second)
          && ASCII_DIGITS_PATTERN.matcher(firstS).matches()) {
        String prevWord = i > 0 ? tokens.get(i - 1).getText() : "";
        String combinedNum = combineNumeralSuffix(prevWord, firstS, secondS);
        if (!combinedNum.isEmpty()) {
          result.add(combinedNum);
          combined = true;
          continue;
        }
      }
      if (!isWord(first) || !isWord(second)) {
        result.add(firstS);
        continue;
      }
      String c = combineCommon(firstS, secondS);
      if (c.length() > 0) {
        result.add(c);
        combined = true;
      } else {
        result.add(firstS);
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
    List<String> result = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      String text = token.getText();
      String replacement = replacements.get(text);
      if (replacement != null) {
        result.add(replacement);
      } else if (isWord(token)) {
        result.add(contractionExpander.expandWord(text));
      } else if (token.getType() == Type.Number) {
        result.add(normalizeInflectedNumeral(text));
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  private Set<String> collectProtectedCapitalizedWords(String sentence) {
    if (Boolean.TRUE.equals(spellCheckCapitalizedWords) || !containsLowercaseLetter(sentence)) {
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
