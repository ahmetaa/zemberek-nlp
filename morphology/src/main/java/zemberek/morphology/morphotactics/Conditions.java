package zemberek.morphology.morphotactics;

import static java.util.Arrays.asList;
import static zemberek.morphology.morphotactics.Operator.AND;
import static zemberek.morphology.morphotactics.Operator.OR;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.SurfaceTransition;
import zemberek.morphology.analysis.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

class Conditions {

  public static final Condition HAS_TAIL = new HasTail();
  public static final Condition HAS_NO_TAIL = new HasNoTail();
  static final Condition HAS_SURFACE = new HasAnySuffixSurface();
  static final Condition HAS_NO_SURFACE = new HasAnySuffixSurface().not();
  static final Condition CURRENT_GROUP_EMPTY = new NoSurfaceAfterDerivation();
  static final Condition CURRENT_GROUP_NOT_EMPTY = new NoSurfaceAfterDerivation().not();
  static final Condition HAS_DERIVATION = new HasDerivation();
  static final Condition HAS_NO_DERIVATION = not(new HasDerivation());


  static Condition has(RootAttribute attribute) {
    return new HasRootAttribute(attribute);
  }

  static Condition has(PhoneticAttribute attribute) {
    return new HasPhoneticAttribute(attribute);
  }

  static Condition rootIs(DictionaryItem item) {
    return new DictionaryItemIs(item);
  }

  static Condition rootPrimaryPos(PrimaryPos pos) {
    return new RootPrimaryPosIs(pos);
  }

  static Condition rootIsAny(DictionaryItem... items) {
    return new DictionaryItemIsAny(items);
  }

  static Condition rootIsNone(DictionaryItem... items) {
    return new DictionaryItemIsNone(items);
  }

  static Condition notHave(RootAttribute attribute) {
    return new HasRootAttribute(attribute).not();
  }

  public static Condition notHaveAny(RootAttribute... attributes) {
    return new HasAnyRootAttribute(attributes).not();
  }

  static Condition notHave(PhoneticAttribute attribute) {
    return new HasPhoneticAttribute(attribute).not();
  }

  static Condition rootIsNot(DictionaryItem item) {
    return new DictionaryItemIs(item).not();
  }

  public static Condition currentMorphemeIs(Morpheme morpheme) {
    return new CurrentMorphemeIs(morpheme);
  }

  public static Condition currentMorphemeIsAny(Morpheme... morphemes) {
    return new CurrentMorphemeIsAny(morphemes);
  }

  public static Condition lastMorphemeIsNot(Morpheme morpheme) {
    return new CurrentMorphemeIs(morpheme).not();
  }

  static Condition currentStateIs(MorphemeState state) {
    return new CurrentStateIs(state);
  }

  static Condition currentStateIsNot(MorphemeState state) {
    return new CurrentStateIsNot(state);
  }

  static Condition previousStateIs(MorphemeState state) {
    return new PreviousStateIs(state);
  }

  static Condition previousStateIsNot(MorphemeState state) {
    return new PreviousStateIsNot(state);
  }

  static Condition prviousMorphemeIs(Morpheme morpheme) {
    return new PreviousMorphemeIs(morpheme);
  }

  static Condition previousMorphemeIsNot(Morpheme morpheme) {
    return new PreviousMorphemeIs(morpheme).not();
  }

  public static Condition and(Condition left, Condition right) {
    return condition(AND, left, right);
  }

  public static Condition and(Collection<? extends Condition> conditions) {
    return condition(AND, conditions);
  }

  public static Condition and(Condition... conditions) {
    return condition(AND, conditions);
  }


  public static Condition condition(Operator operator, Condition left, Condition right) {
    return CombinedCondition.of(operator, left, right);
  }

  public static Condition condition(Operator operator, Condition... conditions) {
    return condition(operator, asList(conditions));
  }

  public static Condition condition(Operator operator, Collection<? extends Condition> conditions) {
    return CombinedCondition.of(operator, conditions);
  }

  public static Condition or(Condition left, Condition right) {
    return condition(OR, left, right);
  }

  public static Condition or(Condition... conditions) {
    return condition(OR, conditions);
  }

  public static Condition or(Collection<? extends Condition> conditions) {
    return condition(OR, conditions);
  }

  public static Condition not(Condition condition) {
    return condition.not();
  }

  private static class HasRootAttribute extends AbstractCondition {

    RootAttribute attribute;

    HasRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getDictionaryItem().hasAttribute(attribute);
    }

