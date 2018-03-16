package zemberek.morphology._analyzer;

import org.junit.Test;
import zemberek.morphology._morphotactics.Morpheme;

public class VerbDerivationTest extends AnalyzerTestBase {

  @Test
  public void causativeTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okut",
        matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("okuttur",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("okutturt",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("okutul",
        matchesTailLex("Verb + Caus + Verb + Pass + Verb + Imp + A2sg"));

    tester.expectFail(
        "okutt",
        "okuturtur"
    );

    // "okutur" should not have a causative analysis
    tester.expectFalse("okutur", matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));

    tester = getTester("semirmek");
    tester.expectSingle("semirt",
        matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("semirttir",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));
    tester.expectSingle("semirttirt",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Imp + A2sg"));

    tester.expectFail(
        "semirtt",
        "semirtirtir"
    );
  }

  @Test
  public void infinitive1() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okumak",
        matchesTailLex("Verb + Inf1 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okumamak",
        matchesTailLex("Verb + Neg + Inf1 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmak",
        matchesTailLex("Verb + Caus + Verb + Inf1 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmaktan",
        matchesTailLex("Verb + Caus + Verb + Inf1 + Noun + A3sg + Pnon + Abl"));

    tester.expectFail(
        "okumaka",
        "okumaklar",
        "okumağ",
        "okumağı"
    );
  }

  @Test
  public void infinitive2() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuma",
        matchesTailLex("Verb + Inf2 + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okumama",
        matchesTailLex("Verb + Neg + Inf2 + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okumama",
        matchesTailLex("Verb + Inf2 + Noun + A3sg + P1sg + Dat"));
    tester.expectAny("okutma",
        matchesTailLex("Verb + Caus + Verb + Inf2 + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okutma",
        matchesTailLex("Verb + Caus + Verb + Neg + Imp + A2sg"));
    tester.expectAny("okutmama",
        matchesTailLex("Verb + Caus + Verb + Inf2 + Noun + A3sg + P1sg + Dat"));
    tester.expectAny("okuması",
        matchesTailLex("Verb + Inf2 + Noun + A3sg + P3sg + Nom"));
    tester.expectAny("okutması",
        matchesTailLex("Verb + Caus + Verb + Inf2 + Noun + A3sg + P3sg + Nom"));
    tester.expectAny("okutulması",
        matchesTailLex("Verb + Caus + Verb + Pass + Verb + Inf2 + Noun + A3sg + P3sg + Nom"));

    tester.expectAny("okutmamadan",
        matchesTailLex("Verb + Caus + Verb + Neg + Inf2 + Noun + A3sg + Pnon + Abl"));
  }


  @Test
  public void infinitive3() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okuyuş",
        matchesTailLex("Verb + Inf3 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okumayış",
        matchesTailLex("Verb + Neg + Inf3 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmayış",
        matchesTailLex("Verb + Caus + Verb + Neg + Inf3 + Noun + A3sg + Pnon + Nom"));
    tester.expectSingle("okutmayıştan",
        matchesTailLex("Verb + Caus + Verb + Neg + Inf3 + Noun + A3sg + Pnon + Abl"));
  }

  @Test
  public void pastPartTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuduk",
        matchesTailLex("Verb + PastPart + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okuduk",
        matchesTailLex("Verb + PastPart + Adj + Pnon"));
    tester.expectAny("okumadık",
        matchesTailLex("Verb + Neg + PastPart + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okumadık",
        matchesTailLex("Verb + Neg + PastPart + Adj + Pnon"));
    tester.expectAny("okumadığım",
        matchesTailLex("Verb + Neg + PastPart + Noun + A3sg + P1sg + Nom"));
    tester.expectAny("okumadığım",
        matchesTailLex("Verb + Neg + PastPart + Adj + P1sg"));
    tester.expectAny("okuttuğumuz",
        matchesTailLex("Verb + Caus + Verb + PastPart + Noun + A3sg + P1pl + Nom"));
    tester.expectAny("okuttuğumuzdu",
        matchesTailLex("Verb + Caus + Verb + PastPart + Noun + A3sg + P1pl + Nom + Zero + Verb + Past + A3sg"));

    // false positive test
    tester.expectFalse("okuduğum",
        matchesTailLex("Verb + PastPart + Noun + A3sg + Pnon + Nom + Zero + Verb + Pres + A1sg"));
  }

  @Test
  public void futurePartTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuyacak",
        matchesTailLex("Verb + FutPart + Adj + Pnon"));
    tester.expectAny("okumayacak",
        matchesTailLex("Verb + Neg + FutPart + Adj + Pnon"));
    tester.expectAny("okumayacağım",
        matchesTailLex("Verb + Neg + FutPart + Noun + A3sg + P1sg + Nom"));
    tester.expectAny("okumayacağım",
        matchesTailLex("Verb + Neg + FutPart + Adj + P1sg"));
    tester.expectAny("okutmayacağımız",
        matchesTailLex("Verb + Caus + Verb + Neg + FutPart + Noun + A3sg + P1pl + Nom"));
    tester.expectAny("okutmayacağımızdı",
        matchesTailLex("Verb + Caus + Verb + Neg + FutPart + Noun + A3sg + P1pl + Nom + Zero + Verb + Past + A3sg"));

    // false positive test
    tester.expectFalse("okuyacağım",
        matchesTailLex("Verb + FutPart + Noun + A3sg + Pnon + Nom + Zero + Verb + Pres + A1sg"));
    // Oflazer does not allow Noun+A3sg+Pnon+Nom
    tester.expectFalse("okuyacak",
        matchesTailLex("Verb + FutPart + Noun + A3sg + Pnon + Nom"));
    tester.expectFalse("okumayacak",
        matchesTailLex("Verb + Neg + FutPart + Noun + A3sg + Pnon + Nom"));



    tester = getTester("cam");
    tester.expectAny("camlaşmayabileceği",
        matchesTailLex("FutPart + Adj + P3sg"));
  }

  @Test
  public void presPartTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuyan",
        matchesTailLex("Verb + PresPart + Adj"));
    tester.expectAny("okumayan",
        matchesTailLex("Verb + Neg + PresPart + Adj"));
    tester.expectAny("okumayana",
        matchesTailLex("Verb + Neg + PresPart + Noun + A3sg + Pnon + Dat"));
    tester.expectAny("okutmayanda",
        matchesTailLex("Verb + Caus + Verb + Neg + PresPart + Noun + A3sg + Pnon + Loc"));
  }

  @Test
  public void multiVerbtoVerbTest() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okuyagel",
        matchesTailLex("Verb + EverSince + Verb + Imp + A2sg"));
    tester.expectSingle("okuyadur",
        matchesTailLex("Verb + Repeat + Verb + Imp + A2sg"));
    tester.expectSingle("okuyagör",
        matchesTailLex("Verb + Repeat + Verb + Imp + A2sg"));
    tester.expectSingle("okuyayaz",
        matchesTailLex("Verb + Almost + Verb + Imp + A2sg"));
    tester.expectSingle("okuyuver",
        matchesTailLex("Verb + Hastily + Verb + Imp + A2sg"));
    tester.expectSingle("okuyakal",
        matchesTailLex("Verb + Stay + Verb + Imp + A2sg"));
    tester.expectSingle("okuyakoy",
        matchesTailLex("Verb + Start + Verb + Imp + A2sg"));
  }

  @Test
  public void adverbDerivation() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okurcasına",
        matchesTailLex("Verb + Aor + A3sg + AsIf + Adv"));
    tester.expectSingle("okumazcasına",
        matchesTailLex("Verb + Neg + Aor + A3sg + AsIf + Adv"));
    tester.expectSingle("okumuşçasına",
        matchesTailLex("Verb + Narr + A3sg + AsIf + Adv"));
    tester.expectSingle("okuyalı",
        matchesTailLex("Verb + SinceDoingSo + Adv"));
    tester.expectSingle("okudukça",
        matchesTailLex("Verb + AsLongAs + Adv"));
    tester.expectSingle("okuyarak",
        matchesTailLex("Verb + ByDoingSo + Adv"));
    tester.expectSingle("okuyup",
        matchesTailLex("Verb + AfterDoing + Adv"));
    tester.expectAny("okumadan",
        matchesTailLex("Verb + WithoutHavingDoneSo + Adv"));
    tester.expectSingle("okumaksızın",
        matchesTailLex("Verb + WithoutHavingDoneSo + Adv"));
    tester.expectSingle("okuyamadan",
        matchesTailLex("Verb + WithoutBeingAbleToHaveDoneSo + Adv"));
  }

  @Test
  public void whileAdverbDerivation() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("okurken",
        matchesTailLex("Verb + Aor + While + Adv"));
    tester.expectSingle("okurlarken",
        matchesTailLex("Verb + Aor + A3pl + While + Adv"));
    tester.expectSingle("okumazken",
        matchesTailLex("Verb + Neg + Aor + While + Adv"));
    tester.expectSingle("okuyorken",
        matchesTailLex("Verb + Prog1 + While + Adv"));
    tester.expectSingle("okumaktayken",
        matchesTailLex("Verb + Prog2 + While + Adv"));
    tester.expectAny("okuyacakken",
        matchesTailLex("Verb + Fut + While + Adv"));
    tester.expectSingle("okumuşken",
        matchesTailLex("Verb + Narr + While + Adv"));
    tester.expectSingle("okuyabilirken",
        matchesTailLex("Verb + Able + Verb + Aor + While + Adv"));

    tester.expectFail(
        "okuduyken",
        "okurumken",
        "okudularken"
    );
  }


  @Test
  public void agt() {
    AnalysisTester tester = getTester("okumak");

    tester.expectAny("okuyucu",
        matchesTailLex("Verb + Agt + Adj"));
    tester.expectAny("okuyucu",
        matchesTailLex("Verb + Agt + Noun + A3sg + Pnon + Nom"));
    tester.expectAny("okutucu",
        matchesTailLex("Verb + Caus + Verb + Agt + Adj"));

    tester.expectFail(
        "okuyucucu"
    );
  }


}
