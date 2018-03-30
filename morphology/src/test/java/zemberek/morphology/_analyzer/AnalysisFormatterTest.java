package zemberek.morphology._analyzer;

import static zemberek.morphology._analyzer.AnalyzerTestBase.getAnalyzer;

import org.junit.Assert;
import org.junit.Test;

public class AnalysisFormatterTest {

  @Test
  public void defaultSurfaceFormatterTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    Assert.assertEquals("[kitap:Noun] kitap:Noun+lar:A3pl+Pnon+da:Loc",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));

    analysis = analyzer.analyze("kitapsız").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3sg+Pnon+Nom|sız:Without→Adj",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));

    analysis = analyzer.analyze("kitaplardaymış").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+lar:A3pl+Pnon+da:Loc|Zero→Verb+ymış:Narr+A3sg",
        AnalysisFormatters.DEFAULT_SURFACE.format(analysis));
  }

  @Test
  public void defaultLexicalFormatterTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3pl+Pnon+Loc",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));

    analysis = analyzer.analyze("kitapsız").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3sg+Pnon+Nom|Without→Adj",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));

    analysis = analyzer.analyze("kitaplardaymış").get(0);
    Assert.assertEquals("[kitap:Noun] kitap:Noun+A3pl+Pnon+Loc|Zero→Verb+Narr+A3sg",
        AnalysisFormatters.DEFAULT_LEXICAL.format(analysis));
  }

  @Test
  public void defaultOflazerFormatterTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    Assert.assertEquals("kitap+Noun+A3pl+Pnon+Loc",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));

    analysis = analyzer.analyze("kitapsız").get(0);
    Assert.assertEquals("kitap+Noun+A3sg+Pnon+Nom^DB+Adj+Without",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));

    analysis = analyzer.analyze("kitaplardaymış").get(0);
    Assert.assertEquals("kitap+Noun+A3pl+Pnon+Loc^DB+Verb+Zero+Narr+A3sg",
        AnalysisFormatters.OFLAZER_STYLE.format(analysis));
  }


}
