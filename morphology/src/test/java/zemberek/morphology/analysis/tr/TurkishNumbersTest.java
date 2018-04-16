package zemberek.morphology.analysis.tr;

import org.junit.Assert;
import org.junit.Test;

public class TurkishNumbersTest {

  @Test
  public void cardinalTest() {
    Assert.assertEquals("sıfır",TurkishNumbers.convertToString(0));
    Assert.assertEquals("bin",TurkishNumbers.convertToString(1000));
  }

  @Test
  public void ordinalTest() {
    Assert.assertEquals("sıfırıncı",TurkishNumbers.convertOrdinalNumberString("0."));
  }


}
