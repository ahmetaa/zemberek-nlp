package zemberek.morphology.analysis;

import static zemberek.morphology.analysis.WordAnalysisSurfaceFormatter.CaseType.*;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;

public class WordAnalysisSurfaceFormatterTest {

  @Test
  public void formatNonProperNoun() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon("elma", "kitap", "demek", "evet")
        .build();

    String[] inputs = {"elmamadaki", "elma", "kitalarımdan", "kitabımızsa", "diyebileceğimiz",
        "dedi", "evet"};

    WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();

    for (String input : inputs) {
      WordAnalysis results = morphology.analyze(input);
      for (SingleAnalysis result : results) {
        Assert.assertEquals(input, formatter.format(result, null));
      }
    }
  }

  @Test
  public void formatKnownProperNouns() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon("Ankara", "Iphone [Pr:ayfon, A:LocaleEn]", "Google [Pr:gugıl]")
        .build();

    String[] inputs = {"ankarada", "ıphonumun", "googledan", "Iphone", "Google", "Googlesa"};
    String[] expected = {"Ankara'da", "Iphone'umun", "Google'dan", "Iphone", "Google", "Google'sa"};

    check(morphology, inputs, expected,"'");
  }

  private void check(TurkishMorphology morphology, String[] inputs, String[] expected, String apostrophe) {
    WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();

    int i = 0;
    for (String input : inputs) {
      WordAnalysis results = morphology.analyze(input);
      for (SingleAnalysis result : results) {
        if (result.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun) {
          String format = formatter.format(result, apostrophe);
          Assert.assertEquals(expected[i], format);
        }
      }
      i++;
    }
  }

  @Test
  public void formatKnownProperNounsNoQuote() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon("Blah [A:NoQuote]").build();

    String[] inputs = {"blaha", "Blahta"};
    String[] expected = {"Blaha", "Blahta"};

    check(morphology, inputs, expected,null);
  }

  @Test
  public void formatVerbs() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon("olmak").build();

    String[] inputs = {"olarak", "Olarak"};
    String[] expected = {"olarak", "Olarak"};

    check(morphology, inputs, expected,null);
    // giving apostrophe should not effect the output.
    check(morphology, inputs, expected,"'");
  }

  @Test
  public void formatNumerals() {
    TurkishMorphology morphology = TurkishMorphology.builder().disableCache().build();
    String[] inputs = {"1e", "4ten", "123ü", "12,5ten"};
    String[] expected = {"1'e", "4'ten", "123'ü", "12,5ten"};

    WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();

    int i = 0;
    for (String input : inputs) {
      WordAnalysis results = morphology.analyze(input);
      for (SingleAnalysis result : results) {
        if (result.getDictionaryItem().primaryPos == PrimaryPos.Numeral) {
          Assert.assertEquals(expected[i], formatter.format(result, "'"));
        }
      }
      i++;
    }
  }


  @Test
  public void formatToCase() {
    TurkishMorphology morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon("kış", "şiir", "Aydın", "Google [Pr:gugıl]")
        .build();

    String[] inputs =
        {"aydında", "googledan", "Google", "şiirde", "kışçığa", "kış"};

    String[] expectedDefaultCase =
        {"Aydın'da", "Google'dan", "Google", "şiirde", "kışçığa", "kış"};
    String[] expectedLowerCase =
        {"aydın'da", "google'dan", "google", "şiirde", "kışçığa", "kış"};
    String[] expectedUpperCase =
        {"AYDIN'DA", "GOOGLE'DAN", "GOOGLE", "ŞİİRDE", "KIŞÇIĞA", "KIŞ"};
    String[] expectedCapitalCase =
        {"Aydın'da", "Google'dan", "Google", "Şiirde", "Kışçığa", "Kış"};
    String[] expectedUpperRootLowerEndingCase =
        {"AYDIN'da", "GOOGLE'dan", "GOOGLE", "ŞİİRde", "KIŞçığa", "KIŞ"};

    testCaseType(morphology, inputs, expectedDefaultCase,
        DEFAULT_CASE);
    testCaseType(morphology, inputs, expectedLowerCase,
        LOWER_CASE);
    testCaseType(morphology, inputs, expectedUpperCase,
        UPPER_CASE);
    testCaseType(morphology, inputs, expectedCapitalCase,
        TITLE_CASE);
    testCaseType(morphology, inputs, expectedUpperRootLowerEndingCase,
        UPPER_CASE_ROOT_LOWER_CASE_ENDING);
  }

  private void testCaseType(
      TurkishMorphology morphology,
      String[] inputs,
      String[] expected,
      WordAnalysisSurfaceFormatter.CaseType caseType) {

    WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();

    int i = 0;
    for (String input : inputs) {
      WordAnalysis results = morphology.analyze(input);
      for (SingleAnalysis result : results) {
        Assert.assertEquals(expected[i], formatter.formatToCase(result, caseType ));
      }
      i++;
    }
  }


  @Test
  public void guessCaseTest() {

    String[] inputs = {"abc", "Abc", "ABC", "Abc'de", "ABC'DE", "ABC.", "ABC'de", "a", "12", "A",
        "A1"};
    WordAnalysisSurfaceFormatter.CaseType[] expected = {
        LOWER_CASE,
        TITLE_CASE,
        UPPER_CASE,
        TITLE_CASE,
        UPPER_CASE,
        UPPER_CASE,
        UPPER_CASE_ROOT_LOWER_CASE_ENDING,
        LOWER_CASE,
        DEFAULT_CASE,
        UPPER_CASE,
        UPPER_CASE,
    };

    WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();

    int i = 0;
    for (String input : inputs) {
      Assert.assertEquals(expected[i], formatter.guessCase(input));
      i++;
    }
  }

}
