package zemberek.core.turkish;

import org.junit.Assert;
import org.junit.Test;

public class TurkishAlphabetTest {

  @Test
  public void isVowelTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String vowels = "aeiuüıoöâîû";
    for (char c : vowels.toCharArray()) {
      Assert.assertTrue(alphabet.isVowel(c));
    }
    String nonvowels = "bcçdfgğjklmnprştvxwzq.";
    for (char c : nonvowels.toCharArray()) {
      Assert.assertFalse(alphabet.isVowel(c));
    }
  }

  @Test
  public void vowelCountTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] entries = {"a", "aa", "", "bb", "bebaba"};
    int[] expCounts = {1, 2, 0, 0, 3};
    int i = 0;
    for (String entry : entries) {
      Assert.assertEquals(expCounts[i++], alphabet.vowelCount(entry));
    }
  }

  @Test
  public void voiceTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String iStr = "çÇgGkKpPtTaAbB";
    String oStr = "cCğĞğĞbBdDaAbB";
    for (int i = 0; i < iStr.length(); i++) {
      char in = iStr.charAt(i);
      char outExpected = oStr.charAt(i);
      Assert.assertEquals("",
          String.valueOf(outExpected),
          String.valueOf(alphabet.voice(in)));
    }
  }

  @Test
  public void devoiceTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String iStr = "bBcCdDgGğĞaAkK";
    String oStr = "pPçÇtTkKkKaAkK";
    for (int i = 0; i < iStr.length(); i++) {
      char in = iStr.charAt(i);
      char outExpected = oStr.charAt(i);
      Assert.assertEquals("",
          String.valueOf(outExpected),
          String.valueOf(alphabet.devoice(in)));
    }
  }

  @Test
  public void circumflexTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String iStr = "abcâîûÂÎÛ fg12";
    String oStr = "abcaiuAİU fg12";
    Assert.assertEquals(oStr, alphabet.normalizeCircumflex(iStr));
  }

  @Test
  public void ascciifyTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String iStr = "abcçğıiİIoöüşâîûÂÎÛz";
    String oStr = "abccgiiIIoousaiuAIUz";
    Assert.assertEquals(oStr, alphabet.toAscii(iStr));
  }

}
