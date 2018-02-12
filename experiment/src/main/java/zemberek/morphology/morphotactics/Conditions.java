package zemberek.morphology.morphotactics;

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



  private static class ContainsRootAttribute extends AbstractCondition {

    RootAttribute attribute;

    ContainsRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean check(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
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
      // normally this should also check if visitor has no derivation.
      return item != null && visitor.hasDictionaryItem(item);
    }

    @Override
    public String toString() {
      return "ContainsDictionaryItem{" + item + '}';
    }
  }

  public static class hasAnySuffixSurface extends AbstractCondition {

    @Override
    public boolean check(SearchPath visitor) {
      return visitor.containsSuffixWithSurface();
    }

    @Override
    public String toString() {
      return "hasAnySuffixSurface{}";
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
