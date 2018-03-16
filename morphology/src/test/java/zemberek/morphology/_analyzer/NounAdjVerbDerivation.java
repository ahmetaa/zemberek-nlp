package zemberek.morphology._analyzer;

import org.junit.Test;

public class NounAdjVerbDerivation extends AnalyzerTestBase {

  @Test
  public void become() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtalaş",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Imp + A2sg"));
    t.expectAny("tahtalaştık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Past + A1pl"));
    t.expectAny("tahtalaşacak",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Fut + A3sg"));

    t.expectFail(
        "tahtamlaş",
        "tahtalarlaştı",
        "tahtayalaştı"
    );

    t = getTester("kitap");
    t.expectAny("kitaplaştı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Past + A3sg"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağlaştık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Become + Verb + Past + A1pl"));
  }

  @Test
  public void acquire() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtalan",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Imp + A2sg"));
    t.expectAny("tahtalandık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Past + A1pl"));
    t.expectAny("tahtalanacak",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Fut + A3sg"));

    t.expectFail(
        "tahtamlan",
        "tahtalarlandı",
        "tahtayaland"
    );

    t = getTester("kitap");
    t.expectAny("kitaplandı",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Past + A3sg"));

    t = getTester(
        "zeytin",
        "yağ",
        "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");

    t.expectAny("zeytinyağlandık",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Acquire + Verb + Past + A1pl"));
  }

  @Test
  public void whileTest() {
    AnalysisTester t = getTester("tahta");

    t.expectAny("tahtayken",
        matchesTailLex("Noun + A3sg + Pnon + Nom + Zero + Verb + While + Adv"));
    t.expectAny("tahtamken",
        matchesTailLex("Noun + A3sg + P1sg + Nom + Zero + Verb + While + Adv"));
  }


}
