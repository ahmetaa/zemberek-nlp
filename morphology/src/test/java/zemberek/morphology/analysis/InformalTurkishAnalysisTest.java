package zemberek.morphology.analysis;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class InformalTurkishAnalysisTest extends AnalyzerTestBase {

  public static TurkishMorphotactics getSpokenMorphotactics(String... dictionaryLines) {
    RootLexicon lexicon = TurkishDictionaryLoader.load(dictionaryLines);
    return new InformalTurkishMorphotactics(lexicon);
  }

  static AnalysisTester getTester(String... dictionaryLines) {
    return new AnalysisTester(
        RuleBasedAnalyzer.forDebug(getSpokenMorphotactics(dictionaryLines)));
  }

  static AnalysisTester getTesterAscii(String... dictionaryLines) {
    return new AnalysisTester(RuleBasedAnalyzer
        .forDebug(getSpokenMorphotactics(dictionaryLines), true));
  }

  @Test
  public void testProgressiveDeformation() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıyom", matchesTailLex("Verb + Prog1_Informal + A1sg"));
    t.expectSingle("yazıyon", matchesTailLex("Verb + Prog1_Informal + A2sg"));
    t.expectSingle("yazıyosun", matchesTailLex("Verb + Prog1_Informal + A2sg"));
    t.expectSingle("yazıyo", matchesTailLex("Verb + Prog1_Informal + A3sg"));
    t.expectSingle("yazıyoz", matchesTailLex("Verb + Prog1_Informal + A1pl"));
    t.expectSingle("yazıyosunuz", matchesTailLex("Verb + Prog1_Informal + A2pl"));
    t.expectSingle("yazıyonuz", matchesTailLex("Verb + Prog1_Informal + A2pl"));
    t.expectSingle("yazıyolar", matchesTailLex("Verb + Prog1_Informal + A3pl"));

    t.expectSingle("yazıyosa", matchesTailLex("Verb + Prog1_Informal + Cond + A3sg"));
    t.expectSingle("yazıyomuş", matchesTailLex("Verb + Prog1_Informal + Narr + A3sg"));

    t = getTester("bilmek [A:Aorist_I]");
    t.expectSingle("biliyosun", matchesTailLex("Verb + Prog1_Informal + A2sg"));

    t.expectFail(
        "gitiyo",
        "gidyo"
    );
  }

  @Test
  public void testProgressiveDeformation2() {
    AnalysisTester t = getTester("okumak");

    t.expectSingle("okuyom", matchesTailLex("Verb + Prog1_Informal + A1sg"));
    t.expectSingle("okuyon", matchesTailLex("Verb + Prog1_Informal + A2sg"));
    t.expectSingle("okuyosun", matchesTailLex("Verb + Prog1_Informal + A2sg"));
  }

  @Test
  public void progressiveDeformationDrop() {
    AnalysisTester t = getTester("aramak");

    t.expectSingle("arıyom", matchesTailLex("Verb + Prog1_Informal + A1sg"));
    t.expectSingle("arıyo", matchesTailLex("Verb + Prog1_Informal + A3sg"));

    t.expectFail(
        "arayom",
        "ar",
        "ardım"
    );

    t = getTester("yürümek");

    t.expectSingle("yürüyom", matchesTailLex("Verb + Prog1_Informal + A1sg"));
    t.expectSingle("yürüyo", matchesTailLex("Verb + Prog1_Informal + A3sg"));

    t = getTester("denemek");

    t.expectSingle("deniyom", matchesTailLex("Verb + Prog1_Informal + A1sg"));
    t.expectSingle("deniyo", matchesTailLex("Verb + Prog1_Informal + A3sg"));
  }

  @Test
  public void progressiveDeformationNegative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmıyom", matchesTailLex("Verb + Neg + Prog1_Informal + A1sg"));
    t.expectSingle("yazmıyosun", matchesTailLex("Verb + Neg + Prog1_Informal + A2sg"));
    t.expectSingle("yazmıyo", matchesTailLex("Verb + Neg + Prog1_Informal + A3sg"));

    t.expectSingle("yazamıyom", matchesTailLex("Verb + Unable + Prog1_Informal + A1sg"));
    t.expectSingle("yazamıyosun", matchesTailLex("Verb + Unable + Prog1_Informal + A2sg"));
    t.expectSingle("yazamıyo", matchesTailLex("Verb + Unable + Prog1_Informal + A3sg"));

    t = getTester("aramak");

    t.expectSingle("aramıyoz", matchesTailLex("Verb + Neg + Prog1_Informal + A1pl"));
    t.expectSingle("aramıyosunuz", matchesTailLex("Verb + Neg + Prog1_Informal + A2pl"));
    t.expectSingle("aramıyolar", matchesTailLex("Verb + Neg + Prog1_Informal + A3pl"));

    t.expectSingle("arayamıyoz", matchesTailLex("Verb + Unable + Prog1_Informal + A1pl"));
    t.expectSingle("arayamıyosunuz", matchesTailLex("Verb + Unable + Prog1_Informal + A2pl"));
    t.expectSingle("arayamıyolar", matchesTailLex("Verb + Unable + Prog1_Informal + A3pl"));

    t = getTester("affetmek [A:Voicing]");
    t.expectSingle("affetmiyo", matchesTailLex("Verb + Neg + Prog1_Informal + A3sg"));
    t.expectSingle("affedemiyo", matchesTailLex("Verb + Unable + Prog1_Informal + A3sg"));

    t = getTester("demek");
    t.expectSingle("diyo", matchesTailLex("Verb + Prog1_Informal + A3sg"));
  }

  @Test
  public void optativeP1plDeformation() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazak", matchesTailLex("Verb + Opt + A1pl_Informal"));
    t.expectSingle("yazmayak", matchesTailLex("Verb + Neg + Opt + A1pl_Informal"));
    t.expectSingle("yazamayak", matchesTailLex("Verb + Unable + Opt + A1pl_Informal"));

    t = getTester("etmek [A:Voicing]");
    t.expectSingle("edek", matchesTailLex("Verb + Opt + A1pl_Informal"));

    t.expectAny("etmeyek", matchesTailLex("Verb + Neg + Opt + A1pl_Informal"));
    t.expectAny("edemeyek", matchesTailLex("Verb + Unable + Opt + A1pl_Informal"));
  }

  @Test
  public void futureDeformation() {
    AnalysisTester t = getTester("yazmak");

    t.expectAny("yazacam", matchesTailLex("Verb + Fut_Informal + A1sg"));
    t.expectAny("yazcam", matchesTailLex("Verb + Fut_Informal + A1sg"));
    t.expectAny("yazcanız", matchesTailLex("Verb + Fut_Informal + A2pl"));
    t.expectAny("yazıcam", matchesTailLex("Verb + Fut_Informal + A1sg"));

    t.expectAny("yazmıycam", matchesTailLex("Verb + Neg_Informal + Fut_Informal + A1sg"));
    t.expectAny("yazamıycam", matchesTailLex("Verb + Unable_Informal + Fut_Informal + A1sg"));

    t = getTester("eğlenmek");
    t.expectAny("eğlenicem", matchesTailLex("Verb + Fut_Informal + A1sg"));

    t = getTester("etmek [A:Voicing]");
    t.expectAny("edicem", matchesTailLex("Verb + Fut_Informal + A1sg"));

  }

  @Test
  public void futureDeformation2() {
    AnalysisTester t = getTester("gitmek [A:Voicing]");
    t.expectAny("gidicem", matchesTailLex("Verb + Fut_Informal + A1sg"));
  }


  @Test
  public void asciiTolerant1() {
    AnalysisTester t = getTesterAscii("eğlenmek");
    t.expectAny("eglenicem", matchesTailLex("Verb + Fut_Informal + A1sg"));

    t = getTesterAscii("etmek [A:Voicing]");
    t.expectAny("edıcem", matchesTailLex("Verb + Fut_Informal + A1sg"));

  }

  @Test
  public void testProgressiveYDropDeformation() {
    AnalysisTester t = getTester("yazmak");
    t.expectSingle("yazıom", matchesTailLex("Verb + Prog1_Informal + A1sg"));
    t.expectSingle("yazıo", matchesTailLex("Verb + Prog1_Informal + A3sg"));
    t.expectSingle("yazıorum", matchesTailLex("Verb + Prog1_Informal + A1sg"));

    t = getTester("aramak");
    t.expectSingle("aramıoz", matchesTailLex("Verb + Neg + Prog1_Informal + A1pl"));

    t = getTester("affetmek [A:Voicing]");
    t.expectSingle("affetmio", matchesTailLex("Verb + Neg + Prog1_Informal + A3sg"));

    t = getTester("dinlemek", "dinlenmek");
    t.expectAny("dinleniormuş", matchesTailLex("Verb + Prog1_Informal + Narr + A3sg"));

    t = getTester("demek");
    t.expectSingle("dio", matchesTailLex("Verb + Prog1_Informal + A3sg"));
    t.expectSingle("dior", matchesTailLex("Verb + Prog1_Informal + A3sg"));
  }

  @Test
  public void testFutureParticipleDeformation() {
    AnalysisTester t = getTester("kalkmak", "buluşmak", "yapmak", "gitmek [A:Voicing]", "demek");
    t.expectAny("kalkcağını",
        matchesTailLex("Verb + FutPart_Informal + Noun + A3sg + P3sg + Acc"));
    t.expectAny("buluşabilceğimiz",
        matchesTailLex("Verb + Able + Verb + FutPart_Informal + Adj + P1pl"));
    t.expectAny("yapmıycağını",
        matchesTailLex("Verb + Neg_Informal + FutPart_Informal + Noun + A3sg + P3sg + Acc"));
    t.expectAny("gidiceğim",
        matchesTailLex("Verb + FutPart_Informal + Adj + P1sg"));
    t.expectAny("diyceği",
        matchesTailLex("Verb + FutPart_Informal + Adj + P3sg"));
  }

  @Test
  public void testInformalSurfaceConversion() {
    RootLexicon lexicon = TurkishDictionaryLoader.load(
        "yazmak", "dinlenmek", "demek", "kalkmak", "buluşmak", "yapmak", "gitmek [A:Voicing]");
    TurkishMorphology morphology = TurkishMorphology.builder(lexicon)
        .disableUnidentifiedTokenAnalyzer()
        .useInformalAnalysis()
        .build();
    InformalAnalysisConverter converter =
        new InformalAnalysisConverter(morphology.getWordGenerator());

    String[][] pairs = {
        {"yazıom", "yazıyorum"},
        {"dinleniormuş", "dinleniyormuş"},
        {"dio", "diyor"},
        {"kalkcağını", "kalkacağını"},
        {"buluşabilceğimiz", "buluşabileceğimiz"},
        {"yapmıycağını", "yapmayacağını"},
        {"gidiceğim", "gideceğim"},
        {"diyceği", "diyeceği"},
    };
    for (String[] pair : pairs) {
      WordAnalysis analyses = morphology.analyze(pair[0]);
      Assert.assertTrue("Expected analysis for " + pair[0], analyses.isCorrect());
      boolean matched = false;
      for (SingleAnalysis a : analyses) {
        String surface = converter.convert(pair[0], a).surface;
        if (pair[1].equals(surface)) {
          matched = true;
          break;
        }
      }
      Assert.assertTrue("Expected " + pair[0] + " -> " + pair[1], matched);
    }
  }


}
