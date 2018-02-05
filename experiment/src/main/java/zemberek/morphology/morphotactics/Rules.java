package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.MorphemeSurfaceForm;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

public class Rules {

  public static Rule allowOnly(RootAttribute attribute) {
    return new AllowOnlyRootAttribute(attribute);
  }

  public static Rule allowOnly(PhoneticExpectation expectation) {
    return new AllowOnlyIfContainsPhoneticExpectation(expectation);
  }

  public static Rule allowOnly(DictionaryItem item) {
    return new AllowDictionaryItem(item);
  }

  public static Rule rejectIfContains(PhoneticExpectation expectation) {
    return new RejectIfContainsPhoneticExpectation(expectation);
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
      return visitor.containsRootAttribute(attribute);
    }
  }

  private static class AllowOnlyIfContainsPhoneticExpectation implements Rule {

    PhoneticExpectation expectation;

    AllowOnlyIfContainsPhoneticExpectation(PhoneticExpectation expectation) {
      this.expectation = expectation;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.containsPhoneticExpectation(expectation);
    }
  }

  private static class RejectIfContainsPhoneticExpectation implements Rule {

    PhoneticExpectation expectation;

    RejectIfContainsPhoneticExpectation(PhoneticExpectation expectation) {
      this.expectation = expectation;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return !visitor.containsPhoneticExpectation(expectation);
    }
  }

  private static class AllowDictionaryItem implements Rule {

    DictionaryItem item;

    AllowDictionaryItem(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
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
      return item == null || !visitor.hasDictionaryItem(item);
    }
  }

  public static class RejectIfHasAnySuffixSurface implements Rule {

    @Override
    public boolean canPass(SearchPath visitor) {
      for (MorphemeSurfaceForm form : visitor.getHistory()) {
        if (!form.surface.isEmpty()) {
          return false;
        }
      }
      return true;
    }
  }


}
