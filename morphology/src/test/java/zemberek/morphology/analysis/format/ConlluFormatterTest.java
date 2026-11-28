package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;

public class ConlluFormatterTest {

  private static TurkishMorphology defaultMorphology;

  @BeforeClass
  public static void setUpClass() {
    defaultMorphology = TurkishMorphology.createWithDefaults();
  }

  private static SingleAnalysis findAnalysis(String word, String substring) {
    return defaultMorphology.analyze(word).getAnalysisResults().stream()
        .filter(a -> a.formatLong().contains(substring))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No analysis containing " + substring + " for " + word));
  }

  @Test
  public void lexicalVsGrammaticalIgWalkProducesExactLemmaAndDerivedFrom() {
    ConlluFormatter fmt = ConlluFormatter.bounDefault();

    // 'gerçekleştirilen': gerçek (Adj) -> gerçekleş (Become: lexical) -> Caus -> Pass -> PresPart (grammatical)
    SingleAnalysis gerceklestirilen = findAnalysis("gerçekleştirilen", "[gerçek:Adj]");
    UdFeatureMapper.ProjectedToken proj = UdFeatureMapper.projectFull(
        gerceklestirilen, "gerçekleştirilen", ConlluFormatter.Dialect.UD_TURKIC_BOUN);
    Assert.assertEquals("gerçekleş", proj.lemma());
    Assert.assertEquals("VERB", proj.upos());
    Assert.assertEquals("Ptcp", proj.xpos());
    Assert.assertEquals("gerçek", proj.derivedFrom());
    Assert.assertEquals("Polarity=Pos|Tense=Pres|VerbForm=Part|Voice=CauPass", proj.feats());

    // 'öğretmenlik': öğretmen -> öğretmenlik (Ness: lexical, unvoiced k), DerivedFrom=öğretmen
    SingleAnalysis ogretmenlik = findAnalysis("öğretmenlik", "Ness→Noun");
    UdFeatureMapper.ProjectedToken pNoun = UdFeatureMapper.projectFull(
        ogretmenlik, "öğretmenlik", ConlluFormatter.Dialect.UD_TURKIC_BOUN);
    Assert.assertEquals("öğretmenlik", pNoun.lemma());
    Assert.assertEquals("NOUN", pNoun.upos());
    Assert.assertEquals("öğretmen", pNoun.derivedFrom());
    Assert.assertEquals("Case=Nom|Number=Sing|Person=3", pNoun.feats());

    // Stacked moods: gelebilse -> Mood=CndPot, gelmelidir -> Mood=GenNec
    SingleAnalysis gelebilse = findAnalysis("gelebilse", "Able→Verb+se:Desr");
    Assert.assertTrue(fmt.format(gelebilse).contains("Mood=DesPot"));

    SingleAnalysis gelmelidir = findAnalysis("gelmelidir", "meli:Neces");
    Assert.assertTrue(fmt.format(gelmelidir).contains("Mood=GenNec"));
  }

