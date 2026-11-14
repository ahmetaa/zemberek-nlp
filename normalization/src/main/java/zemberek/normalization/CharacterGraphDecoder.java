package zemberek.normalization;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import zemberek.core.ScoredItem;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntMap;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;

public class CharacterGraphDecoder {

  public static final Map<Character, String> TURKISH_FQ_NEAR_KEY_MAP = Map.ofEntries(
      entry('a', "eüs"), entry('b', "svn"), entry('c', "vçx"), entry('ç', "czö"),
      entry('d', "orsf"), entry('e', "iawr"), entry('f', "gd"), entry('g', "fğh"),
      entry('ğ', "gıpü"), entry('h', "npgj"), entry('ı', "ğou"), entry('i', "ueş"),
      entry('j', "öhk"), entry('k', "tmjl"), entry('l', "mykş"), entry('m', "klnö"),
      entry('n', "rhbm"), entry('o', "ıdp"), entry('ö', "jvmç"), entry('p', "hqoğ"),
      entry('r', "dnet"), entry('s', "zbad"), entry('ş', "yli"), entry('t', "ükry"),
      entry('u', "iyı"), entry('ü', "atğ"), entry('v', "öcb"), entry('y', "lştu"),
      entry('z', "çsx"), entry('x', "wzc"), entry('q', "pqw"), entry('w', "qxe"));

  public static final Map<Character, String> TURKISH_Q_NEAR_KEY_MAP = Map.ofEntries(
      entry('a', "s"), entry('b', "vn"), entry('c', "vx"), entry('ç', "ö"),
      entry('d', "sf"), entry('e', "wr"), entry('f', "gd"), entry('g', "fh"),
      entry('ğ', "pü"), entry('h', "gj"), entry('ı', "ou"), entry('i', "ş"),
      entry('j', "hk"), entry('k', "jl"), entry('l', "kş"), entry('m', "nö"),
      entry('n', "bm"), entry('o', "ıp"), entry('ö', "mç"), entry('p', "oğ"),
      entry('r', "et"), entry('s', "ad"), entry('ş', "li"), entry('t', "ry"),
      entry('u', "yı"), entry('ü', "ğ"), entry('v', "cb"), entry('y', "tu"),
      entry('z', "x"), entry('x', "zc"), entry('q', "w"), entry('w', "qe"));

  static final float INSERTION_PENALTY = 1;
  static final float DELETION_PENALTY = 1;
  static final float SUBSTITUTION_PENALTY = 1;
  static final float NEAR_KEY_SUBSTITUTION_PENALTY = 0.5f;
  static final float TRANSPOSITION_PENALTY = 1;
  public static final DiacriticsIgnoringMatcher DIACRITICS_IGNORING_MATCHER =
      new DiacriticsIgnoringMatcher();

  public final float maxPenalty;
  public final boolean checkNearKeySubstitution;
  public Map<Character, String> nearKeyMap = Map.of();
  private CharacterGraph graph = new CharacterGraph();

  public CharacterGraphDecoder(float maxPenalty) {
    this.maxPenalty = maxPenalty;
    this.checkNearKeySubstitution = false;
  }

  public CharacterGraphDecoder() {
    this(1);
  }

  public CharacterGraphDecoder(CharacterGraph graph) {
    this.graph = graph;
    this.maxPenalty = 1;
    this.checkNearKeySubstitution = false;
  }

  public CharacterGraphDecoder(float maxPenalty, Map<Character, String> nearKeyMap) {
    this.maxPenalty = maxPenalty;
    this.nearKeyMap = Collections.unmodifiableMap(nearKeyMap);
    this.checkNearKeySubstitution = true;
  }

  public CharacterGraph getGraph() {
    return graph;
  }

  private String process(String str) {
    return removeIgnoredChars(str.toLowerCase(Turkish.LOCALE));
  }

