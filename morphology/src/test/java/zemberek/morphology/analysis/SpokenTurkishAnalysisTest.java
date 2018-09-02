package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.SpokenTurkishMorphotactics;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class SpokenTurkishAnalysisTest extends AnalyzerTestBase {

  public static TurkishMorphotactics getSpokenMorphotactics(String... dictionaryLines) {
    RootLexicon lexicon = TurkishDictionaryLoader.load(dictionaryLines);
    return new SpokenTurkishMorphotactics(lexicon);
  }

  static AnalysisTester getTester(String... dictionaryLines) {
    return new AnalysisTester(InterpretingAnalyzer.forDebug(getSpokenMorphotactics(dictionaryLines)));
  }

  @Test
  public void testProgressiveDeformation() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıyom", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("yazıyon", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("yazıyosun", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("yazıyo", matchesTailLex("Verb + Prog1 + A3sg"));
    t.expectSingle("yazıyoz", matchesTailLex("Verb + Prog1 + A1pl"));
    t.expectSingle("yazıyosunuz", matchesTailLex("Verb + Prog1 + A2pl"));
    t.expectSingle("yazıyonuz", matchesTailLex("Verb + Prog1 + A2pl"));
    t.expectSingle("yazıyolar", matchesTailLex("Verb + Prog1 + A3pl"));

    t.expectSingle("yazıyosa", matchesTailLex("Verb + Prog1 + Cond + A3sg"));
    t.expectSingle("yazıyomuş", matchesTailLex("Verb + Prog1 + Narr + A3sg"));

    t = getTester("bilmek [A:Aorist_I]");
    t.expectSingle("biliyosun", matchesTailLex("Verb + Prog1 + A2sg"));

    t.expectFail(
        "gitiyo",
        "gidyo"
    );
  }

  @Test
  public void testProgressiveDeformation2() {
    AnalysisTester t = getTester("okumak");

    t.expectSingle("okuyom", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("okuyon", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("okuyosun", matchesTailLex("Verb + Prog1 + A2sg"));
  }

  @Test
  public void progressiveDeformationDrop() {
    AnalysisTester t = getTester("aramak");

    t.expectSingle("arıyom", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("arıyo", matchesTailLex("Verb + Prog1 + A3sg"));

    t.expectFail(
        "arayom",
        "ar",
        "ardım"
    );

    t = getTester("yürümek");

    t.expectSingle("yürüyom", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("yürüyo", matchesTailLex("Verb + Prog1 + A3sg"));

    t = getTester("denemek");

    t.expectSingle("deniyom", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("deniyo", matchesTailLex("Verb + Prog1 + A3sg"));
  }

  @Test
  public void progressiveDeformationNegative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmıyom", matchesTailLex("Verb + Neg + Prog1 + A1sg"));
    t.expectSingle("yazmıyosun", matchesTailLex("Verb + Neg + Prog1 + A2sg"));
    t.expectSingle("yazmıyo", matchesTailLex("Verb + Neg + Prog1 + A3sg"));

    t = getTester("aramak");

    t.expectSingle("aramıyoz", matchesTailLex("Verb + Neg + Prog1 + A1pl"));
    t.expectSingle("aramıyosunuz", matchesTailLex("Verb + Neg + Prog1 + A2pl"));
    t.expectSingle("aramıyolar", matchesTailLex("Verb + Neg + Prog1 + A3pl"));

    t = getTester("affetmek [A:Voicing]");
    t.expectSingle("affetmiyo", matchesTailLex("Verb + Neg + Prog1 + A3sg"));

  }

  @Test
  public void optativeP1plDeformation() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazak", matchesTailLex("Verb + Opt + A1pl"));
    t.expectSingle("yazmayak", matchesTailLex("Verb + Neg + Opt + A1pl"));

    t = getTester("etmek [A:Voicing]");
    t.expectSingle("edek", matchesTailLex("Verb + Opt + A1pl"));
    t.expectAny("etmeyek", matchesTailLex("Verb + Neg + Opt + A1pl"));
  }

  @Test
  public void futureDeformation() {
    AnalysisTester t = getTester("yazmak");

    t.expectAny("yazacam", matchesTailLex("Verb + Fut + A1sg"));
    t.expectAny("yazcam", matchesTailLex("Verb + Fut + A1sg"));
    t.expectAny("yazıcam", matchesTailLex("Verb + Fut + A1sg"));

    t.expectAny("yazmıycam", matchesTailLex("Verb + Neg + Fut + A1sg"));

    // TODO: Add more tests.
  }


}