  @Test
  public void questionParticleDeğilImekAndExistentialsMatchBounAndImstConventions() {
    ConlluFormatter boun = ConlluFormatter.bounDefault();
    ConlluFormatter imst = ConlluFormatter.imstDefault();

    // Harmonic 'mı' and 'mıydı' canonicalize lemma to 'mi' with full AUX/Ques inflectional features
    SingleAnalysis bareMi = findAnalysis("mı", "[mı:Ques]");
    Assert.assertEquals(
        "mi\tAUX\tQues\tAspect=Imp|Number=Sing|Person=3|Tense=Pres",
        boun.format(bareMi));
    Assert.assertEquals(
        "mi\tAUX\tQues\tAspect=Perf|Mood=Ind|Number=Sing|Person=3|Tense=Pres",
        imst.format(bareMi));

    SingleAnalysis miydi = findAnalysis("mıydı", "[mı:Ques]");
    Assert.assertEquals(
        "mi\tAUX\tQues\tAspect=Perf|Evident=Fh|Number=Sing|Person=3|Tense=Past",
        boun.format(miydi));

    // Focus clitic 'de'/'da' -> PART/Emph in BOUN vs CCONJ/Conj in IMST
    SingleAnalysis focusDe = findAnalysis("de", "[de:Conj]");
    Assert.assertEquals("de\tPART\tEmph\t_", boun.format(focusDe));
    Assert.assertEquals("de\tCCONJ\tConj\t_", imst.format(focusDe));

    // Existential 'var' -> NOUN/Exist with Polarity=Pos in BOUN vs ADJ/Adj in IMST
    SingleAnalysis varAdj = findAnalysis("var", "[var:Adj]");
    Assert.assertEquals("var\tNOUN\tExist\tNumber=Sing|Person=3|Polarity=Pos", boun.format(varAdj));
    Assert.assertEquals("var\tADJ\tAdj\t_", imst.format(varAdj));

    // Inflected numeral '1990'da' stays UPOS=NUM with XPOS=NNum and Case=Loc
    SingleAnalysis numLoc = findAnalysis("1990'da", "Num");
    Assert.assertEquals(
        "1990\tNUM\tNNum\tCase=Loc|Number=Sing|NumType=Card|Person=3",
        boun.format(numLoc));

    // Full infinitive '-mAk' omits Number=Sing|Person=3 in BOUN while '-mA' retains nominal agreement
    SingleAnalysis saglamak = findAnalysis("sağlamak", "Inf1→Noun");
    Assert.assertEquals(
        "sağla\tVERB\tVnoun\tCase=Nom|Polarity=Pos|VerbForm=Vnoun",
        boun.format(saglamak));

    // Substantivized adjective ('güzeller') -> NOUN in BOUN vs ADJ/NAdj in IMST
    SingleAnalysis guzeller = findAnalysis("güzeller", "[güzel:Adj] güzel:Adj|Zero→Noun+ler:A3pl");
    Assert.assertEquals("güzel\tNOUN\t_\tCase=Nom|Number=Plur|Person=3", boun.format(guzeller));
    Assert.assertEquals("güzel\tADJ\tNAdj\tCase=Nom|Number=Plur|Person=3", imst.format(guzeller));

    // 'değildir' -> AUX, Overt, Mood=Gen, Polarity=Neg
    SingleAnalysis degildir = findAnalysis("değildir", "[değil:Verb]");
    Assert.assertEquals(
        "değil\tAUX\tOvert\tMood=Gen|Number=Sing|Person=3|Polarity=Neg|Tense=Pres",
        boun.format(degildir));

    // 'idi' -> lemma 'i', AUX, Overt
    SingleAnalysis idi = findAnalysis("idi", "[imek:Verb]");
    Assert.assertEquals(
        "i\tAUX\tOvert\tAspect=Perf|Evident=Fh|Number=Sing|Person=3|Polarity=Pos|Tense=Past",
        boun.format(idi));

    // Feature ordering: Number comes strictly before Number[psor], Person before Person[psor]
    SingleAnalysis hedeflerim = findAnalysis("hedeflerim", "[hedef:Noun]");
    Assert.assertEquals(
        "hedef\tNOUN\t_\tCase=Nom|Number=Plur|Number[psor]=Sing|Person=3|Person[psor]=1",
        boun.format(hedeflerim));
  }

