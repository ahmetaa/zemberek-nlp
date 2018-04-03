package zemberek.morphology._analyzer;

import static zemberek.morphology._analyzer.AnalyzerTestBase.getAnalyzer;

import org.junit.Assert;
import org.junit.Test;

public class AnalysisFormatterTest {

  @Test
  public void defaultSurfaceFormatterTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    Assert.assertEquals("[kitap:Noun] kitap:Noun+lar:A3pl+da:Loc",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));

    analysis = analyzer.analyze("kitapsız").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3sg|sız:Without→Adj",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));

    analysis = analyzer.analyze("kitaplardaymış").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+lar:A3pl+da:Loc|Zero→Verb+ymış:Narr+A3sg",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okut").get(0);
    Assert.assertEquals("[okumak:Verb] oku:Verb|t:Caus→Verb+Imp+A2sg",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));
  }

  @Test
  public void defaultLexicalFormatterTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3pl+Loc",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));

    analysis = analyzer.analyze("kitapsız").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3sg|Without→Adj",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));

    analysis = analyzer.analyze("kitaplardaymış").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3pl+Loc|Zero→Verb+Narr+A3sg",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okut").get(0);
    Assert.assertEquals("[okumak:Verb] oku:Verb|Caus→Verb+Imp+A2sg",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));
  }

  @Test
  public void oflazerStyleFormatterTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    Assert.assertEquals("kitap+Noun+A3pl+Loc",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));

    analysis = analyzer.analyze("kitapsız").get(0);
    Assert.assertEquals("kitap+Noun+A3sg^DB+Adj+Without",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));

    analysis = analyzer.analyze("kitaplardaymış").get(0);
    Assert.assertEquals("kitap+Noun+A3pl+Loc^DB+Verb+Zero+Narr+A3sg",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okut").get(0);
    Assert.assertEquals("oku+Verb^DB+Verb+Caus+Imp+A2sg",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));
  }


}
