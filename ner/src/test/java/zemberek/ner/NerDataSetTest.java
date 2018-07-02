package zemberek.ner;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;
import zemberek.ner.NerDataSet.AnnotationStyle;

public class NerDataSetTest {

  @Test
  public void testOpenNlpStyle() throws IOException {
    Path p = TestUtil.tempFileWithData(
        "<Start:ABC> Foo Bar <End> ivir zivir <Start:DEF> haha <End> . ");
    NerDataSet set = NerDataSet.load(p, AnnotationStyle.OPEN_NLP);
    System.out.println("types= " + set.types);
    Assert.assertTrue(TestUtil.containsAll(set.types, "ABC", "DEF", "OUT"));
  }

  @Test
  public void testBracketStyle() throws IOException {
    Path p = TestUtil.tempFileWithData(
        "[ABC Foo Bar] ivir zivir [DEF haha] . ");
    NerDataSet set = NerDataSet.load(p, AnnotationStyle.BRACKET);
    System.out.println("types= " + set.types);
    Assert.assertTrue(TestUtil.containsAll(set.types, "ABC", "DEF", "OUT"));
  }


}