  static String removeIgnoredChars(String s) {
    int len = s.length();
    StringBuilder sb = null;
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c == '\'' || c == '’' || c == '.') {
        if (sb == null) {
          sb = new StringBuilder(len - 1);
          sb.append(s, 0, i);
        }
      } else if (sb != null) {
        sb.append(c);
      }
    }
    return sb != null ? sb.toString() : s;
  }

  public void addWord(String word) {
    graph.addWord(process(word), Node.TYPE_WORD);
  }

  void addWord(String indexKey, String storedWord) {
    graph.addWord(indexKey, Node.TYPE_WORD, storedWord);
  }

  public void addWords(String... words) {
    for (String word : words) {
      addWord(word);
    }
  }

  public void addWords(List<String> vocabulary) {
    for (String word : vocabulary) {
      addWord(word);
    }
  }

  /**
   * Returns suggestions sorted by penalty.
   */
  public List<ScoredItem<String>> getSuggestionsWithScores(String input) {
    Decoder decoder = new Decoder();
    return getMatches(input, decoder);
  }

  private List<ScoredItem<String>> getMatches(String input, Decoder decoder) {
    FloatValueMap<String> results = decoder.decode(input);

    List<ScoredItem<String>> res = new ArrayList<>(results.size());
    for (String result : results) {
      res.add(new ScoredItem<>(result, results.get(result)));
    }
    res.sort((a, b) -> Float.compare(a.score, b.score));
    return res;
  }

  public List<ScoredItem<String>> getSuggestionsWithScores(String input, CharMatcher matcher) {
    Decoder decoder = new Decoder(matcher);
    return getMatches(input, decoder);
  }

  public FloatValueMap<String> decode(String input) {
    return new Decoder().decode(input);
  }

  public List<String> getSuggestions(String input) {
    return new Decoder().decode(input).getKeyList();
  }

  public List<String> getSuggestions(String input, CharMatcher matcher) {
    return new Decoder(matcher).decode(input).getKeyList();
  }

  public List<String> getSuggestionsSorted(String input) {
    return getSuggestionsWithScores(input).stream()
        .map(s -> s.item)
        .toList();
  }

  enum Operation {
    NO_ERROR, INSERTION, DELETION, SUBSTITUTION, TRANSPOSITION, N_A
  }

  public interface CharMatcher {

    char[] matches(char c);
  }

  static class Hypothesis implements Comparable<Hypothesis> {

    Operation operation;
    int charIndex;
    Node node;
    float penalty;
    String word;
    String ending;
    Hypothesis previous;

    Hypothesis(Hypothesis previous, Node node, float penalty, Operation operation, String word,
        String ending) {
      this.previous = previous;
      this.node = node;
      this.penalty = penalty;
      this.charIndex = -1;
      this.operation = operation;
      this.word = word;
      this.ending = ending;
    }

    Hypothesis(Hypothesis previous, Node node, float penalty, int charIndex, Operation operation,
        String word, String ending) {
      this.previous = previous;
      this.node = node;
      this.penalty = penalty;
      this.charIndex = charIndex;
      this.operation = operation;
      this.word = word;
      this.ending = ending;
    }

    String backTrack() {
      StringBuilder sb = new StringBuilder();
      Hypothesis p = previous;
      while (p.node.chr != 0) {
        if (p.node != p.previous.node) {
          sb.append(p.node.chr);
        }
        p = p.previous;
      }
      return sb.reverse().toString();
    }

    String getContent() {
      String w = word == null ? "" : word;
      String e = ending == null ? "" : ending;
      return w + e;
    }

    void setWord(Node node) {
      if (node.word == null) {
        return;
      }
      if (node.getType() == Node.TYPE_WORD) {
        this.word = node.word;
      } else if (node.getType() == Node.TYPE_ENDING) {
        this.ending = node.word;
      }
    }

    Hypothesis getNew(Node node, float penaltyToAdd, Operation operation) {
      return new Hypothesis(this, node, this.penalty + penaltyToAdd, charIndex, operation,
          this.word, this.ending);
    }

    Hypothesis getNewMoveForward(Node node, float penaltyToAdd, Operation operation) {
      return new Hypothesis(this, node, this.penalty + penaltyToAdd, charIndex + 1, operation,
          this.word, this.ending);
    }

    Hypothesis getNew(Node node, float penaltyToAdd, int index, Operation operation) {
      return new Hypothesis(this, node, this.penalty + penaltyToAdd, index, operation, this.word,
          this.ending);
    }

    Hypothesis getNew(float penaltyToAdd, Operation operation) {
      return new Hypothesis(this, this.node, this.penalty + penaltyToAdd, charIndex, operation,
          this.word, this.ending);
    }

    @Override
    public int compareTo(Hypothesis o) {
      return Float.compare(penalty, o.penalty);
    }

    @Override
    public String toString() {
      return "Hypothesis{" +
          "previous=" + backTrack() + " " + previous.operation +
          ", node=" + node +
          ", penalty=" + penalty +
          ", index=" + charIndex +
          ", OP=" + operation.name() +
          '}';
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

      if (charIndex != that.charIndex) {
        return false;
      }
      // TODO: this should not be here.
      if (Float.compare(that.penalty, penalty) != 0) {
        return false;
      }
      if (!node.equals(that.node)) {
        return false;
      }
      if (!Objects.equals(word, that.word)) {
        return false;
      }
      return Objects.equals(ending, that.ending);
    }

    @Override
    public int hashCode() {
      int result = charIndex;
      result = 31 * result + node.hashCode();
      // TODO: this should not be here.
      result = 31 * result + (penalty != +0.0f ? Float.floatToIntBits(penalty) : 0);
      result = 31 * result + (word != null ? word.hashCode() : 0);
      result = 31 * result + (ending != null ? ending.hashCode() : 0);
      return result;
    }
  }

  private static class DiacriticsIgnoringMatcher implements CharMatcher {

    static IntMap<char[]> map = new IntMap<>();

    public DiacriticsIgnoringMatcher() {
      String allLetters = TurkishAlphabet.INSTANCE.getAllLetters() + "+.,'-";

      for (int i = 0; i < allLetters.length(); i++) {
        char[] ca = new char[1];
        char c = allLetters.charAt(i);
        ca[0] = c;
        map.put(c, ca);
      }
      // override some
      map.put('a', new char[]{'a', 'â'});
      map.put('c', new char[]{'c', 'ç'});
      map.put('g', new char[]{'g', 'ğ'});
      map.put('ı', new char[]{'ı', 'i', 'î'});
      map.put('i', new char[]{'i', 'ı', 'î'});
      map.put('o', new char[]{'o', 'ö'});
      map.put('s', new char[]{'s', 'ş'});
      map.put('u', new char[]{'u', 'ü', 'û'});
      map.put('A', new char[]{'A', 'Â'});
      map.put('C', new char[]{'C', 'Ç'});
      map.put('G', new char[]{'G', 'Ğ'});
      map.put('I', new char[]{'I', 'İ', 'Î'});
      map.put('İ', new char[]{'İ', 'I', 'Î'});
      map.put('O', new char[]{'O', 'Ö'});
      map.put('S', new char[]{'S', 'Ş'});
      map.put('U', new char[]{'U', 'Ü', 'Û'});
    }

    @Override
    public char[] matches(char c) {
      char[] res = map.get(c);
      return res == null ? new char[]{c} : res;
    }
  }

  private class Decoder {

    FloatValueMap<String> finished = new FloatValueMap<>(8);
    CharMatcher matcher;

    Decoder() {
      this(null);
    }

    Decoder(CharMatcher matcher) {
      this.matcher = matcher;
    }

    FloatValueMap<String> decode(String input) {
      Hypothesis rootHyp = new Hypothesis(null, graph.getRoot(), 0, Operation.N_A, null, null);
      Set<Hypothesis> current = expand(rootHyp, input);
      while (!current.isEmpty()) {
        Set<Hypothesis> next = new HashSet<>();
        for (Hypothesis hypothesis : current) {
          next.addAll(expand(hypothesis, input));
        }
        current = next;
      }
      return finished;
    }

    private Set<Hypothesis> expand(Hypothesis hypothesis, String input) {
      Set<Hypothesis> next = new HashSet<>();
      int nextIndex = hypothesis.charIndex + 1;
      boolean inBounds = nextIndex < input.length();
      boolean atInputEnd = nextIndex == input.length() - 1;

      if (inBounds) {
        expandExactMatches(hypothesis, input.charAt(nextIndex), atInputEnd, next);
      } else if (hypothesis.node.word != null) {
        addHypothesis(hypothesis);
      }

      if (hypothesis.penalty >= maxPenalty) {
        return next;
      }

      Iterable<Node> allChildNodes = hypothesis.node.hasEpsilonConnection()
          ? hypothesis.node.getAllChildNodes()
          : hypothesis.node.getImmediateChildNodeIterable();

      if (inBounds) {
        expandSubstitutions(hypothesis, allChildNodes, input.charAt(nextIndex), atInputEnd, next);
      }

      if (hypothesis.penalty + DELETION_PENALTY > maxPenalty) {
        return next;
      }

      if (inBounds) {
        next.add(
            hypothesis.getNewMoveForward(hypothesis.node, DELETION_PENALTY, Operation.DELETION));
      }

      for (Node child : allChildNodes) {
        Hypothesis h = hypothesis.getNew(child, INSERTION_PENALTY, Operation.INSERTION);
        h.setWord(child);
        next.add(h);
      }

      if (inBounds) {
        expandTranspositions(hypothesis, input, nextIndex, next);
      }
      return next;
    }

    private void expandExactMatches(
        Hypothesis hypothesis, char nextChar, boolean atInputEnd, Set<Hypothesis> next) {
      for (Node child : hypothesis.node.getMatchingChildren(nextChar, matcher)) {
        Hypothesis h = hypothesis.getNewMoveForward(child, 0, Operation.NO_ERROR);
        h.setWord(child);
        next.add(h);
        if (atInputEnd && h.node.word != null) {
          addHypothesis(h);
        }
      }
    }

    private void expandSubstitutions(
        Hypothesis hypothesis,
        Iterable<Node> allChildNodes,
        char nextChar,
        boolean atInputEnd,
        Set<Hypothesis> next) {
      for (Node child : allChildNodes) {
        float penalty = substitutionPenalty(child.chr, nextChar);
        if (penalty <= 0 || hypothesis.penalty + penalty > maxPenalty) {
          continue;
        }
        Hypothesis h = hypothesis.getNewMoveForward(child, penalty, Operation.SUBSTITUTION);
        h.setWord(child);
        if (atInputEnd) {
          if (h.node.word != null) {
            addHypothesis(h);
          }
        } else {
          next.add(h);
        }
      }
    }

    private float substitutionPenalty(char candidateChar, char inputChar) {
      if (!checkNearKeySubstitution) {
        return SUBSTITUTION_PENALTY;
      }
      if (candidateChar == inputChar) {
        return 0;
      }
      String nearChars = nearKeyMap.get(candidateChar);
      return (nearChars != null && nearChars.indexOf(inputChar) >= 0)
          ? NEAR_KEY_SUBSTITUTION_PENALTY
          : SUBSTITUTION_PENALTY;
    }

    private void expandTranspositions(
        Hypothesis hypothesis, String input, int nextIndex, Set<Hypothesis> next) {
      if (input.length() <= 2 || nextIndex >= input.length() - 1) {
        return;
      }
      char currentChar = input.charAt(nextIndex);
      char transposedChar = input.charAt(nextIndex + 1);
      boolean atInputEnd = (nextIndex + 1 == input.length() - 1);

      for (Node first : hypothesis.node.getMatchingChildren(transposedChar, matcher)) {
        for (Node second : first.getMatchingChildren(currentChar, matcher)) {
          Hypothesis h = hypothesis.getNew(
              second, TRANSPOSITION_PENALTY, nextIndex + 1, Operation.TRANSPOSITION);
          if (first.getType() == Node.TYPE_WORD) {
            h.setWord(first);
          }
          h.setWord(second);
          if (atInputEnd) {
            if (h.node.word != null) {
              addHypothesis(h);
            }
            if (h.penalty < maxPenalty) {
              next.add(h);
            }
          } else {
            next.add(h);
          }
        }
      }
    }

    private void addHypothesis(Hypothesis hypothesis) {
      String hypWord = hypothesis.getContent();
      if (!finished.contains(hypWord)) {
        finished.set(hypWord, hypothesis.penalty);
      } else if (finished.get(hypWord) > hypothesis.penalty) {
        finished.set(hypWord, hypothesis.penalty);
      }
    }
  }
}
