package zemberek.morphology.analysis;

import org.junit.Test;

public class VerbalAndCopularMorphotacticsTest extends AnalyzerTestBase {

  @Test
  public void testSpuriousCopulaWhileAndFeelLikeBlocked() {
    AnalysisTester verbTester = getTester("yazmak", "okumak");
    // Block double A3pl across Copula (yazacak-lar-dır-lar).
    verbTester.expectFail("yazacaklardırlar");
    verbTester.expectAny("yazacaktırlar", matchesTailLex("Cop + A3pl"));

    AnalysisTester nounTester = getTester("elma");
    nounTester.expectFail("elmalardırlar");
    nounTester.expectSingle(
        "elmadırlar", matchesTailLex("Noun + A3sg + Zero + Verb + Pres + Cop + A3pl"));

    // Block duplicate NarrPart->Adj->Zero->Verb+While on "okumuşken".
    verbTester.expectSingle("okumuşken", matchesTailLex("Verb + Narr + While + Adv"));
    verbTester.expectAny(
        "okumuşlarken",
        matchesTailLex("Verb + NarrPart + Adj + Zero + Noun + A3pl + Zero + Verb + While + Adv"));

    // Block FeelLike + P2sg + Zero->Verb + A1pl on "yazasınız".
    verbTester.expectFalse(
        "yazasınız",
        matchesTailLex("FeelLike + Noun + A3sg + P2sg + Zero + Verb + Pres + A1pl"));
    verbTester.expectAny("yazasınız", matchesTailLex("Verb + Opt + A2pl"));
    verbTester.expectAny("yazasınız", matchesTailLex("Verb + FeelLike + Noun + A3sg + P2pl"));
  }

  @Test
  public void testObliquePluralPastNarrAndNegativeAoristAblative() {
    // Oblique plural nominal/pronominal predicates with Past/Narr/Cond/While.
    AnalysisTester nounTester = getTester("ev", "elma");
    nounTester.expectSingle(
        "evdelerdi", matchesTailLex("Noun + A3sg + Loc + Zero + Verb + Pres + A3pl + Past"));
    nounTester.expectSingle(
        "evdelermiş", matchesTailLex("Noun + A3sg + Loc + Zero + Verb + Pres + A3pl + Narr"));
    nounTester.expectSingle(
        "evdelerse", matchesTailLex("Noun + A3sg + Loc + Zero + Verb + Pres + A3pl + Cond"));
    nounTester.expectSingle(
        "evdelerken",
        matchesTailLex("Noun + A3sg + Loc + Zero + Verb + Pres + A3pl + While + Adv"));
    nounTester.expectSingle("elmalardı", matchesTailLex("Noun + A3pl + Zero + Verb + Past + A3sg"));
    nounTester.expectFail("evdeydilerdi", "evdelerdiler");

    AnalysisTester pronTester = getTester("nere [P:Pron,Ques]", "biz [P:Pron,Pers]");
    pronTester.expectSingle(
        "neredelermiş", matchesTailLex("Pron + A3sg + Loc + Zero + Verb + Pres + A3pl + Narr"));
    pronTester.expectSingle(
        "bizdelerdi", matchesTailLex("Pron + A1pl + Loc + Zero + Verb + Pres + A3pl + Past"));

    // Negative aorist participle + ablative ("bilmezden", "görmezden", "duymazdan").
    AnalysisTester negAorTester = getTester("bilmek", "görmek", "duymak", "darılmak");
    negAorTester.expectSingle(
        "bilmezden", matchesTailLex("Verb + Neg + AorPart + Adj + Zero + Noun + A3sg + Abl"));
    negAorTester.expectSingle(
        "görmezden", matchesTailLex("Verb + Neg + AorPart + Adj + Zero + Noun + A3sg + Abl"));
    negAorTester.expectSingle(
        "duymazdan", matchesTailLex("Verb + Neg + AorPart + Adj + Zero + Noun + A3sg + Abl"));
    negAorTester.expectSingle(
        "göremezden", matchesTailLex("Verb + Unable + AorPart + Adj + Zero + Noun + A3sg + Abl"));
    negAorTester.expectFail("darılmazda", "bilmezde");
  }
}
