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

}
