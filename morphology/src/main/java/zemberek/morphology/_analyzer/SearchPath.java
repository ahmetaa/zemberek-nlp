package zemberek.morphology._analyzer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.enums.BitmapEnum;
import zemberek.core.enums.EnumBitSet;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology._morphotactics.MorphemeState;
import zemberek.morphology._morphotactics.StemTransition;

/**
 * This class represents a path in morphotactics graph. During analysis many SearchPaths are created
 * and surviving paths are used for generating analysis results.
 */
public class SearchPath {

  // letters that have been parsed.
  String head;

  // letters to parse.
  String tail;

  MorphemeState currentState;

  List<MorphemeSurfaceForm> morphemes;

  EnumSet<PhoneticAttribute> phoneticAttributes;
  EnumBitSet phoneticAttributesNew;

  private boolean terminal = false;
  private boolean containsDerivation = false;
  private boolean containsSuffixWithSurface = false;

  public static SearchPath initialPath(StemTransition stemTransition, String head, String tail) {
    List<MorphemeSurfaceForm> morphemes = new ArrayList<>(4);
    MorphemeSurfaceForm root = new MorphemeSurfaceForm(stemTransition.surface, stemTransition);
    morphemes.add(root);
    return new SearchPath(
        head,
        tail,
        stemTransition.to,
        morphemes,
        stemTransition.getPhoneticAttributes().clone(),
        stemTransition.to.terminal);
  }

  private SearchPath(
      String head,
      String tail,
      MorphemeState currentState,
      List<MorphemeSurfaceForm> morphemes,
      EnumSet<PhoneticAttribute> phoneticAttributes,
      boolean terminal) {
    this.head = head;
    this.tail = tail;
    this.currentState = currentState;
    this.morphemes = morphemes;
    this.phoneticAttributes = phoneticAttributes;
    this.phoneticAttributesNew = EnumBitSet.fromSet(phoneticAttributes);
    this.terminal = terminal;
  }

  SearchPath getCopy(
      MorphemeSurfaceForm surfaceNode,
      EnumSet<PhoneticAttribute> phoneticAttributes) {
    boolean t = surfaceNode.morphemeState.terminal;
    ArrayList<MorphemeSurfaceForm> hist = new ArrayList<>(morphemes);
    hist.add(surfaceNode);
    String newHead = head + surfaceNode.surface;
    String newTail = tail.substring(surfaceNode.surface.length());
    SearchPath path = new SearchPath(
        newHead,
        newTail,
        surfaceNode.morphemeState,
        hist,
        phoneticAttributes,
        t);
    path.containsSuffixWithSurface = containsSuffixWithSurface || !surfaceNode.surface.isEmpty();
    path.containsDerivation = containsDerivation || surfaceNode.morphemeState.derivative;
    return path;
  }

  public String toString() {
    StemTransition st = getStemTransition();
    String morphemeStr =
        String.join(" + ", morphemes.stream()
            .map(MorphemeSurfaceForm::toString)
            .collect(Collectors.toList()));
    return "[(" + st.item.id + ")(" + head + "-" + tail + ") " + morphemeStr + "]";
  }

  public String getHead() {
    return head;
  }

  public String getTail() {
    return tail;
  }

  public StemTransition getStemTransition() {
    return (StemTransition) morphemes.get(0).lexicalTransition;
  }

  public MorphemeState getCurrentState() {
    return currentState;
  }

  public MorphemeState getPreviousState() {
    if (morphemes.size() < 2) {
      return null;
    }
    return morphemes.get(morphemes.size() - 2).morphemeState;
  }

  public EnumSet<PhoneticAttribute> getPhoneticAttributes() {
    return phoneticAttributes;
  }

  public EnumBitSet getPhoneticAttributesNew() {
    return phoneticAttributesNew;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public List<MorphemeSurfaceForm> getMorphemes() {
    return morphemes;
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

  public DictionaryItem getDictionaryItem() {
    return getStemTransition().item;
  }

}
