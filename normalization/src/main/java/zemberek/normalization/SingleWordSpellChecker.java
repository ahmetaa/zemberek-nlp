package zemberek.normalization;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.turkish.Turkish;

public class SingleWordSpellChecker {

  public static final Map<Character, String> TURKISH_FQ_NEAR_KEY_MAP =
      CharacterGraphDecoder.TURKISH_FQ_NEAR_KEY_MAP;
  public static final Map<Character, String> TURKISH_Q_NEAR_KEY_MAP =
      CharacterGraphDecoder.TURKISH_Q_NEAR_KEY_MAP;

  public final float maxPenalty;
  public final boolean checkNearKeySubstitution;
  public Map<Character, String> nearKeyMap;
  private final CharacterGraphDecoder decoder;

  public SingleWordSpellChecker() {
    this(1);
  }

  public SingleWordSpellChecker(float maxPenalty) {
    this.decoder = new CharacterGraphDecoder(maxPenalty);
    this.maxPenalty = decoder.maxPenalty;
    this.checkNearKeySubstitution = decoder.checkNearKeySubstitution;
    this.nearKeyMap = decoder.nearKeyMap;
  }

  public SingleWordSpellChecker(float maxPenalty, Map<Character, String> nearKeyMap) {
    this.decoder = new CharacterGraphDecoder(maxPenalty, nearKeyMap);
    this.maxPenalty = decoder.maxPenalty;
    this.checkNearKeySubstitution = decoder.checkNearKeySubstitution;
    this.nearKeyMap = decoder.nearKeyMap;
  }

  public String process(String str) {
    return CharacterGraphDecoder.removeIgnoredChars(str.toLowerCase(Turkish.LOCALE));
  }

  public void addWord(String word) {
    decoder.addWord(process(word), word);
  }

  public void addWords(String... words) {
    for (String word : words) {
      addWord(word);
    }
  }

  public void buildDictionary(List<String> vocabulary) {
    for (String word : vocabulary) {
      addWord(word);
    }
  }

  public FloatValueMap<String> decode(String input) {
    return decoder.decode(input);
  }

  /**
   * Returns suggestions sorted by penalty.
   */
  public List<ScoredString> getSuggestionsWithScores(String input) {
    return decoder.getSuggestionsWithScores(input).stream()
        .map(item -> new ScoredString(item.item, item.score))
        .toList();
  }

  public List<String> getSuggestions(String input) {
    return decoder.getSuggestions(input);
  }

  public List<String> getSuggestionsSorted(String input) {
    return decoder.getSuggestionsSorted(input);
  }

  public static class ScoredString implements Comparable<ScoredString> {

    public final String s;
    public final float penalty;

    public ScoredString(String s, float penalty) {
      this.s = s;
      this.penalty = penalty;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ScoredString other)) {
        return false;
      }
      return Float.compare(other.penalty, penalty) == 0 && Objects.equals(s, other.s);
    }

    @Override
    public int hashCode() {
      return Objects.hash(s, penalty);
    }

    @Override
    public int compareTo(ScoredString o) {
      return Float.compare(penalty, o.penalty);
    }
  }
}