    @Override
    public String toString() {
      return "HasRootAttribute{" + attribute + '}';
    }
  }

  private static class HasAnyRootAttribute extends AbstractCondition {

    RootAttribute[] attributes;

    HasAnyRootAttribute(RootAttribute... attributes) {
      this.attributes = attributes.clone();
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getDictionaryItem().hasAnyAttribute(attributes);
    }

    @Override
    public String toString() {
      return "HasAnyRootAttribute{" + Arrays.toString(attributes) + '}';
    }
  }

  private static class HasPhoneticAttribute extends AbstractCondition {

    PhoneticAttribute attribute;

    HasPhoneticAttribute(PhoneticAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getPhoneticAttributes().contains(attribute);
    }

    @Override
    public String toString() {
      return "HasPhoneticAttribute{" + attribute + '}';
    }
  }

  private static class DictionaryItemIs extends AbstractCondition {

    DictionaryItem item;

    DictionaryItemIs(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return item != null && visitor.hasDictionaryItem(item);
    }

    @Override
    public String toString() {
      return "DictionaryItemIs{" + item + '}';
    }
  }

  private static class RootPrimaryPosIs extends AbstractCondition {

    PrimaryPos pos;

    RootPrimaryPosIs(PrimaryPos pos) {
      this.pos = pos;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getDictionaryItem().primaryPos == pos;
    }

    @Override
    public String toString() {
      return "RootPrimaryPosIs{" + pos + '}';
    }
  }

  public static class SecondaryPosIs extends AbstractCondition {

    SecondaryPos pos;

    SecondaryPosIs(SecondaryPos pos) {
      this.pos = pos;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getDictionaryItem().secondaryPos == pos;
    }

    @Override
    public String toString() {
      return "SecondaryPosIs{" + pos + '}';
    }
  }


  private static class DictionaryItemIsAny extends AbstractCondition {

    Set<DictionaryItem> items;

    DictionaryItemIsAny(DictionaryItem... items) {
      this.items = new HashSet<>(Arrays.asList(items));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return items.contains(visitor.getDictionaryItem());
    }

    @Override
    public String toString() {
      return "DictionaryItemIsAny{" + items + '}';
    }
  }

  private static class DictionaryItemIsNone extends AbstractCondition {

    Set<DictionaryItem> items;

    DictionaryItemIsNone(DictionaryItem... items) {
      this.items = new HashSet<>(Arrays.asList(items));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return !items.contains(visitor.getDictionaryItem());
    }

    @Override
    public String toString() {
      return "DictionaryItemIsNone{" + items + '}';
    }
  }


  public static class HasAnySuffixSurface extends AbstractCondition {

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.containsSuffixWithSurface();
    }

    @Override
    public String toString() {
      return "HasAnySuffixSurface{}";
    }
  }

  // accepts if visitor has letters to consume.
  public static class HasTail extends AbstractCondition {

    @Override
    public boolean accept(SearchPath visitor) {
      return !visitor.getTail().isEmpty();
    }

    @Override
    public String toString() {
      return "HasTail{}";
    }
  }

  public static class HasNoTail extends AbstractCondition {

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getTail().isEmpty();
    }

    @Override
    public String toString() {
      return "HasNoTail{}";
    }
  }

  public static class HasTailSequence extends AbstractCondition {

    Morpheme[] morphemes;

    HasTailSequence(Morpheme... morphemes) {
      this.morphemes = morphemes;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> forms = visitor.getTransitions();
      if (forms.size() < morphemes.length) {
        return false;
      }
      int i = 0;
      int j = forms.size() - morphemes.length;
      while (i < morphemes.length) {
        if (morphemes[i++] != forms.get(j++).getMorpheme()) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "HasTailSequence{" + Arrays.toString(morphemes) + "}";
    }
  }

  public static class ContainsMorphemeSequence extends AbstractCondition {

    Morpheme[] morphemes;

    ContainsMorphemeSequence(Morpheme... morphemes) {
      this.morphemes = morphemes;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> forms = visitor.getTransitions();
      if (forms.size() < morphemes.length) {
        return false;
      }
      int m = 0;
      for (SurfaceTransition form : forms) {
        if (form.getMorpheme().equals(morphemes[m])) {
          m++;
          if (m == morphemes.length) {
            return true;
          }
        } else {
          m = 0;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "ContainsMorphemeSequence{" + Arrays.toString(morphemes) + "}";
    }
  }


  public static class CurrentMorphemeIs extends AbstractCondition {

    Morpheme morpheme;

    CurrentMorphemeIs(Morpheme morpheme) {
      this.morpheme = morpheme;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getCurrentState().morpheme.equals(morpheme);
    }

    @Override
    public String toString() {
      return "CurrentMorphemeIs{ " + morpheme + " }";
    }
  }

  public static class PreviousMorphemeIs extends AbstractCondition {

    Morpheme morpheme;

    PreviousMorphemeIs(Morpheme morpheme) {
      this.morpheme = morpheme;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      MorphemeState previousState = visitor.getPreviousState();
      return previousState != null && previousState.morpheme.equals(this.morpheme);
    }

    @Override
    public String toString() {
      return "PreviousMorphemeIs{ " + morpheme + " }";
    }
  }


  public static class PreviousStateIs extends AbstractCondition {

    MorphemeState state;

    PreviousStateIs(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      MorphemeState previousState = visitor.getPreviousState();
      return previousState != null && previousState.equals(this.state);
    }

    @Override
    public String toString() {
      return "PreviousStateIs{ " + state + " }";
    }
  }

  public static class PreviousStateIsNot extends AbstractCondition {

    MorphemeState state;

    PreviousStateIsNot(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      MorphemeState previousState = visitor.getPreviousState();
      return previousState == null || !previousState.equals(this.state);
    }

    @Override
    public String toString() {
      return "PreviousStateIsNot{ " + state + " }";
    }
  }


  public static class RootSurfaceIs extends AbstractCondition {

    String surface;

    RootSurfaceIs(String surface) {
      this.surface = surface;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getStemTransition().surface.equals(this.surface);
    }

    @Override
    public String toString() {
      return "RootSurfaceIs{ " + surface + " }";
    }
  }

  public static class RootSurfaceIsAny extends AbstractCondition {

    String[] surfaces;

    RootSurfaceIsAny(String... surfaces) {
      this.surfaces = surfaces;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      for (String s : surfaces) {
        if (visitor.getStemTransition().surface.equals(s)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "RootSurfaceIsAny{ " + Arrays.toString(surfaces) + " }";
    }
  }

  public static class CurrentStateIs extends AbstractCondition {

    MorphemeState state;

    CurrentStateIs(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getCurrentState().equals(state);
    }

    @Override
    public String toString() {
      return "CurrentStateIs{ " + state + " }";
    }
  }

  public static class CurrentStateIsNot extends AbstractCondition {

    MorphemeState state;

    CurrentStateIsNot(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return !visitor.getCurrentState().equals(state);
    }

    @Override
    public String toString() {
      return "CurrentStateIsNot{ " + state + " }";
    }
  }

  public static class NotCondition extends AbstractCondition {

    Condition condition;

    NotCondition(Condition condition) {
      this.condition = condition;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return !condition.accept(visitor);
    }

    @Override
    public String toString() {
      return "Not(" + condition + ")";
    }
  }

  static Condition lastDerivationIs(MorphemeState state) {
    return new LastDerivationIs(state);
  }

  public static class LastDerivationIs extends AbstractCondition {

    MorphemeState state;

    LastDerivationIs(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        SurfaceTransition sf = suffixes.get(i);
        if (sf.getState().derivative) {
          return sf.getState() == state;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "LastDerivationIs{" + state + '}';
    }
  }

  public static class HasDerivation extends AbstractCondition {

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();
      for (SurfaceTransition suffix : suffixes) {
        if (suffix.getState().derivative) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "HasDerivation";
    }
  }

  public static class LastDerivationIsAny extends AbstractCondition {

    Set<MorphemeState> states;

    LastDerivationIsAny(MorphemeState... states) {
      this.states = new HashSet<>(states.length);
      this.states.addAll(Arrays.asList(states));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        SurfaceTransition sf = suffixes.get(i);
        if (sf.getState().derivative) {
          return states.contains(sf.getState());
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "LastDerivationIsAny{" + states + '}';
    }
  }


  // Checks if any of the "MorphemeState" in "states" exist in current Inflectional Group.
  // If previous group starts after a derivation, derivation MorphemeState is also checked.
  public static class CurrentGroupContainsAny extends AbstractCondition {

    Set<MorphemeState> states;

    CurrentGroupContainsAny(MorphemeState... states) {
      this.states = new HashSet<>(states.length);
      this.states.addAll(Arrays.asList(states));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        SurfaceTransition sf = suffixes.get(i);
        if (states.contains(sf.getState())) {
          return true;
        }
        if (sf.getState().derivative) {
          return false;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "CurrentGroupContainsAny{" + states + '}';
    }
  }

  // Checks if any of the "MorphemeState" in "states" exist in previous Inflectional Group.
  // If previous group starts after a derivation, derivation MorphemeState is also checked.
  // TODO: this may have a bug. Add test
  public static class PreviousGroupContains extends AbstractCondition {

    Set<MorphemeState> states;

    PreviousGroupContains(MorphemeState... states) {
      this.states = new HashSet<>(states.length);
      this.states.addAll(Arrays.asList(states));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();

      int lastIndex = suffixes.size() - 1;
      SurfaceTransition sf = suffixes.get(lastIndex);
      // go back until a transition that is connected to a derivative morpheme.
      while (!sf.getState().derivative) {
        if (lastIndex == 0) { // there is no previous group. return early.
          return false;
        }
        lastIndex--;
        sf = suffixes.get(lastIndex);
      }

      for (int i = lastIndex - 1; i > 0; i--) {
        sf = suffixes.get(i);
        if (states.contains(sf.getState())) {
          return true;
        }
        if (sf.getState().derivative) { //could not found the morpheme in this group.
          return false;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "PreviousGroupContains{" + states + '}';
    }
  }

  // Checks if any of the "Morpheme" in "morphemes" exist in previous Inflectional Group.
  // If previous group starts after a derivation, derivation Morpheme is also checked.
  public static class PreviousGroupContainsMorpheme extends AbstractCondition {

    Set<Morpheme> morphemes;

    PreviousGroupContainsMorpheme(Morpheme... morphemes) {
      this.morphemes = new HashSet<>(morphemes.length);
      this.morphemes.addAll(Arrays.asList(morphemes));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();

      int lastIndex = suffixes.size() - 1;
      SurfaceTransition sf = suffixes.get(lastIndex);
      // go back until a transition that is connected to a derivative morpheme.
      while (!sf.getState().derivative) {
        if (lastIndex == 0) { // there is no previous group. return early.
          return false;
        }
        lastIndex--;
        sf = suffixes.get(lastIndex);
      }

      for (int i = lastIndex - 1; i > 0; i--) {
        sf = suffixes.get(i);
        if (morphemes.contains(sf.getState().morpheme)) {
          return true;
        }
        if (sf.getState().derivative) { //could not found the morpheme in this group.
          return false;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "PreviousGroupContainsMorpheme{" + morphemes + '}';
    }
  }

  // No letters are consumed after derivation occurred.This does not include the transition
  // that caused derivation.
  public static class NoSurfaceAfterDerivation extends AbstractCondition {

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        SurfaceTransition sf = suffixes.get(i);
        if (sf.getState().derivative) {
          return true;
        }
        if (!sf.surface.isEmpty()) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "NoSurfaceAfterDerivation{}";
    }
  }

  public static class ContainsMorpheme extends AbstractCondition {

    Set<Morpheme> morphemes;

    ContainsMorpheme(Morpheme... morphemes) {
      this.morphemes = new HashSet<>(morphemes.length);
      this.morphemes.addAll(Arrays.asList(morphemes));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<SurfaceTransition> suffixes = visitor.getTransitions();
      for (SurfaceTransition suffix : suffixes) {
        if (morphemes.contains(suffix.getState().morpheme)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "ContainsMorpheme{" + morphemes + '}';
    }
  }

  public static class PreviousMorphemeIsAny extends AbstractCondition {

    Set<Morpheme> morphemes;

    PreviousMorphemeIsAny(Morpheme... morphemes) {
      this.morphemes = new HashSet<>(morphemes.length);
      this.morphemes.addAll(Arrays.asList(morphemes));
    }

    @Override
    public boolean accept(SearchPath path) {
      MorphemeState previousState = path.getPreviousState();
      return previousState != null && morphemes.contains(previousState.morpheme);
    }

    @Override
    public String toString() {
      return "PreviousMorphemeIsAny{" + morphemes + '}';
    }

  }

  public static class CurrentMorphemeIsAny extends AbstractCondition {

    Set<Morpheme> morphemes;

    CurrentMorphemeIsAny(Morpheme... morphemes) {
      this.morphemes = new HashSet<>(morphemes.length);
      this.morphemes.addAll(Arrays.asList(morphemes));
    }

    @Override
    public boolean accept(SearchPath path) {
      MorphemeState previousState = path.getCurrentState();
      return previousState != null && morphemes.contains(previousState.morpheme);
    }

    @Override
    public String toString() {
      return "CurrentMorphemeIsAny{" + morphemes + '}';
    }

  }

  public static class PreviousStateIsAny extends AbstractCondition {

    Set<MorphemeState> states;

    PreviousStateIsAny(MorphemeState... states) {
      this.states = new HashSet<>(states.length);
      this.states.addAll(Arrays.asList(states));
    }

    @Override
    public boolean accept(SearchPath path) {
      MorphemeState previousState = path.getPreviousState();
      return previousState != null && states.contains(previousState);
    }
  }


}
