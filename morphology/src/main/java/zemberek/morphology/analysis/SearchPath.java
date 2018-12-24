package zemberek.morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.lexicon.DictionaryItem;

/**
 * This class represents a path in morphotactics graph. During analysis many SearchPaths are created
 * and surviving paths are used for generating analysis results.
 */
public class SearchPath {

  // letters to parse.
  String tail;

  MorphemeState currentState;

  List<SurfaceTransition> transitions;

  AttributeSet<PhoneticAttribute> phoneticAttributes;

  private boolean terminal;
  private boolean containsDerivation = false;
  private boolean containsSuffixWithSurface = false;

  public static SearchPath initialPath(StemTransition stemTransition, String tail) {
    List<SurfaceTransition> morphemes = new ArrayList<>(4);
    SurfaceTransition root = new SurfaceTransition(stemTransition.surface, stemTransition);
    morphemes.add(root);
    return new SearchPath(
        tail,
        stemTransition.to,
        morphemes,
        stemTransition.getPhoneticAttributes().copy(),
        stemTransition.to.terminal);
  }

  private SearchPath(
      String tail,
      MorphemeState currentState,
      List<SurfaceTransition> transitions,
      AttributeSet<PhoneticAttribute> phoneticAttributes,
      boolean terminal) {
    this.tail = tail;
    this.currentState = currentState;
    this.transitions = transitions;
    this.phoneticAttributes = phoneticAttributes;
    this.terminal = terminal;
  }

  SearchPath getCopy(
      SurfaceTransition surfaceNode,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {

    boolean isTerminal = surfaceNode.getState().terminal;
    ArrayList<SurfaceTransition> hist = new ArrayList<>(transitions);
    hist.add(surfaceNode);
    String newTail = tail.substring(surfaceNode.surface.length());
    SearchPath path = new SearchPath(
        newTail,
        surfaceNode.getState(),
        hist,
        phoneticAttributes,
        isTerminal);
    path.containsSuffixWithSurface = containsSuffixWithSurface || !surfaceNode.surface.isEmpty();
    path.containsDerivation = containsDerivation || surfaceNode.getState().derivative;
    return path;
  }

  public SearchPath getCopyForGeneration(
      SurfaceTransition surfaceNode,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {

    boolean isTerminal = surfaceNode.getState().terminal;
    ArrayList<SurfaceTransition> hist = new ArrayList<>(transitions);
    hist.add(surfaceNode);
    SearchPath path = new SearchPath(
        tail,
        surfaceNode.getState(),
        hist,
        phoneticAttributes,
        isTerminal);
    path.containsSuffixWithSurface = containsSuffixWithSurface || !surfaceNode.surface.isEmpty();
    path.containsDerivation = containsDerivation || surfaceNode.getState().derivative;
    return path;
  }

  public String toString() {
    StemTransition st = getStemTransition();
    String morphemeStr =
        transitions.stream()
            .map(SurfaceTransition::toString)
            .collect(Collectors.joining(" + "));
    return "[(" + st.item.id + ")(-" + tail + ") " + morphemeStr + "]";
  }

  public String getTail() {
    return tail;
  }

  public StemTransition getStemTransition() {
    return (StemTransition) transitions.get(0).lexicalTransition;
  }

  public MorphemeState getCurrentState() {
    return currentState;
  }

  public MorphemeState getPreviousState() {
    if (transitions.size() < 2) {
      return null;
    }
    return transitions.get(transitions.size() - 2).getState();
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
    return transitions;
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
    return transitions.get(transitions.size() - 1);
  }

  public DictionaryItem getDictionaryItem() {
    return getStemTransition().item;
  }

}
