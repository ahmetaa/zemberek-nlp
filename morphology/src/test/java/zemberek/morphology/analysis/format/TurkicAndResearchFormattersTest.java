package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class TurkicAndResearchFormattersTest {

  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() {
    morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon(
            "kitap",
            "göndermek",
            "götürmek",
            "görmek",
            "itmek",
            "gitmek [A:Voicing]",
            "aramak",
            "gelmek",
            "okumak",
            "ev",
            "Ankara",
            "ben [P:Pron, Pers]",
            "mı [P:Ques]",
            ". [P:Punc]")
        .build();
  }

  private static SingleAnalysis first(String word) {
    return morphology.analyze(word).getAnalysisResults().get(0);
  }

  private static SingleAnalysis findAnalysis(String word, String substring) {
    return morphology.analyze(word).getAnalysisResults().stream()
        .filter(a -> a.formatLong().contains(substring))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No analysis matching " + substring + " for " + word));
  }

  @Test
  public void apertiumFormatterMatchesPanTurkicTagConventions() {
    Assert.assertEquals(
        "kitap<n><pl><px1pl><abl>",
        AnalysisFormatters.APERTIUM_TURKIC.format(first("kitaplarımızdan")));
    Assert.assertEquals(
        "Ankara<np><al><loc>",
        AnalysisFormatters.APERTIUM_TURKIC.format(findAnalysis("Ankara'da", "[Ankara:Noun,Prop]")));
    Assert.assertEquals(
        "gel<v><abil><neg><ifi><p3><pl>",
        AnalysisFormatters.APERTIUM_TURKIC.format(first("gelemediler")));
    Assert.assertEquals(
        "ben<prn><pers><p1><sg><dat>",
        AnalysisFormatters.APERTIUM_TURKIC.format(first("bana")));
    Assert.assertEquals("mi<qst>", AnalysisFormatters.APERTIUM_TURKIC.format(first("mı")));
    Assert.assertEquals(".<sent>", AnalysisFormatters.APERTIUM_TURKIC.format(first(".")));

    WordAnalysis unknown = morphology.analyze("xyzqw");
    Assert.assertEquals("^xyzqw/*xyzqw$", ApertiumFormatter.INSTANCE.formatWordStream(unknown));

    SentenceAnalysis sa = morphology.analyzeAndDisambiguate("Ankara'da kitaplarımızdan.");
    String stream = ApertiumFormatter.INSTANCE.formatSentenceStream(sa);
    Assert.assertTrue(stream.contains("^Ankara'da/Ankara<np><al><loc>$"));
    Assert.assertTrue(stream.contains("^kitaplarımızdan/kitap<n><pl><px1pl><abl>$"));
    Assert.assertTrue(stream.endsWith("^./.<sent>$"));
  }

  @Test
  public void uniMorphFormatterMatchesUpstreamUnimorphTurDataset() {
    // Matches exact rows in github.com/unimorph/tur:
    // göndermek	gönderirim	V;IND;PRS;HAB;1;SG;POS;DECL
    // götürmek	götürdüm	V;IND;PST;1;SG;POS;DECL
    // görmek	görmüyormuşsunuz	V;INFR;PRS;PROG;2;PL;NEG;DECL
    Assert.assertEquals(
        "göndermek\tgönderirim\tV;IND;PRS;HAB;1;SG;POS;DECL",
        AnalysisFormatters.UNIMORPH.format(first("gönderirim")));
    Assert.assertEquals(
        "götürmek\tgötürdüm\tV;IND;PST;1;SG;POS;DECL",
        AnalysisFormatters.UNIMORPH.format(first("götürdüm")));
    Assert.assertEquals(
        "görmek\tgörmüyormuşsunuz\tV;INFR;PRS;PROG;2;PL;NEG;DECL",
        AnalysisFormatters.UNIMORPH.format(first("görmüyormuşsunuz")));
    Assert.assertEquals(
        "itmek\titiyordun\tV;IND;PST;PROG;2;SG;POS;DECL",
        AnalysisFormatters.UNIMORPH.format(first("itiyordun")));

    // Nominal copula preserves singular noun ('ev') alongside plural copular subject ('ARGNO3P')
    Assert.assertEquals(
        "ev\tevdeydiler\tN;ARGNO3P;SG;PST",
        AnalysisFormatters.UNIMORPH.format(findAnalysis("evdeydiler", "Zero→Verb")));

    // Nominal order in unimorph/tur: N;CASE;NUM;POSS
    Assert.assertEquals(
        "kitap\tkitaplarımızdan\tN;ABL;PL;PSS1P",
        AnalysisFormatters.UNIMORPH.format(first("kitaplarımızdan")));
    Assert.assertEquals(
        "kitap\tkitabım\tN;NOM;SG;PSS1S",
        AnalysisFormatters.UNIMORPH.format(findAnalysis("kitabım", "P1sg")));

    // Non-finite participle
    Assert.assertEquals(
        "V.PTCP;PRS;POS;NOM;PL",
        UniMorphFormatter.BUNDLE_ONLY.format(findAnalysis("gelenler", "PresPart→Noun+ler:A3pl")));
  }

  @Test
  public void canonicalSegmentationAndLeipzigGlossingRestoreMutatedStems() {
    // Compare SURFACE_SEQUENCE ('kitab ımız dan') vs CANONICAL_SURFACE_SEQUENCE ('kitap ımız dan')
    SingleAnalysis kitabimizdan = first("kitabımızdan");
    Assert.assertEquals("kitab ımız dan", AnalysisFormatters.SURFACE_SEQUENCE.format(kitabimizdan));
    Assert.assertEquals("kitap ımız dan",
        AnalysisFormatters.CANONICAL_SURFACE_SEQUENCE.format(kitabimizdan));
    Assert.assertEquals("kitap + ımız + dan",
        SegmentedSurfaceFormatter.CANONICAL_PLUS.format(kitabimizdan));

    // Voiced 'gid' -> 'git' and vowel-dropped 'ar' -> 'ara'
    Assert.assertEquals("git iyor", AnalysisFormatters.CANONICAL_SURFACE_SEQUENCE.format(first("gidiyor")));
    Assert.assertEquals("ara ıyor", AnalysisFormatters.CANONICAL_SURFACE_SEQUENCE.format(first("arıyor")));

    // Leipzig two-tier interlinear gloss
    Assert.assertEquals(
        "kitab-ımız-dan\tkitap-POSS.1PL-ABL",
        AnalysisFormatters.LEIPZIG_INTERLINEAR.format(kitabimizdan));
    Assert.assertEquals(
        "gid-iyor\tgit-PROG",
        AnalysisFormatters.LEIPZIG_INTERLINEAR.format(first("gidiyor")));
    Assert.assertEquals(
        "oku-t\toku-CAUS.IMP.2SG",
        AnalysisFormatters.LEIPZIG_INTERLINEAR.format(
            findAnalysis("okut", "Caus→Verb+Imp+A2sg")));
  }

  @Test
  public void allTurkicAndResearchFormattersSatisfyInvariantsAcrossDiverseSentences() {
    TurkishMorphology fullMorph = TurkishMorphology.createWithDefaults();
    String[] sentences = {
      "Fakülteyi bitirenler en uçtan göreve başlıyorlarmış.",
      "Ankara'da ve İstanbul'daydı; TBMM'de 15'inci madde görüşülecekmiş!",
      "Gerçekleştirilen projelerdeki eksiklikler giderilemediğinden başvurular ertelendi.",
      "Bu kitaplarımızdan birini seninkiyle değiştirebilir miyiz?",
      "2026'da %12,5'lik artışla 3'er paket dağıtılacağı açıklandı.",
      "QuasarXyz123 bilinmeyen kelimesi de cümlede vardı, yoktur demeyin."
    };
    for (String sentence : sentences) {
      SentenceAnalysis sa = fullMorph.analyzeAndDisambiguate(sentence);
      String apStream = ApertiumFormatter.INSTANCE.formatSentenceStream(sa);
      Assert.assertTrue(apStream.startsWith("^") && apStream.endsWith("$"));

      for (SentenceWordAnalysis swa : sa) {
        List<SingleAnalysis> candidates = new ArrayList<>(swa.getWordAnalysis().getAnalysisResults());
        candidates.add(swa.getBestAnalysis());
        for (SingleAnalysis cand : candidates) {
          String ap = ApertiumFormatter.INSTANCE.format(cand);
          Assert.assertFalse(ap.isEmpty());
          if (!cand.isUnknown()) {
            Assert.assertTrue(ap.contains("<") && ap.endsWith(">"));
          }
          String[] umCols = UniMorphFormatter.TAB_ROW.format(cand).split("\t", -1);
          Assert.assertEquals(3, umCols.length);
          Assert.assertFalse(umCols[2].isEmpty());
          Assert.assertFalse(SegmentedSurfaceFormatter.CANONICAL_SPACE.format(cand).isEmpty());
          Assert.assertEquals(2, SegmentedSurfaceFormatter.LEIPZIG.format(cand).split("\t", -1).length);
        }
      }
    }
  }
}
