package zemberek.morphology.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;

/**
 * This class represents a path in morphotactics graph. During analysis many SearchPaths are created
 * and surviving paths are used for generating analysis results.
 */
public class SearchPath {

  // Parent node in the persistent path chain (null for root stem node).
  final SearchPath previous;

  // Input string and current unread tail offset.
  final String word;
  final int tailIndex;

  final MorphemeState currentState;
  final String surface;
  final MorphemeTransition lexicalTransition;

  final AttributeSet<PhoneticAttribute> phoneticAttributes;

  // Cached root transition to avoid walking back to root in condition checks.
  private final StemTransition stemTransition;
  private final int length;
  private final boolean terminal;
  private final boolean containsDerivation;
  private final boolean containsSuffixWithSurface;

  public static SearchPath initialPath(StemTransition stemTransition, String tail) {
    return initialPath(stemTransition, tail, 0);
  }

  public static SearchPath initialPath(StemTransition stemTransition, String word, int tailIndex) {
    return new SearchPath(
        null,
        stemTransition,
        word,
        tailIndex,
        stemTransition.to,
        stemTransition.surface,
        stemTransition,
        stemTransition.getPhoneticAttributes(),
        1,
        stemTransition.to.terminal,
        stemTransition.to.derivative,
        false);
  }

  private SearchPath(
      SearchPath previous,
      StemTransition stemTransition,
      String word,
      int tailIndex,
      MorphemeState currentState,
      String surface,
      MorphemeTransition lexicalTransition,
      AttributeSet<PhoneticAttribute> phoneticAttributes,
      int length,
      boolean terminal,
      boolean containsDerivation,
      boolean containsSuffixWithSurface) {
    this.previous = previous;
    this.stemTransition = stemTransition;
    this.word = word;
    this.tailIndex = tailIndex;
    this.currentState = currentState;
    this.surface = surface;
    this.lexicalTransition = lexicalTransition;
    this.phoneticAttributes = phoneticAttributes;
    this.length = length;
    this.terminal = terminal;
    this.containsDerivation = containsDerivation;
    this.containsSuffixWithSurface = containsSuffixWithSurface;
  }

  SearchPath advance(
      String surface,
      SuffixTransition transition,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {
    MorphemeState nextState = transition.to;
    int surfLen = surface.length();
    return new SearchPath(
        this,
        stemTransition,
        word,
        tailIndex + surfLen,
        nextState,
        surface,
        transition,
        phoneticAttributes,
        length + 1,
        nextState.terminal,
        containsDerivation || nextState.derivative,
        containsSuffixWithSurface || surfLen > 0);
  }

  SearchPath advanceEmpty(
      SuffixTransition transition,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {
    MorphemeState nextState = transition.to;
    return new SearchPath(
        this,
        stemTransition,
        word,
        tailIndex,
        nextState,
        "",
        transition,
        phoneticAttributes,
        length + 1,
        nextState.terminal,
        containsDerivation || nextState.derivative,
        containsSuffixWithSurface);
  }

  SearchPath getCopy(
      SurfaceTransition surfaceNode,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {
    return advance(
        surfaceNode.surface,
        (SuffixTransition) surfaceNode.lexicalTransition,
        phoneticAttributes);
  }

  public SearchPath getCopyForGeneration(
      SurfaceTransition surfaceNode,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {
    MorphemeState nextState = surfaceNode.getState();
    return new SearchPath(
        this,
        stemTransition,
        word,
        tailIndex,
        nextState,
        surfaceNode.surface,
        surfaceNode.lexicalTransition,
        phoneticAttributes,
        length + 1,
        nextState.terminal,
        containsDerivation || nextState.derivative,
        containsSuffixWithSurface || !surfaceNode.surface.isEmpty());
  }

  public String toString() {
    StemTransition st = getStemTransition();
    String morphemeStr =
        getTransitions().stream()
            .map(SurfaceTransition::toString)
            .collect(Collectors.joining(" + "));
    return "[(" + st.item.id + ")(-" + getTail() + ") " + morphemeStr + "]";
  }

  public SearchPath getPrevious() {
    return previous;
  }

  public int getLength() {
    return length;
  }

  public String getSurface() {
    return surface;
  }

  public int getTailLength() {
    return word.length() - tailIndex;
  }

  public boolean hasEmptyTail() {
    return tailIndex == word.length();
  }

  public String getTail() {
    return tailIndex == 0 ? word : word.substring(tailIndex);
  }

  public StemTransition getStemTransition() {
    return stemTransition;
  }

  public MorphemeState getCurrentState() {
    return currentState;
  }

  public MorphemeState getPreviousState() {
    return previous == null ? null : previous.currentState;
  }

  public AttributeSet<PhoneticAttribute> getPhoneticAttributes() {
    return phoneticAttributes;
  }

  public boolean containsPhoneticAttribute(PhoneticAttribute attribute) {
    return phoneticAttributes.contains(attribute);
  }

  public boolean isTerminal() {
    return terminal;
  }

  public List<SurfaceTransition> getTransitions() {
    SurfaceTransition[] arr = new SurfaceTransition[length];
    int idx = length - 1;
    for (SearchPath p = this; p != null; p = p.previous) {
      arr[idx--] = new SurfaceTransition(p.surface, p.lexicalTransition);
    }
    return new ArrayList<>(Arrays.asList(arr));
  }

  public boolean containsDerivation() {
    return containsDerivation;
  }

  public boolean containsSuffixWithSurface() {
    return containsSuffixWithSurface;
  }

  public boolean hasDictionaryItem(DictionaryItem item) {
    // TODO: for performance, probably it is safe to check references only.
    return item.equals(getStemTransition().item);
  }

  public SurfaceTransition getLastTransition() {
    return new SurfaceTransition(surface, lexicalTransition);
  }

  public DictionaryItem getDictionaryItem() {
    return getStemTransition().item;
  }

}
