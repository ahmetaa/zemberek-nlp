package zemberek.core.turkish;

import org.junit.Assert;
import org.junit.Test;

public class _TurkishAlphabetTest {

  @Test
  public void isVowelTest() {
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
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
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
    String[] entries = {"a", "aa", "", "bb", "bebaba"};
    int[] expCounts = {1, 2, 0, 0, 3};
    int i = 0;
    for (String entry : entries) {
      Assert.assertEquals(expCounts[i++], alphabet.vowelCount(entry));
    }
  }

  @Test
  public void voiceTest() {
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
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
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
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
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
    String iStr = "abcâîûÂÎÛ fg12";
    String oStr = "abcaiuAİU fg12";
    Assert.assertEquals(oStr, alphabet.normalizeCircumflex(iStr));
  }

}
