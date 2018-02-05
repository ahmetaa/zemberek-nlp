package zemberek.morphology.analyzer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.StemTransition;

/**
 * This class represents a path in morphotactics graph. During analysis many SearchPaths
 * are created and surviving paths are used for generating analysis results.
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

  List<MorphemeSurfaceForm> history = new ArrayList<>();

  EnumSet<PhoneticAttribute> phoneticAttributes;
  EnumSet<PhoneticExpectation> phoneticExpectations;

  boolean terminal = false;

  public SearchPath(StemTransition stemTransition, String head, String tail) {
    this.stemTransition = stemTransition;
    this.terminal = stemTransition.to.terminal;
    this.currentState = stemTransition.to;
    this.phoneticAttributes = stemTransition.getPhoneticAttributes().clone();
    this.phoneticExpectations = stemTransition.getPhoneticExpectations().clone();
    this.head = head;
    this.tail = tail;
  }

  public SearchPath(String head, String tail,
      StemTransition stemTransition, MorphemeState currentState,
      List<MorphemeSurfaceForm> history,
      EnumSet<PhoneticAttribute> phoneticAttributes,
      EnumSet<PhoneticExpectation> phoneticExpectations, boolean terminal) {
    this.head = head;
    this.tail = tail;
    this.stemTransition = stemTransition;
    this.currentState = currentState;
    this.history = history;
    this.phoneticAttributes = phoneticAttributes;
    this.phoneticExpectations = phoneticExpectations;
    this.terminal = terminal;
  }

  SearchPath getCopy(MorphemeSurfaceForm surfaceNode,
      EnumSet<PhoneticAttribute> phoneticAttributes,
      EnumSet<PhoneticExpectation> phoneticExpectations
  ) {
    boolean t = surfaceNode.lexicalTransition.to.terminal;
    ArrayList<MorphemeSurfaceForm> hist = new ArrayList<>(history);
    hist.add(surfaceNode);
    String newHead = head + surfaceNode.surface;
    String newTail = tail.substring(surfaceNode.surface.length());
    return new SearchPath(newHead, newTail, stemTransition, surfaceNode.lexicalTransition.to,
        hist, phoneticAttributes, phoneticExpectations, t);
  }

  public boolean containsKey(String key) {
    return false;
  }

  public boolean containsRootAttribute(RootAttribute attribute) {
    return stemTransition.item.attributes.contains(attribute);
  }

  public boolean containsPhoneticExpectation(PhoneticExpectation expectation) {
    return phoneticExpectations.contains(expectation);
  }

  public boolean containsTailSequence(List<String> keys) {
    return false;
  }
}
