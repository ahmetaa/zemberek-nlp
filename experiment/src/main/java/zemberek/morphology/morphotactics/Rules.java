package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

public class Rules {

  public static Rule allowOnly(RootAttribute attribute) {
    return new AllowOnlyRootAttribute(attribute);
  }

  public static Rule allowOnly(PhoneticAttribute attribute) {
    return new AllowOnlyIfContainsPhoneticAttribute(attribute);
  }

  public static Rule allowOnly(DictionaryItem item) {
    return new AllowDictionaryItem(item);
  }

  public static Rule rejectIfContains(PhoneticAttribute attribute) {
    return new RejectIfContainsPhoneticAttribute(attribute);
  }

  public static Rule rejectIfContains(DictionaryItem item) {
    return new RejectDictionaryItem(item);
  }

  private static class AllowOnlyRootAttribute implements Rule {

    RootAttribute attribute;

    AllowOnlyRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return visitor.containsRootAttribute(attribute);
    }
  }

  private static class AllowOnlyIfContainsPhoneticAttribute implements Rule {

    PhoneticAttribute attribute;

    public AllowOnlyIfContainsPhoneticAttribute(PhoneticAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.getPhoneticAttributes().contains(attribute);
    }
  }

  private static class RejectIfContainsPhoneticAttribute implements Rule {

    PhoneticAttribute attribute;

    RejectIfContainsPhoneticAttribute(PhoneticAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return !visitor.getPhoneticAttributes().contains(attribute);
    }
  }

  private static class AllowDictionaryItem implements Rule {

    DictionaryItem item;

    AllowDictionaryItem(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return item != null && visitor.hasDictionaryItem(item);
    }
  }

  private static class RejectDictionaryItem implements Rule {

    DictionaryItem item;

    RejectDictionaryItem(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return item == null || !visitor.hasDictionaryItem(item);
    }
  }

  public static class RejectIfHasAnySuffixSurface implements Rule {

    @Override
    public boolean canPass(SearchPath visitor) {
      return !visitor.containsSuffixWithSurface();
    }
  }


}