  @Test
  public void autonomousMwtSplittingAndConlluStructureHoldOnSentences() {
    ConlluFormatter fmt = ConlluFormatter.builder()
        .dialect(ConlluFormatter.Dialect.UD_TURKIC_BOUN)
        .mwtMode(ConlluFormatter.MwtMode.COPULA_AND_KI)
        .includeDerivedFrom(true)
        .includeZemberekInMisc(true)
        .build();

    String text = "Evdeki öğrenciler İstanbul'daydı, göreve başlıyorlarmış.";
    SentenceAnalysis sa = defaultMorphology.analyzeAndDisambiguate(text);
    String conllu = fmt.formatSentence("test_1", sa);

    String[] lines = conllu.split("\n");
    Assert.assertEquals("# sent_id = test_1", lines[0]);
    Assert.assertEquals("# text = " + text, lines[1]);

    List<String[]> rows = new ArrayList<>();
    for (String line : lines) {
      if (!line.startsWith("#") && !line.isEmpty()) {
        String[] cols = line.split("\t", -1);
        Assert.assertEquals("Every CoNLL-U row must have exactly 10 columns: " + line, 10, cols.length);
        rows.add(cols);
      }
    }

    // 1-2 Evdeki -> 1 Evde + 2 ki
    Assert.assertEquals("1-2", rows.get(0)[0]);
    Assert.assertEquals("Evdeki", rows.get(0)[1]);
    Assert.assertEquals("1", rows.get(1)[0]);
    Assert.assertEquals("Evde", rows.get(1)[1]);
    Assert.assertEquals("Loc", extractFeat(rows.get(1)[5], "Case"));
    Assert.assertEquals("2", rows.get(2)[0]);
    Assert.assertEquals("ki", rows.get(2)[1]);
    Assert.assertEquals("PART", rows.get(2)[3]);

    // İstanbul'daydı -> MWT with SpaceAfter=No on the MWT range header (because ',' follows immediately),
    // and '_' in MISC on both sub-tokens per strict UD CoNLL-U v2 specification!
    String[] istMwt = rows.stream().filter(r -> r[1].equals("İstanbul'daydı")).findFirst().orElseThrow();
    Assert.assertTrue(istMwt[0].contains("-"));
    Assert.assertTrue("MWT header before comma must carry SpaceAfter=No", istMwt[9].contains("SpaceAfter=No"));

    String[] istHost = rows.stream().filter(r -> r[1].equals("İstanbul'da")).findFirst().orElseThrow();
    Assert.assertEquals("İstanbul", istHost[2]);
    Assert.assertEquals("PROPN", istHost[3]);
    Assert.assertEquals("_", istHost[9]);

    // başlıyorlarmış -> başlıyorlar + mış (with SpaceAfter=No on MWT header before '.')
    String[] basMwt = rows.stream().filter(r -> r[1].equals("başlıyorlarmış")).findFirst().orElseThrow();
    Assert.assertTrue(basMwt[0].contains("-"));
    Assert.assertTrue(basMwt[9].contains("SpaceAfter=No"));
    Assert.assertFalse("Pipe in Zemberek MISC must be escaped",
        basMwt[9].contains("|abil:") || basMwt[9].contains("|Zero→"));

    String[] basHost = rows.stream().filter(r -> r[1].equals("başlıyorlar")).findFirst().orElseThrow();
    Assert.assertEquals("başla", basHost[2]);
    Assert.assertEquals("VERB", basHost[3]);

    String[] basAux = rows.stream().filter(r -> r[1].equals("mış")).findFirst().orElseThrow();
    Assert.assertEquals("AUX", basAux[3]);
    Assert.assertEquals("Zero", basAux[4]);
    Assert.assertTrue(basAux[5].contains("Evident=Nfh"));

    // Inflected pronominal -ki ('seninkiyle') splits into genitive host + inflected PRON 'kiyle'
    SentenceAnalysis kiSa = defaultMorphology.analyzeAndDisambiguate("Seninkiyle değiştirdik.");
    String kiConllu = fmt.formatSentence("test_2", kiSa);
    Assert.assertTrue(kiConllu.contains("1-2\tSeninkiyle\t"));
    Assert.assertTrue(kiConllu.contains("\tSenin\tsen\tPRON\tPers\tCase=Gen|Number=Sing|Person=2|PronType=Prs\t"));
    Assert.assertTrue(kiConllu.contains("\tkiyle\tki\tPRON\tRel\tCase=Ins|Number=Sing|Person=3\t"));

    // Nominal/adjectival copular converb '-yken' ('varken') splits into existential host + AUX 'ken'
    SentenceAnalysis varkenSa = defaultMorphology.analyzeAndDisambiguate("Zaman varken çalışalım.");
    String varkenConllu = fmt.formatSentence("test_3", varkenSa);
    Assert.assertTrue(varkenConllu.contains("2-3\tvarken\t"));
    Assert.assertTrue(varkenConllu.contains("\tvar\tvar\tNOUN\tExist\tNumber=Sing|Person=3|Polarity=Pos\t"));
    Assert.assertTrue(varkenConllu.contains("\tken\ti\tAUX\tZero\tVerbForm=Conv\t"));
  }

