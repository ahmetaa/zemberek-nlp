package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;

public class PronounMorphotacticsAndLexiconTest extends AnalyzerTestBase {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.createWithDefaults();
  }

  @Test
  public void noSpuriousDemonstrativeAdjectiveParses() {
    List<SingleAnalysis> olan = defaultMorphology.analyze("olan").getAnalysisResults();
    Assert.assertTrue(olan.stream().anyMatch(a -> a.getDictionaryItem().lemma.equals("olmak")));
    Assert.assertTrue(olan.stream().noneMatch(a -> a.getDictionaryItem().lemma.equals("o")));

    List<SingleAnalysis> omuzum = defaultMorphology.analyze("omuzum").getAnalysisResults();
    Assert.assertTrue(omuzum.stream().noneMatch(a -> a.getDictionaryItem().lemma.equals("o")));

    List<SingleAnalysis> odalar = defaultMorphology.analyze("odalar").getAnalysisResults();
    Assert.assertTrue(odalar.stream().noneMatch(a -> a.getDictionaryItem().lemma.equals("o")));

    List<SingleAnalysis> surada = defaultMorphology.analyze("şurada").getAnalysisResults();
    Assert.assertTrue(
        surada.stream()
            .noneMatch(
                a ->
                    a.getDictionaryItem().lemma.equals("şu")
                        && a.getDictionaryItem().primaryPos == PrimaryPos.Adjective));

    // Determiner and Pronoun entries remain intact.
    List<SingleAnalysis> o = defaultMorphology.analyze("o").getAnalysisResults();
    Assert.assertTrue(
        o.stream().anyMatch(a -> a.getDictionaryItem().primaryPos == PrimaryPos.Determiner));
    Assert.assertTrue(
        o.stream()
            .anyMatch(a -> a.getDictionaryItem().secondaryPos == SecondaryPos.DemonstrativePron));
    Assert.assertTrue(
        o.stream().anyMatch(a -> a.getDictionaryItem().secondaryPos == SecondaryPos.PersonalPron));

    List<SingleAnalysis> su = defaultMorphology.analyze("şu").getAnalysisResults();
    Assert.assertTrue(
        su.stream().anyMatch(a -> a.getDictionaryItem().primaryPos == PrimaryPos.Determiner));
    Assert.assertTrue(
        su.stream()
            .anyMatch(a -> a.getDictionaryItem().secondaryPos == SecondaryPos.DemonstrativePron));
  }

  @Test
  public void questionPronounP2pl() {
    AnalysisTester tester =
        getTester("kim [P:Pron,Ques]", "ne [P:Pron,Ques]", "nere [P:Pron,Ques]");
    tester.expectAny("neyiniz", matchesTailLex("Pron + A3sg + P2pl"));
    tester.expectSingle("neyinize", matchesTailLex("Pron + A3sg + P2pl + Dat"));
    tester.expectAny("kiminiz", matchesTailLex("Pron + A3sg + P2pl"));
    tester.expectSingle("kiminizde", matchesTailLex("Pron + A3sg + P2pl + Loc"));
    tester.expectAny("nereniz", matchesTailLex("Pron + A3sg + P2pl"));
    tester.expectAny("neleriniz", matchesTailLex("Pron + A3pl + P2pl"));
    tester.expectAny("kimleriniz", matchesTailLex("Pron + A3pl + P2pl"));

    Assert.assertTrue(hasMatch(defaultMorphology.analyze("neyiniz"), "ne", "Pron + A3sg + P2pl"));
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("kiminiz"), "kim", "Pron + A3sg + P2pl"));
  }

  @Test
  public void falanFalancaBerikiPronounsInDefaultLexicon() {
    Assert.assertTrue(
        defaultMorphology.analyze("falan").getAnalysisResults().stream()
            .anyMatch(a -> a.getDictionaryItem().id.equals("falan_Pron_Pers")));
    Assert.assertTrue(hasMatch(defaultMorphology.analyze("falana"), "falan", "Pron + A3sg + Dat"));

    Assert.assertTrue(
        defaultMorphology.analyze("falanca").getAnalysisResults().stream()
            .anyMatch(a -> a.getDictionaryItem().id.equals("falanca_Pron_Pers")));
    Assert.assertTrue(
        hasMatch(defaultMorphology.analyze("falancaya"), "falanca", "Pron + A3sg + Dat"));

    Assert.assertTrue(
        defaultMorphology.analyze("beriki").getAnalysisResults().stream()
            .anyMatch(a -> a.getDictionaryItem().id.equals("beriki_Pron_Quant")));
    Assert.assertTrue(
        hasMatch(defaultMorphology.analyze("berikini"), "beriki", "Pron + A3sg + P3sg + Acc"));
    Assert.assertTrue(
        hasMatch(defaultMorphology.analyze("berikiler"), "beriki", "Pron + A3pl"));
  }

  @Test
  public void pronounGenitiveRelProducesPronounIncludingBen() {
    AnalysisTester tester =
        getTester(
            "ben [P:Pron,Pers]",
            "sen [P:Pron,Pers]",
            "o [P:Pron,Pers]",
            "biz [P:Pron,Pers]",
            "siz [P:Pron,Pers]",
            "bu [P:Pron,Demons]",
            "kim [P:Pron,Ques]");
    tester.expectSingle("benimki", matchesTailLex("Pron + A1sg + Gen + Rel + Pron + A3sg"));
    tester.expectSingle("benimkini", matchesTailLex("Pron + A1sg + Gen + Rel + Pron + A3sg + Acc"));
    tester.expectSingle("benimkiler", matchesTailLex("Pron + A1sg + Gen + Rel + Pron + A3pl"));
    tester.expectSingle("seninki", matchesTailLex("Pron + A2sg + Gen + Rel + Pron + A3sg"));
    tester.expectSingle("seninkini", matchesTailLex("Pron + A2sg + Gen + Rel + Pron + A3sg + Acc"));
    tester.expectSingle("onunki", matchesTailLex("Pron + A3sg + Gen + Rel + Pron + A3sg"));
    tester.expectSingle("bizimki", matchesTailLex("Pron + A1pl + Gen + Rel + Pron + A3sg"));
    tester.expectSingle("sizinki", matchesTailLex("Pron + A2pl + Gen + Rel + Pron + A3sg"));
    tester.expectSingle("bununki", matchesTailLex("Pron + A3sg + Gen + Rel + Pron + A3sg"));
    tester.expectSingle("kiminki", matchesTailLex("Pron + A3sg + Gen + Rel + Pron + A3sg"));

    Assert.assertTrue(
        hasMatch(
            defaultMorphology.analyze("benimkiler"),
            "ben",
            "Pron + A1sg + Gen + Rel + Pron + A3pl"));
  }

  private boolean hasMatch(WordAnalysis analysis, String lemma, String lexicalTail) {
    return analysis.getAnalysisResults().stream()
        .anyMatch(
            a ->
                a.getDictionaryItem().lemma.equals(lemma)
                    && formatLexicalSequence(a).endsWith(lexicalTail));
  }
}
