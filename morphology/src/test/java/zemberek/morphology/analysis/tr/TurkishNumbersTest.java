package zemberek.morphology.analysis.tr;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class TurkishNumbersTest {

  @Test
  public void cardinalTest() {
    Assert.assertEquals("sıfır", TurkishNumbers.convertToString(0));
    Assert.assertEquals("bin", TurkishNumbers.convertToString(1000));
    Assert.assertEquals("bir", TurkishNumbers.convertToString(1));
    Assert.assertEquals("on bir", TurkishNumbers.convertToString(11));
    Assert.assertEquals("yüz on bir", TurkishNumbers.convertToString(111));
    Assert.assertEquals("yüz on bir bin", TurkishNumbers.convertToString(111000));
    Assert.assertEquals(
        "bir milyon iki yüz otuz dört bin beş yüz altmış yedi",
        TurkishNumbers.convertToString(1_234_567));
    Assert.assertEquals(
        "bir milyar iki yüz otuz dört milyon beş yüz altmış yedi bin sekiz yüz doksan",
        TurkishNumbers.convertToString(1_234_567_890));

  }

  @Test
  public void cardinalTest2() {
    Assert.assertEquals("sıfır", TurkishNumbers.convertNumberToString("0"));
    Assert.assertEquals("sıfır sıfır", TurkishNumbers.convertNumberToString("00"));
    Assert.assertEquals("sıfır sıfır sıfır", TurkishNumbers.convertNumberToString("000"));
    Assert.assertEquals("sıfır sıfır sıfır bir", TurkishNumbers.convertNumberToString("0001"));
    Assert.assertEquals("bin", TurkishNumbers.convertNumberToString("1000"));
    Assert.assertEquals("bir", TurkishNumbers.convertNumberToString("1"));
    Assert.assertEquals("on bir", TurkishNumbers.convertNumberToString("11"));
    Assert.assertEquals("yüz on bir", TurkishNumbers.convertNumberToString("111"));
    Assert.assertEquals("yüz on bir bin", TurkishNumbers.convertNumberToString("111000"));
    Assert.assertEquals("sıfır yüz on bir bin", TurkishNumbers.convertNumberToString("0111000"));
    Assert.assertEquals("sıfır sıfır yüz on bir bin",
        TurkishNumbers.convertNumberToString("00111000"));
  }

  @Test
  public void ordinalTest() {
    Assert.assertEquals("sıfırıncı",
        TurkishNumbers.convertOrdinalNumberString("0."));
  }

  @Test
  public void separateNumbersTest() {
    Assert.assertEquals(Lists.newArrayList("H", "12", "A", "5")
        , TurkishNumbers.separateNumbers("H12A5"));
    Assert.assertEquals(Lists.newArrayList("F", "16", "'ya")
        , TurkishNumbers.separateNumbers("F16'ya"));
  }

  @Test
  public void separateConnectedNumbersTest() {
    Assert.assertEquals(Lists.newArrayList("on")
        , TurkishNumbers.seperateConnectedNumbers("on"));
    Assert.assertEquals(Lists.newArrayList("on", "iki", "bin", "altı", "yüz")
        , TurkishNumbers.seperateConnectedNumbers("onikibinaltıyüz"));
    Assert.assertEquals(Lists.newArrayList("bir", "iki", "üç")
        , TurkishNumbers.seperateConnectedNumbers("birikiüç"));
  }

  @Test
  public void testTextToNumber1() {
    Assert.assertEquals(11, TurkishNumbers.convertToNumber("on bir"));
    Assert.assertEquals(111, TurkishNumbers.convertToNumber("yüz on bir"));
    Assert.assertEquals(101, TurkishNumbers.convertToNumber("yüz bir"));
    Assert.assertEquals(1000_000, TurkishNumbers.convertToNumber("bir milyon"));
    Assert.assertEquals(-1, TurkishNumbers.convertToNumber("bir bin"));
  }

  @Test
  public void romanNumberTest() {
    Assert.assertEquals(-1,
        TurkishNumbers.romanToDecimal("foo"));
    Assert.assertEquals(-1,
        TurkishNumbers.romanToDecimal("IIIIIII"));
    Assert.assertEquals(1987,
        TurkishNumbers.romanToDecimal("MCMLXXXVII"));
  }
}
