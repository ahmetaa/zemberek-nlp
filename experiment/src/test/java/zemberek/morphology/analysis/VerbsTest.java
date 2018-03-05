package zemberek.morphology.analysis;

import org.junit.Test;

public class VerbsTest extends AnalyzerTestBase {

  @Test
  public void Imp() {
    AnalysisTester tester = getTester("okumak");

    tester.expectSingle("oku", matchesTailLex("Verb + Imp + A2sg"));
    tester.expectSingle("okusun", matchesTailLex("Verb + Imp + A3sg"));
    tester.expectSingle("okusunlar", matchesTailLex("Verb + Imp + A3pl"));
  }


  @Test
  public void progressive1() {
    AnalysisTester tester = getTester("yazmak");

    tester.expectSingle("yazıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("yazıyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    tester.expectSingle("yazıyor", matchesTailLex("Verb + Prog1 + A3sg"));
    tester.expectSingle("yazıyoruz", matchesTailLex("Verb + Prog1 + A1pl"));
    tester.expectSingle("yazıyorlar", matchesTailLex("Verb + Prog1 + A3pl"));

    tester = getTester("gitmek [A:Voicing]");
    tester.expectSingle("gidiyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("gidiyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    tester.expectSingle("gidiyor", matchesTailLex("Verb + Prog1 + A3sg"));
  }

  @Test
  public void progressive1Drop() {
    AnalysisTester tester = getTester("aramak");

    tester.expectSingle("arıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("arıyor", matchesTailLex("Verb + Prog1 + A3sg"));

    tester = getTester("yürümek");

    tester.expectSingle("yürüyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("yürüyor", matchesTailLex("Verb + Prog1 + A3sg"));

    tester = getTester("denemek");

    tester.expectSingle("deniyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    tester.expectSingle("deniyor", matchesTailLex("Verb + Prog1 + A3sg"));
  }

  @Test
  public void aorist1() {
    AnalysisTester tester = getTester("yazmak"); // Aorist_A attribute is inferred.

    tester.expectSingle("yazarım", matchesTailLex("Verb + Aor + A1sg"));
    tester.expectSingle("yazarsın", matchesTailLex("Verb + Aor + A2sg"));
    tester.expectSingle("yazar", matchesTailLex("Verb + Aor + A3sg"));
    tester.expectSingle("yazarız", matchesTailLex("Verb + Aor + A1pl"));
    tester.expectSingle("yazarlar", matchesTailLex("Verb + Aor + A3pl"));

    tester.expectSingle("yazdırır",
        matchesTailLex("Verb + Caus + Verb + Aor + A3sg"));
    tester.expectSingle("yazdırtır",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Aor + A3sg"));
    tester.expectSingle("yazdırttırır",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Aor + A3sg"));

    tester = getTester("semirmek");
    tester.expectSingle("semiririm", matchesTailLex("Verb + Aor + A1sg"));
    tester.expectSingle("semirirsin", matchesTailLex("Verb + Aor + A2sg"));
    tester.expectSingle("semirir", matchesTailLex("Verb + Aor + A3sg"));

    tester.expectSingle("semirtirim",
        matchesTailLex("Verb + Caus + Verb + Aor + A1sg"));
    tester.expectSingle("semirttiririm",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Aor + A1sg"));

  }

}
