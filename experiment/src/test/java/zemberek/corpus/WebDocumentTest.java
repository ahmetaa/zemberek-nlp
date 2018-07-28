package zemberek.corpus;

import com.google.common.base.Splitter;
import org.junit.Assert;
import org.junit.Test;

public class WebDocumentTest {

  @Test
  public void testTitleWithDoubleQuotes() {
    String meta = "<doc id=\"http://www.cnnturk.com/spor/futbol/fernandao-yeni-yildan-sampiyonluk-bekliyor\" source=\"www.cnnturk.com\" title=\"Fernandao \"yeni yıldan şampiyonluk\" bekliyor\" labels=\"Jose Fernandao,Fenerbahçe,transfer\" category=\"\" crawl-date=\"2017-01-03\">";
    String content = "Fernandao yeni yıldan şampiyonluk bekliyor\n"
        + "30.12.2016 Cuma 17:04 (Güncellendi: 30.12.2016 Cuma 17:09)\n"
        + "Fernandao yeni yıldan şampiyonluk bekliyor\n"
        + "</doc>";
    WebDocument d = WebDocument.fromText(meta, Splitter.on("\n").splitToList(content));
    Assert.assertEquals("Fernandao \"yeni yıldan şampiyonluk\" bekliyor", d.getTitle());
  }

}
