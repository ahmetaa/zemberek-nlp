package zemberek.morphology.morphotactics;

import java.util.Arrays;
import java.util.List;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.SearchPath;

public class Rules {

  public static Rule allowOnly(String key) {
    return new AllowOnly(key);
  }

  public static Rule allowOnly(RootAttribute attribute) {
    return new AllowOnlyRootAttribute(attribute);
  }

  public static Rule allowOnly(PhoneticExpectation expectation) {
    return new AllowOnlyIfContainsPhoneticExpectation(expectation);
  }

  public static Rule allowTailSequence(String... keys) {
    return new AllowOnlySequence(keys);
  }


  public static Rule allowAny(String... keys) {
    return new AllowAny(keys);
  }

  public static Rule mandatory(String key) {
    return new Mandatory(key);
  }

  public static Rule rejectOnly(String key) {
    return new RejectOnly(key);
  }

  public static Rule rejectAny(String... keys) {
    return new RejectAny(keys);
  }

  public static Rule rejectIfContains(RootAttribute attribute) {
    return new RejectIfContainsRootAttribute(attribute);
  }

  public static Rule rejectIfContains(PhoneticExpectation expectation) {
    return new RejectIfContainsPhoneticExpectation(expectation);
  }

  public static class AllowOnlySequence implements Rule {

    List<String> tailKeys;

    public AllowOnlySequence(String... keys) {
      this.tailKeys = Arrays.asList(keys);
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.containsTailSequence(tailKeys);
    }
  }

  public static class AllowOnly implements Rule {

    String key;

    public AllowOnly(String key) {
      this.key = key;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.containsKey(key);
    }
  }

  public static class Mandatory implements Rule {

    String key;

    public Mandatory(String key) {
      this.key = key;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.containsKey(key);
    }
  }

  public static class AllowAny implements Rule {

    List<String> keys;

    public AllowAny(String... keys) {
      this.keys = Arrays.asList(keys);
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      for (String key : keys) {
        if (visitor.containsKey(key)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class RejectOnly implements Rule {

    String key;

    RejectOnly(String key) {
      this.key = key;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return !visitor.containsKey(key);
    }
  }

  public static class RejectAny implements Rule {

    List<String> keys;

    RejectAny(String... keys) {
      this.keys = Arrays.asList(keys);
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      for (String key : keys) {
        if (visitor.containsKey(key)) {
          return false;
        }
      }
      return true;
    }
  }

  private static class AllowOnlyRootAttribute implements Rule {

    RootAttribute attribute;

    public AllowOnlyRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.containsRootAttribute(attribute);
    }
  }


  private static class AllowOnlyIfContainsPhoneticExpectation implements Rule {

    PhoneticExpectation expectation;

    public AllowOnlyIfContainsPhoneticExpectation(
        PhoneticExpectation expectation) {
      this.expectation = expectation;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return visitor.containsPhoneticExpectation(expectation);
    }
  }

  private static class RejectIfContainsRootAttribute implements Rule {

    RootAttribute attribute;

    public RejectIfContainsRootAttribute(
        RootAttribute attribute) {
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return !visitor.containsRootAttribute(attribute);
    }
  }

  private static class RejectIfContainsPhoneticExpectation implements Rule {

    PhoneticExpectation expectation;

    public RejectIfContainsPhoneticExpectation(
        PhoneticExpectation expectation) {
      this.expectation = expectation;
    }

    @Override
    public boolean canPass(SearchPath visitor) {
      return !visitor.containsPhoneticExpectation(expectation);
    }
  }

}
