package zemberek.morphology.analysis;

import org.junit.Test;

public class VerbsTest extends AnalyzerTestBase {

  @Test
  public void Imp() {
    AnalysisTester t = getTester("okumak");

    t.expectSingle("oku", matchesTailLex("Verb + Imp + A2sg"));
    t.expectSingle("okusun", matchesTailLex("Verb + Imp + A3sg"));    
    t.expectSingle("okuyun", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("okuyunuz", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("okusunlar", matchesTailLex("Verb + Imp + A3pl"));
  }

  @Test
  public void ImpNeg() {
    AnalysisTester t = getTester("okumak");

    t.expectSingle("okuma", matchesTailLex("Verb + Neg + Imp + A2sg"));
    t.expectSingle("okumasın", matchesTailLex("Verb + Neg + Imp + A3sg"));
    t.expectSingle("okumayın", matchesTailLex("Verb + Neg + Imp + A2pl"));
    t.expectSingle("okumayınız", matchesTailLex("Verb + Neg + Imp + A2pl"));
    t.expectSingle("okumasınlar", matchesTailLex("Verb + Neg + Imp + A3pl"));
  }


  @Test
  public void progressivePositive() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("yazıyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("yazıyor", matchesTailLex("Verb + Prog1 + A3sg"));
    t.expectSingle("yazıyoruz", matchesTailLex("Verb + Prog1 + A1pl"));
    t.expectSingle("yazıyorsunuz", matchesTailLex("Verb + Prog1 + A2pl"));
    t.expectSingle("yazıyorlar", matchesTailLex("Verb + Prog1 + A3pl"));

    t = getTester("gitmek [A:Voicing]");
    t.expectSingle("gidiyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("gidiyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("gidiyor", matchesTailLex("Verb + Prog1 + A3sg"));

    t.expectFail(
        "gitiyor",
        "gidyor"
    );
  }

  @Test
  public void progressiveDrop() {
    AnalysisTester t = getTester("aramak");

    t.expectSingle("arıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("arıyor", matchesTailLex("Verb + Prog1 + A3sg"));

    t.expectFail(
        "arayorum"
    );

    t = getTester("yürümek");

    t.expectSingle("yürüyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("yürüyor", matchesTailLex("Verb + Prog1 + A3sg"));

    t = getTester("denemek");

    t.expectSingle("deniyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("deniyor", matchesTailLex("Verb + Prog1 + A3sg"));
  }

  @Test
  public void progressiveNegative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmıyorum", matchesTailLex("Verb + Neg + Prog1 + A1sg"));
    t.expectSingle("yazmıyorsun", matchesTailLex("Verb + Neg + Prog1 + A2sg"));
    t.expectSingle("yazmıyor", matchesTailLex("Verb + Neg + Prog1 + A3sg"));

    t.expectFail(
        "yazmayorum"
    );

    t = getTester("aramak");

    t.expectSingle("aramıyoruz", matchesTailLex("Verb + Neg + Prog1 + A1pl"));
    t.expectSingle("aramıyorsunuz", matchesTailLex("Verb + Neg + Prog1 + A2pl"));
    t.expectSingle("aramıyorlar", matchesTailLex("Verb + Neg + Prog1 + A3pl"));

    t.expectFail(
        "aramayoruz",
        "armıyoruz",
        "armıyor"
    );
  }

  @Test
  public void aorist() {
    AnalysisTester t = getTester("yazmak"); // Aorist_A attribute is inferred.

    t.expectSingle("yazarım", matchesTailLex("Verb + Aor + A1sg"));
    t.expectSingle("yazarsın", matchesTailLex("Verb + Aor + A2sg"));
    t.expectSingle("yazar", matchesTailLex("Verb + Aor + A3sg"));
    t.expectSingle("yazarız", matchesTailLex("Verb + Aor + A1pl"));
    t.expectSingle("yazarlar", matchesTailLex("Verb + Aor + A3pl"));

    t.expectSingle("yazdırır",
        matchesTailLex("Verb + Caus + Verb + Aor + A3sg"));
    t.expectSingle("yazdırtır",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Aor + A3sg"));
    t.expectSingle("yazdırttırır",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Aor + A3sg"));

    t = getTester("semirmek");
    t.expectSingle("semiririm", matchesTailLex("Verb + Aor + A1sg"));
    t.expectSingle("semirirsin", matchesTailLex("Verb + Aor + A2sg"));
    t.expectSingle("semirir", matchesTailLex("Verb + Aor + A3sg"));

    t.expectSingle("semirtirim",
        matchesTailLex("Verb + Caus + Verb + Aor + A1sg"));
    t.expectSingle("semirttiririm",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Aor + A1sg"));
  }

  @Test
  public void aoristNegative() {
    AnalysisTester t = getTester("yazmak"); // Aorist_A attribute is inferred.

    t.expectSingle("yazmam", matchesTailLex("Verb + Neg + Aor + A1sg"));
    t.expectSingle("yazmazsın", matchesTailLex("Verb + Neg + Aor + A2sg"));
    t.expectSingle("yazmaz", matchesTailLex("Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazmayız", matchesTailLex("Verb + Neg + Aor + A1pl"));
    t.expectSingle("yazmazsınız", matchesTailLex("Verb + Neg + Aor + A2pl"));
    t.expectSingle("yazmazlar", matchesTailLex("Verb + Neg + Aor + A3pl"));

    t.expectSingle("yazdırmaz",
        matchesTailLex("Verb + Caus + Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazdırtmaz",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazdırttırmazsınız",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Neg + Aor + A2pl"));

    t = getTester("semirmek");
    t.expectSingle("semirmem", matchesTailLex("Verb + Neg + Aor + A1sg"));
    t.expectSingle("semirmezsin", matchesTailLex("Verb + Neg + Aor + A2sg"));
    t.expectSingle("semirmez", matchesTailLex("Verb + Neg + Aor + A3sg"));

    t.expectSingle("semirtmem",
        matchesTailLex("Verb + Caus + Verb + Neg + Aor + A1sg"));
    t.expectSingle("semirttirmem",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Neg + Aor + A1sg"));
  }

}
