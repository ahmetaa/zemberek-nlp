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

    t = getTester("yazmak");

    t.expectSingle("yaz", matchesTailLex("Verb + Imp + A2sg"));
    t.expectSingle("yazsın", matchesTailLex("Verb + Imp + A3sg"));
    t.expectSingle("yazın", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("yazınız", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("yazsınlar", matchesTailLex("Verb + Imp + A3pl"));
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
  public void progressivePositive2() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıyordum", matchesTailLex("Verb + Prog1 + Past + A1sg"));
    t.expectSingle("yazıyorsan", matchesTailLex("Verb + Prog1 + Cond + A2sg"));
    t.expectSingle("yazıyormuş", matchesTailLex("Verb + Prog1 + Narr + A3sg"));
    t.expectSingle("yazıyorduk", matchesTailLex("Verb + Prog1 + Past + A1pl"));

    t.expectFail(
        "yazıyormuşsak"
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
  public void progressiveNegative2() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmıyordum", matchesTailLex("Verb + Neg + Prog1 + Past + A1sg"));
    t.expectSingle("yazmıyorsan", matchesTailLex("Verb + Neg + Prog1 + Cond + A2sg"));
    t.expectSingle("yazmıyormuş", matchesTailLex("Verb + Neg + Prog1 + Narr + A3sg"));
    t.expectSingle("yazmıyorduk", matchesTailLex("Verb + Neg + Prog1 + Past + A1pl"));

    t = getTester("aramak");

    t.expectSingle("aramıyorduk", matchesTailLex("Verb + Neg + Prog1 + Past + A1pl"));
    t.expectSingle("aramıyorsam", matchesTailLex("Verb + Neg + Prog1 + Cond + A1sg"));
    t.expectSingle("aramıyormuşuz", matchesTailLex("Verb + Neg + Prog1 + Narr + A1pl"));
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
  public void aorist2() {
    AnalysisTester t = getTester("yazmak"); // Aorist_A attribute is inferred.

    t.expectSingle("yazardım", matchesTailLex("Verb + Aor + Past + A1sg"));
    t.expectSingle("yazmazdım", matchesTailLex("Verb + Neg + Aor + Past + A1sg"));
    t.expectSingle("yazardık", matchesTailLex("Verb + Aor + Past + A1pl"));
    t.expectSingle("yazarmışsın", matchesTailLex("Verb + Aor + Narr + A2sg"));
    t.expectSingle("yazarsa", matchesTailLex("Verb + Aor + Cond + A3sg"));
    t.expectSingle("yazmazsa", matchesTailLex("Verb + Neg + Aor + Cond + A3sg"));
    t.expectSingle("yazmazmışız", matchesTailLex("Verb + Neg + Aor + Narr + A1pl"));

    t = getTester("etmek [A:Voicing]");
    t.expectSingle("eder", matchesTailLex("Verb + Aor + A3sg"));
    t.expectSingle("edermiş", matchesTailLex("Verb + Aor + Narr + A3sg"));
    t.expectSingle("etmezmiş", matchesTailLex("Verb + Neg + Aor + Narr + A3sg"));
    t.expectSingle("ederdik", matchesTailLex("Verb + Aor + Past + A1pl"));
    t.expectSingle("etmezsek", matchesTailLex("Verb + Neg + Aor + Cond + A1pl"));
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

  @Test
  public void abilityPositive() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazabil", matchesTailLex("Verb + Able + Verb + Imp + A2sg"));
    t.expectSingle("yazabiliyor", matchesTailLex("Verb + Able + Verb + Prog1 + A3sg"));

    t.expectFail(
        "yazabildir",
        "yazabilebil"
    );

    t = getTester("okumak");

    t.expectSingle("okuyabil", matchesTailLex("Verb + Able + Verb + Imp + A2sg"));
    t.expectSingle("okuyabilir", matchesTailLex("Verb + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void abilityAfterCausative() {

    AnalysisTester t = getTester("okumak");

    t.expectSingle("okutabil", matchesTailLex("Verb + Caus + Verb + Able + Verb + Imp + A2sg"));
    t.expectSingle("okutturabil",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Able + Verb + Imp + A2sg"));
  }

  @Test
  public void abilityNegative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazama", matchesTailLex("Verb + Able + Verb + Neg + Imp + A2sg"));
    t.expectSingle("yazamaz", matchesTailLex("Verb + Able + Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazamıyor", matchesTailLex("Verb + Able + Verb + Neg + Prog1 + A3sg"));

    t = getTester("okumak");

    t.expectSingle("okuyama", matchesTailLex("Verb + Able + Verb + Neg + Imp + A2sg"));
    t.expectSingle("okutmayabilir",
        matchesTailLex("Verb + Caus + Verb + Neg + Able + Verb + Aor + A3sg"));
    t.expectSingle("okutamayabilir",
        matchesTailLex("Verb + Caus + Verb + Able + Verb + Neg + Able + Verb + Aor + A3sg"));
    t.expectSingle("okuyamayabilir",
        matchesTailLex("Verb + Able + Verb + Neg + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void passive1() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıl", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("yazılıyor", matchesTailLex("Verb + Pass + Verb + Prog1 + A3sg"));
    t.expectSingle("yazdırılıyor",
        matchesTailLex("Verb + Caus + Verb + Pass + Verb + Prog1 + A3sg"));
    t.expectSingle("yazdırtılıyor",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Pass + Verb + Prog1 + A3sg"));

    t.expectFail(
        "yazınıyor",
        "yazınıl",
        "yazılınıl",
        "yazıldır",
        "yazdırınıyor",
        "yazdırıldır"
    );

    t = getTester("okumak");

    t.expectSingle("okun", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("okunul", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("okunulabilir", matchesTailLex("Verb + Pass + Verb + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void passive2() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazılmasın", matchesTailLex("Verb + Pass + Verb + Neg + Imp + A3sg"));
    t.expectSingle("yazılmıyor", matchesTailLex("Verb + Pass + Verb + Neg + Prog1 + A3sg"));
    t.expectSingle("yazdırılmıyor",
        matchesTailLex("Verb + Caus + Verb + Pass + Verb + Neg + Prog1 + A3sg"));
    t.expectSingle("yazdırtılmıyor",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Pass + Verb + Neg + Prog1 + A3sg"));

    t.expectFail(
        "yazınmıyor",
        "yazınılma",
        "yazılınılma",
        "yazıldırma",
        "yazdırınmıyor",
        "yazdırıldırma"
    );

    t = getTester("okumak");

    t.expectSingle("okunmayın", matchesTailLex("Verb + Pass + Verb + Neg + Imp + A2pl"));
    t.expectSingle("okunulmasın", matchesTailLex("Verb + Pass + Verb + Neg + Imp + A3sg"));
    t.expectSingle("okunulmayabilir",
        matchesTailLex("Verb + Pass + Verb + Neg + Able + Verb + Aor + A3sg"));
    t.expectSingle("okunulamayabilir",
        matchesTailLex("Verb + Pass + Verb + Able + Verb + Neg + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void past() {
    AnalysisTester t = getTester("yazmak"); 

    t.expectSingle("yazdım", matchesTailLex("Verb + Past + A1sg"));
    t.expectSingle("yazmadım", matchesTailLex("Verb + Neg + Past + A1sg"));
    t.expectSingle("yazdık", matchesTailLex("Verb + Past + A1pl"));
    t.expectSingle("yazmadık", matchesTailLex("Verb + Neg + Past + A1pl"));
    t.expectSingle("yazdıysan", matchesTailLex("Verb + Past + Cond + A2sg"));
    t.expectSingle("yazmadıysan", matchesTailLex("Verb + Neg + Past + Cond + A2sg"));


    t = getTester("etmek [A:Voicing]");
    t.expectSingle("etti", matchesTailLex("Verb + Past + A3sg"));
    t.expectSingle("etmedi", matchesTailLex("Verb + Neg + Past + A3sg"));
    t.expectSingle("ettik", matchesTailLex("Verb + Past + A1pl"));
    t.expectSingle("etmedik", matchesTailLex("Verb + Neg + Past + A1pl"));
  }

  @Test
  public void narrative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmışım", matchesTailLex("Verb + Narr + A1sg"));
    t.expectSingle("yazmamışım", matchesTailLex("Verb + Neg + Narr + A1sg"));
    t.expectSingle("yazmışız", matchesTailLex("Verb + Narr + A1pl"));
    t.expectSingle("yazmamışız", matchesTailLex("Verb + Neg + Narr + A1pl"));
    t.expectSingle("yazmışsan", matchesTailLex("Verb + Narr + Cond + A2sg"));
    t.expectSingle("yazmamışsan", matchesTailLex("Verb + Neg + Narr + Cond + A2sg"));


    t = getTester("etmek [A:Voicing]");
    t.expectSingle("etmiş", matchesTailLex("Verb + Narr + A3sg"));
    t.expectSingle("etmemiş", matchesTailLex("Verb + Neg + Narr + A3sg"));
    t.expectSingle("etmişiz", matchesTailLex("Verb + Narr + A1pl"));
    t.expectSingle("etmemişiz", matchesTailLex("Verb + Neg + Narr + A1pl"));
  }


}
