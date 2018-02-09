package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

public class Conditions {

  public static Condition contains(RootAttribute attribute) {
    return new ContainsRootAttribute(attribute);
  }

  public static Condition contains(PhoneticAttribute attribute) {
    return new ContainsPhoneticAttribute(attribute);
  }

  public static Condition contains(DictionaryItem item) {
    return new ContainsDictionaryItem(item);
  }

  private static class ContainsRootAttribute implements Condition {

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

  private static class ContainsPhoneticAttribute implements Condition {

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

  private static class ContainsDictionaryItem implements Condition {

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

  public static class hasAnySuffixSurface implements Condition {

    @Override
    public boolean check(SearchPath visitor) {
      return !visitor.containsSuffixWithSurface();
    }

    @Override
    public String toString() {
      return "hasAnySuffixSurface{}";
    }
  }
}
