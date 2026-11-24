package zemberek.morphology.analysis.tr;

import org.junit.Assert;
import org.junit.Test;

public class PronunciationGuesserTest {

  private final PronunciationGuesser guesser = new PronunciationGuesser();

  @Test
  public void guessesAlphanumericAndLongDigitSequencesWithoutOverflow() {
    Assert.assertEquals("seüç", guesser.toTurkishLetterPronunciations("s3"));
    Assert.assertEquals("üçge", guesser.toTurkishLetterPronunciations("3g"));
    Assert.assertEquals("feonaltı", guesser.toTurkishLetterPronunciations("f-16"));

    // >18-digit alphanumeric ID must not throw NumberFormatException
    String longId = guesser.toTurkishLetterPronunciations("id123456789012345678901234");
    Assert.assertFalse(longId.isEmpty());
    Assert.assertTrue(longId.endsWith("dört"));
  }

  @Test
  public void guessesAbbreviationAndEnglishChars() {
    Assert.assertEquals("abede", guesser.guessForAbbreviation("abd"));
    Assert.assertEquals("tebeka", guesser.toTurkishLetterPronunciations("tbk"));
    Assert.assertEquals("netfliks", guesser.replaceEnglishSpecificChars("netflix"));
    Assert.assertEquals("vka", guesser.replaceEnglishSpecificChars("w-q'a"));
  }
}
