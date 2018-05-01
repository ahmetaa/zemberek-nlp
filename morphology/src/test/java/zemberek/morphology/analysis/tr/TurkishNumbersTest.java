package zemberek.morphology.analysis.tr;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class TurkishNumbersTest {

  @Test
  public void cardinalTest() {
    Assert.assertEquals("sıfır", TurkishNumbers.convertToString(0));
    Assert.assertEquals("bin", TurkishNumbers.convertToString(1000));
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

}
