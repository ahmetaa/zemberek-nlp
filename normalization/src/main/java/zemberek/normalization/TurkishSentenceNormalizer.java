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
import java.util.stream.Collectors;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.normalization.deasciifier.Deasciifier;
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

  public String normalize(String sentence) {

    if(sentence.trim().length() == 0) {
      return sentence;
    }

    String processed = preProcess(sentence);

    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(processed);

    List<Candidates> candidatesList = new ArrayList<>();

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

      // add matches from ascii equivalents.
      // TODO: this may decrease accuracy. Also, this can be eliminated with ascii tolerant analyzer.
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
          for (Result result : results) {
            candidates.add(result.surface);
          }
        }
      }

      // if there is no formal analysis and length is larger than 5,
      // get top 3 1 distance matches.
      if ((analyses.analysisCount() == 0) && current.length() > 3) {

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
            System.arraycopy(h.history, 1, hist, 0, lmOrder - 1);
          }
          hist[hist.length - 1] = h.current;
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
    sentence = sentence.toLowerCase(Turkish.LOCALE);
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    String s = replaceCommon(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, false);
    if (alwaysApplyDeasciifier || probablyRequiresDeasciifier(s)) {
      s = Deasciifier.deasciify(s);
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    return splitNecessaryWords(tokens, true);
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
    if (i2.startsWith("'") || i2.startsWith("bil")) {
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
    int turkishSpecCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (c == 'ı' || c == 'I') {
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
}
