package zemberek.morphology.morphotactics;

import static java.util.Arrays.asList;
import static zemberek.morphology.morphotactics.Operator.AND;
import static zemberek.morphology.morphotactics.Operator.OR;

import java.util.Collection;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

class Conditions {

  public static Condition contains(RootAttribute attribute) {
    return new ContainsRootAttribute(attribute);
  }

  public static Condition contains(PhoneticAttribute attribute) {
    return new ContainsPhoneticAttribute(attribute);
  }

  public static Condition contains(DictionaryItem item) {
    return new ContainsDictionaryItem(item);
  }


  public static Condition notContains(RootAttribute attribute) {
    return new ContainsRootAttribute(attribute).not();
  }

  public static Condition notContains(PhoneticAttribute attribute) {
    return new ContainsPhoneticAttribute(attribute).not();
  }

  public static Condition notContains(DictionaryItem item) {
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
    public boolean check(SearchPath visitor) {
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
    public boolean check(SearchPath visitor) {
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
    public boolean check(SearchPath visitor) {
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
    public boolean check(SearchPath visitor) {
      return visitor.containsSuffixWithSurface();
    }

    @Override
    public String toString() {
      return "HasAnySuffixSurface{}";
    }
  }

  public static class LastMorphemeIs extends AbstractCondition {

    Morpheme morpheme;

    public LastMorphemeIs(Morpheme morpheme) {
      this.morpheme = morpheme;
    }

    @Override
    public boolean check(SearchPath visitor) {
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
    public boolean check(SearchPath visitor) {
      return !condition.check(visitor);
    }
  }

}
