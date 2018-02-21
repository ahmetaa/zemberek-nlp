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
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.MorphemeSurfaceForm;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

class Conditions {

  public static final Condition HAS_TAIL = new HasTail();
  public static final Condition HAS_NO_TAIL = new HasNoTail();
  static final Condition HAS_NO_SURFACE = new HasAnySuffixSurface().not();
  static final Condition CURRENT_GROUP_EMPTY = new CurrentInflectionalGroupEmpty();
  static final Condition CURRENT_GROUP_NOT_EMPTY = new CurrentInflectionalGroupEmpty().not();


  public static Condition contains(RootAttribute attribute) {
    return new ContainsRootAttribute(attribute);
  }

  public static Condition contains(PhoneticAttribute attribute) {
    return new ContainsPhoneticAttribute(attribute);
  }

  public static Condition contains(DictionaryItem item) {
    return new ContainsDictionaryItem(item);
  }


  public static Condition notContain(RootAttribute attribute) {
    return new ContainsRootAttribute(attribute).not();
  }

  public static Condition notContain(PhoneticAttribute attribute) {
    return new ContainsPhoneticAttribute(attribute).not();
  }

  public static Condition notContain(DictionaryItem item) {
    return new ContainsDictionaryItem(item).not();
  }

  public static Condition lastMorphemeIs(Morpheme morpheme) {
    return new LastMorphemeIs(morpheme);
  }

  public static Condition lastMorphemeIsNot(Morpheme morpheme) {
    return new LastMorphemeIs(morpheme).not();
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

  private static class ContainsRootAttribute extends AbstractCondition {

    RootAttribute attribute;

    ContainsRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      // TODO: maybe this should also check if visitor has no derivation.
      return visitor.containsRootAttribute(attribute);
    }

    @Override
    public String toString() {
      return "ContainsRootAttribute{" + attribute + '}';
    }
  }

  private static class ContainsPhoneticAttribute extends AbstractCondition {

    PhoneticAttribute attribute;

    public ContainsPhoneticAttribute(PhoneticAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getPhoneticAttributes().contains(attribute);
    }

    @Override
    public String toString() {
      return "ContainsPhoneticAttribute{" + attribute + '}';
    }
  }

  private static class ContainsDictionaryItem extends AbstractCondition {

    DictionaryItem item;

    ContainsDictionaryItem(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      // TODO: maybe this should also check if visitor has no derivation.
      return item != null && visitor.hasDictionaryItem(item);
    }

    @Override
    public String toString() {
      return "ContainsDictionaryItem{" + item + '}';
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


  public static class ContainsTailSequence extends AbstractCondition {

    Morpheme[] morphemes;

    public ContainsTailSequence(Morpheme... morphemes) {
      this.morphemes = morphemes;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<MorphemeSurfaceForm> forms = visitor.getMorphemes();
      if (forms.size() < morphemes.length) {
        return false;
      }
      int i = 0;
      int j = forms.size() - morphemes.length;
      while (i < morphemes.length) {
        if (morphemes[i++] != forms.get(j++).morphemeState.morpheme) {
          return false;
        }
      }
      return true;
    }


    @Override
    public String toString() {
      return "ContainsTailSequence{}";
    }
  }

  public static class LastMorphemeIs extends AbstractCondition {

    Morpheme morpheme;

    public LastMorphemeIs(Morpheme morpheme) {
      this.morpheme = morpheme;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return visitor.getCurrentState().morpheme.equals(morpheme);
    }

    @Override
    public String toString() {
      return "LastMorphemeIs{}";
    }
  }

  public static class NotCondition extends AbstractCondition {

    Condition condition;

    public NotCondition(Condition condition) {
      this.condition = condition;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      return !condition.accept(visitor);
    }
  }

  public static class LastDerivationIs extends AbstractCondition {

    MorphemeState state;

    public LastDerivationIs(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<MorphemeSurfaceForm> suffixes = visitor.getMorphemes();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        MorphemeSurfaceForm sf = suffixes.get(i);
        if (sf.morphemeState.derivative) {
          return sf.morphemeState == state;
        }
      }
      return false;
    }
  }

  public static class PreviousNonEmptyMorphemeIs extends AbstractCondition {

    MorphemeState state;

    public PreviousNonEmptyMorphemeIs(MorphemeState state) {
      this.state = state;
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<MorphemeSurfaceForm> suffixes = visitor.getMorphemes();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        MorphemeSurfaceForm sf = suffixes.get(i);
        if (sf.surface.isEmpty()) {
          continue;
        }
        return sf.morphemeState == state;
      }
      return false;
    }
  }

  public static class ContainsAnyAfterDerivation extends AbstractCondition {

    Set<MorphemeState> states;

    public ContainsAnyAfterDerivation(MorphemeState... states) {
      this.states = new HashSet<>(states.length);
      this.states.addAll(Arrays.asList(states));
    }

    @Override
    public boolean accept(SearchPath visitor) {
      List<MorphemeSurfaceForm> suffixes = visitor.getMorphemes();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        MorphemeSurfaceForm sf = suffixes.get(i);
        if (sf.morphemeState.derivative || sf.morphemeState.posRoot) {
          return false;
        }
        if(states.contains(sf.morphemeState)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class CurrentInflectionalGroupEmpty extends AbstractCondition {

    @Override
    public boolean accept(SearchPath visitor) {
      List<MorphemeSurfaceForm> suffixes = visitor.getMorphemes();
      for (int i = suffixes.size() - 1; i > 0; i--) {
        MorphemeSurfaceForm sf = suffixes.get(i);
        if (sf.morphemeState.derivative || sf.morphemeState.posRoot) {
          return true;
        }
        if(!sf.surface.isEmpty()) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "CurrentInflectionalGroupEmpty{}";
    }
  }

}