  @Test
  public void conlluFormatterSatisfiesStrictUdV2InvariantsAcrossDiverseCorpus() {
    String[] sentences = {
      "Fakülteyi bitirenler en uçtan göreve başlıyorlarmış.",
      "Eşeklerin sırtlarına yüklenmiş sepetlerle taşınırdı üzümler.",
      "Ankara'da ve İstanbul'daydı; TBMM'de 15'inci madde görüşülecekmiş!",
      "Gerçekleştirilen projelerdeki eksiklikler giderilemediğinden başvurular ertelendi.",
      "Bu kitaplarımızdan birini seninkiyle değiştirebilir miyiz?",
      "Yarın hava güzel olursa geliverir, olmazsa evde oturup çalışırız.",
      "Böyle bir şey ne görülmüştür ne de duyulmuştur, değil miydi?",
      "Birbirimize güvenmeden, konuşmaksızın ve anlaşamadan ilerleyemeyiz.",
      "Kendi kendine söylenirken oldukça yorgun görünüyordun.",
      "2026'da %12,5'lik artışla 3'er paket dağıtılacağı açıklandı.",
      "QuasarXyz123 bilinmeyen kelimesi de cümlede vardı, yoktur demeyin."
    };
    ConlluFormatter[] modes = {
      ConlluFormatter.builder()
          .dialect(ConlluFormatter.Dialect.UD_TURKIC_BOUN)
          .mwtMode(ConlluFormatter.MwtMode.COPULA_AND_KI)
          .includeDerivedFrom(true)
          .includeZemberekInMisc(true)
          .build(),
      ConlluFormatter.imstDefault(),
      ConlluFormatter.singleToken()
    };
    for (int sIdx = 0; sIdx < sentences.length; sIdx++) {
      SentenceAnalysis sa = defaultMorphology.analyzeAndDisambiguate(sentences[sIdx]);
      for (ConlluFormatter cf : modes) {
        validateConlluBlock(cf.formatSentence("s_" + (sIdx + 1), sa), sentences[sIdx]);
      }
    }
  }

