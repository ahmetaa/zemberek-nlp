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
  public void toAsciiTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String iStr = "abcçğıiİIoöüşâîûÂÎÛz";
    String oStr = "abccgiiIIoousaiuAIUz";
    Assert.assertEquals(oStr, alphabet.toAscii(iStr));
  }

  @Test
  public void equalsIgnoreDiacritics() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] a = {"siraci", "ağac", "ağaç"};
    String[] b = {"şıracı", "ağaç", "agac"};
    for (int i = 0; i < a.length; i++) {
      Assert.assertTrue(alphabet.equalsIgnoreDiacritics(a[i], b[i]));
    }
  }

  @Test
  public void vowelHarmonyA() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] a = {"elma", "kedi", "turp"};
    String[] b= {"lar", "cik", "un"};
    for (int i = 0; i < a.length; i++) {
      Assert.assertTrue(alphabet.checkVowelHarmonyA(a[i], b[i]));
    }
  }

  @Test
  public void vowelHarmonyA2() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] a = {"elma", "kedi", "turp"};
    String[] b = {"ler", "cık", "in"};
    for (int i = 0; i < a.length; i++) {
      Assert.assertFalse(alphabet.checkVowelHarmonyA(a[i], b[i]));
    }
  }

  @Test
  public void vowelHarmonyI1() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] a = {"elma", "kedi", "turp"};
    String[] b = {"yı", "yi", "u"};
    for (int i = 0; i < a.length; i++) {
      Assert.assertTrue(alphabet.checkVowelHarmonyI(a[i], b[i]));
    }
  }

  @Test
  public void vowelHarmonyI2() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] a = {"elma", "kedi", "turp"};
    String[] b = {"yu", "yü", "ı"};
    for (int i = 0; i < a.length; i++) {
      Assert.assertFalse(alphabet.checkVowelHarmonyI(a[i], b[i]));
    }
  }

  @Test
  public void startsWithDiacriticsIgnoredTest() {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    String[] a = {"siraci", "çağlayan"};
    String[] b = {"şıracı", "cag"};
    for (int i = 0; i < a.length; i++) {
      Assert.assertTrue(alphabet.startsWithIgnoreDiacritics(a[i], b[i]));
    }
  }

}
