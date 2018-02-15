package zemberek.morphology.analyzer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.StemTransition;

/**
 * This class represents a path in morphotactics graph. During analysis many SearchPaths are created
 * and surviving paths are used for generating analysis results.
 */
public class SearchPath {

  // letters that have been parsed.
  String head;

  // letters to parse.
  String tail;

  // carries the initial transition. Normally this is not necessary bur here we have it for
  // a small optimization.
  StemTransition stemTransition;

  MorphemeState currentState;

  List<MorphemeSurfaceForm> suffixes;

  EnumSet<PhoneticAttribute> phoneticAttributes;

  private boolean terminal = false;
  private boolean containsDerivation = false;
  private boolean containsSuffixWithSurface = false;

  public static SearchPath initialPath(StemTransition stemTransition, String head, String tail) {
    return new SearchPath(
        head,
        tail,
        stemTransition,
        stemTransition.to,
        new ArrayList<>(3),
        stemTransition.getPhoneticAttributes().clone(),
        stemTransition.to.terminal);
  }

  private SearchPath(
      String head,
      String tail,
      StemTransition stemTransition,
      MorphemeState currentState,
      List<MorphemeSurfaceForm> suffixes,
      EnumSet<PhoneticAttribute> phoneticAttributes,
      boolean terminal) {
    this.head = head;
    this.tail = tail;
    this.stemTransition = stemTransition;
    this.currentState = currentState;
    this.suffixes = suffixes;
    this.phoneticAttributes = phoneticAttributes;
    this.terminal = terminal;
  }

  SearchPath getCopy(MorphemeSurfaceForm surfaceNode,
      EnumSet<PhoneticAttribute> phoneticAttributes) {
    boolean t = surfaceNode.lexicalTransition.to.terminal;
    ArrayList<MorphemeSurfaceForm> hist = new ArrayList<>(suffixes);
    hist.add(surfaceNode);
    String newHead = head + surfaceNode.surface;
    String newTail = tail.substring(surfaceNode.surface.length());
    SearchPath path = new SearchPath(
        newHead,
        newTail,
        stemTransition,
        surfaceNode.lexicalTransition.to,
        hist,
        phoneticAttributes,
        t);
    path.containsSuffixWithSurface = containsSuffixWithSurface || !surfaceNode.surface.isEmpty();
    path.containsDerivation = containsDerivation || surfaceNode.lexicalTransition.to.derivative;
    return path;
  }

  public String toString() {
    String rootStr = stemTransition.item.id + ":" + stemTransition.surface;
    String morphemeStr =
        String.join(" + ", suffixes.stream()
            .map(MorphemeSurfaceForm::toString)
            .collect(Collectors.toList()));
    return "[(" + rootStr + ")(" + head + "-" + tail + ") " + morphemeStr + "]";
  }

  public String getHead() {
    return head;
  }

  public String getTail() {
    return tail;
  }

  public StemTransition getStemTransition() {
    return stemTransition;
  }

  public MorphemeState getCurrentState() {
    return currentState;
  }

  public EnumSet<PhoneticAttribute> getPhoneticAttributes() {
    return phoneticAttributes;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public List<MorphemeSurfaceForm> getSuffixes() {
    return suffixes;
  }

  public boolean containsDerivation() {
    return containsDerivation;
  }

  public boolean containsSuffixWithSurface() {
    return containsSuffixWithSurface;
  }

  public boolean containsRootAttribute(RootAttribute attribute) {
    return stemTransition.item.attributes.contains(attribute);
  }

  public boolean hasDictionaryItem(DictionaryItem item) {
    // TODO: for performance, probably it is safe to check references only.
    return item.equals(stemTransition.item);
  }

}