  private static void validateConlluBlock(String conllu, String expectedText) {
    String[] lines = conllu.split("\n");
    Assert.assertTrue(lines[0].startsWith("# sent_id = "));
    Assert.assertEquals("# text = " + expectedText, lines[1]);

    int expectedNextId = 1;
    int mwtEnd = -1;
    for (int i = 2; i < lines.length; i++) {
      String line = lines[i];
      String[] cols = line.split("\t", -1);
      Assert.assertEquals("Row must have 10 columns: " + line, 10, cols.length);
      for (String col : cols) {
        Assert.assertFalse("Empty column not allowed in CoNLL-U: " + line, col.isEmpty());
      }
      String id = cols[0];
      if (id.contains("-")) {
        String[] parts = id.split("-");
        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);
        Assert.assertEquals(expectedNextId, start);
        Assert.assertTrue(end > start);
        for (int c = 2; c <= 8; c++) {
          Assert.assertEquals("_", cols[c]);
        }
        mwtEnd = end;
      } else {
        int tokId = Integer.parseInt(id);
        Assert.assertEquals(expectedNextId++, tokId);
        if (tokId <= mwtEnd) {
          Assert.assertEquals("_", cols[9]);
        }
        String feats = cols[5];
        if (!"_".equals(feats)) {
          String prevKey = null;
          for (String kv : feats.split("\\|")) {
            int eq = kv.indexOf('=');
            Assert.assertTrue(eq > 0 && eq < kv.length() - 1);
            String key = kv.substring(0, eq);
            if (prevKey != null) {
              Assert.assertTrue(UdFeatureMapper.CONLLU_FEAT_KEY_ORDER.compare(prevKey, key) < 0);
            }
            prevKey = key;
          }
        }
        String misc = cols[9];
        if (!"_".equals(misc)) {
          for (String mEntry : misc.split("\\|")) {
            Assert.assertTrue(mEntry.indexOf('=') > 0);
          }
        }
      }
    }
  }

  private static String extractFeat(String feats, String key) {
    for (String kv : feats.split("\\|")) {
      if (kv.startsWith(key + "=")) {
        return kv.substring(key.length() + 1);
      }
    }
    return null;
  }

  @Test
  public void structuredProjectionMatchesFormatSentenceAndRoundTripsWithDependencies() {
    ConlluFormatter boun = ConlluFormatter.bounDefault();
    String text = "Evdeki çocuklar İstanbul'daydı.";
    SentenceAnalysis sa = defaultMorphology.analyzeAndDisambiguate(text);

    ConlluSentence projected = boun.projectSentence("s-1", sa);
    Assert.assertEquals("s-1", projected.sentId());
    Assert.assertEquals(text, projected.text());
    Assert.assertEquals(boun.formatSentence("s-1", sa), projected.toConllu());

    // Both "Evdeki" (evde + ki) and "İstanbul'daydı" (İstanbul'da + ydı) produce MWT spans
    Assert.assertFalse("Expected MWT spans for bound -ki and copula", projected.mwtSpans().isEmpty());
    Assert.assertEquals(
        List.of("Evdeki", "çocuklar", "İstanbul'daydı", "."),
        projected.surfaceForms());

    // All projected tokens start with unassigned head
    for (ConlluToken tok : projected.tokens()) {
      Assert.assertFalse(tok.hasHead());
      Assert.assertEquals(ConlluToken.UNASSIGNED_HEAD, tok.head());
      Assert.assertEquals("_", tok.deprel());
    }

    // Attach synthetic dependency heads/relations and verify round-trip parse
    int n = projected.size();
    int[] heads = new int[n];
    String[] deprels = new String[n];
    for (int i = 0; i < n; i++) {
      heads[i] = (i == n - 2) ? 0 : (n - 1);
      deprels[i] = (i == n - 2) ? "root" : "dep";
    }
    ConlluSentence parsedDeps = projected.withDependencies(heads, deprels);
    String serialized = parsedDeps.toConllu();
    List<ConlluSentence> reparsed = ConlluSentence.parseAll(serialized);
    Assert.assertEquals(1, reparsed.size());
    Assert.assertEquals(serialized, reparsed.getFirst().toConllu());
    Assert.assertEquals(parsedDeps.tokens().size(), reparsed.getFirst().tokens().size());
    Assert.assertEquals(parsedDeps.mwtSpans().size(), reparsed.getFirst().mwtSpans().size());
    Assert.assertTrue(reparsed.getFirst().tokenById(1).hasHead());
    Assert.assertFalse(reparsed.getFirst().tokenById(1).featPairs().isEmpty());
  }

  @Test
  public void conlluParserHandlesCommentsMwtAndSkipsEmptyNodes() {
    String sample =
        "# sent_id = boun_1\n"
            + "# text = Evdeydi.\n"
            + "1-2\tEvdeydi\t_\t_\t_\t_\t_\t_\t_\tSpaceAfter=No\n"
            + "1\tEvde\tev\tNOUN\t_\tCase=Loc|Number=Sing|Person=3\t0\troot\t_\t_\n"
            + "2\tydi\ti\tAUX\tZero\tAspect=Perf|Evident=Fh|Number=Sing|Person=3|Polarity=Pos|Tense=Past\t1\tcop\t_\t_\n"
            + "2.1\t_\tol\tVERB\t_\t_\t_\t_\t0:root\t_\n"
            + "3\t.\t.\tPUNCT\tPunc\t_\t1\tpunct\t_\t_\n";

    List<ConlluSentence> parsed = ConlluSentence.parseAll(sample);
    Assert.assertEquals(1, parsed.size());
    ConlluSentence s = parsed.getFirst();
    Assert.assertEquals("boun_1", s.sentId());
    Assert.assertEquals("Evdeydi.", s.text());
    Assert.assertEquals(3, s.size());
    Assert.assertEquals(1, s.mwtSpans().size());
    Assert.assertEquals(1, s.mwtSpans().getFirst().startId());
    Assert.assertEquals(2, s.mwtSpans().getFirst().endId());
    Assert.assertEquals(0, s.tokenById(1).head());
    Assert.assertEquals("root", s.tokenById(1).deprel());
    Assert.assertEquals(1, s.tokenById(2).head());
    Assert.assertEquals("cop", s.tokenById(2).deprel());
    Assert.assertEquals(List.of("Case=Loc", "Number=Sing", "Person=3"), s.tokenById(1).featPairs());
    Assert.assertEquals(List.of("Evdeydi", "."), s.surfaceForms());
  }
}
