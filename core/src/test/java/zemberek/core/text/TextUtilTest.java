package zemberek.core.text;

import org.junit.Assert;
import org.junit.Test;

public class TextUtilTest {

  @Test
  public void normalizeQuotesHyphensFastPathReturnsSameReference() {
    String clean = "Türkiye'de 2026 yılında normal bir cümle: \"merhaba\" - test.";
    Assert.assertSame(clean, TextUtil.normalizeQuotesHyphens(clean));
    Assert.assertSame("", TextUtil.normalizeQuotesHyphens(""));
  }

  @Test
  public void normalizeQuotesHyphensMatchesLegacyRegexBehavior() {
    String[] samples = {
        "",
        "Ankara'ya gittim.",
        "''Merhaba'' dedi.",
        "'''üçlü'''",
        "“Alıntı” «Fransız» \u2033 \u0093 \u0094",
        "Ankara’da ‘tek’ `ters` ´akut´ \u2032 \u0091 \u0092",
        "’’çift eğri’’",
        "2020–2026 \u0096 \u0097",
        "“Ali’nin ''kitabı'' – yeni”"
    };
    for (String s : samples) {
      String expected = s
          .replaceAll("[\u201C\u201D\u00BB\u00AB\u2033\u0093\u0094]|''", "\"")
          .replaceAll("[\u0091\u0092\u2032´`’‘]", "'")
          .replaceAll("[\u0096\u0097–]", "-");
      Assert.assertEquals("Mismatch for: " + s, expected, TextUtil.normalizeQuotesHyphens(s));
    }

    // Exhaustive single-character equivalence over BMP subset covering all mapped code points.
    for (int cp = 0; cp <= 0x20FF; cp++) {
      String s = String.valueOf((char) cp);
      String expected = s
          .replaceAll("[\u201C\u201D\u00BB\u00AB\u2033\u0093\u0094]|''", "\"")
          .replaceAll("[\u0091\u0092\u2032´`’‘]", "'")
          .replaceAll("[\u0096\u0097–]", "-");
      Assert.assertEquals("Mismatch for U+" + Integer.toHexString(cp),
          expected, TextUtil.normalizeQuotesHyphens(s));
    }
  }

  @Test
  public void normalizeApostrophesFastPathAndMappings() {
    String clean = "Ankara'ya gittim.";
    Assert.assertSame(clean, TextUtil.normalizeApostrophes(clean));
    Assert.assertSame("", TextUtil.normalizeApostrophes(""));

    Assert.assertEquals(
        "Ankara'da 'tek' 'ters' 'akut' ' ' '",
        TextUtil.normalizeApostrophes("Ankara’da ‘tek’ `ters` ´akut´ \u2032 \u0091 \u0092"));

    for (int cp = 0; cp <= 0x20FF; cp++) {
      String s = String.valueOf((char) cp);
      String expected = s.replaceAll("[\u0091\u0092\u2032´`’‘]", "'");
      Assert.assertEquals("Mismatch for U+" + Integer.toHexString(cp),
          expected, TextUtil.normalizeApostrophes(s));
    }
  }
}
