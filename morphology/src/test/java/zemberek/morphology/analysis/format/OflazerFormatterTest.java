package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;

public class OflazerFormatterTest {

  private static TurkishMorphology morphology;

  @BeforeClass
  public static void setUpClass() {
    morphology = TurkishMorphology.builder()
        .disableCache()
        .setLexicon(
            "kitap",
            "ağız [A:LastVowelDrop]",
            "gitmek [A:Voicing]",
            "aramak",
            "okumak",
            "olmak",
            "ağlamak",
            "başlamak",
            "değil [P:Verb]",
            "imek",
            "Ankara",
            "TBMM [P:Abbrv]",
            "bu [P:Pron, Demons]",
            "ben [P:Pron, Pers]",
            "kendi [P:Pron, Reflex]",
            "herkes [P:Pron, Quant]",
            "masa",
            "erken [P:Adv]",
            "dün [P:Noun, Time]",
            "iki [P:Num, Card]",
            "ikişer [P:Num, Dist]",
            ". [P:Punc]")
        .build();
  }

  private static SingleAnalysis first(String word) {
    return morphology.analyze(word).getAnalysisResults().get(0);
  }

  private static SingleAnalysis findByDefaultContains(String word, String substring) {
    return morphology.analyze(word).getAnalysisResults().stream()
        .filter(a -> a.formatLong().contains(substring))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No analysis matching " + substring + " for " + word));
  }

  @Test
  public void unmutatedRootAndZeroNominalSlotsAreReconstructed() {
    OflazerFormatter fmt = OflazerFormatter.metuSabanci();

    Assert.assertEquals("kitap+Noun+A3sg+Pnon+Nom", fmt.format(first("kitap")));
    // Voiced surface stem 'kitab' must restore lemma 'kitap' and insert +Pnon
    Assert.assertEquals("kitap+Noun+A3sg+Pnon+Dat", fmt.format(first("kitaba")));
    Assert.assertEquals("kitap+Noun+A3pl+Pnon+Loc", fmt.format(first("kitaplarda")));
    // Vowel-dropped surface stem 'ağz' must restore lemma 'ağız' and insert +Nom
    Assert.assertEquals("ağız+Noun+A3sg+P3sg+Nom",
        fmt.format(findByDefaultContains("ağzı", "P3sg")));

    // Multi-IG derivation reconstructs +Pnon+Nom on BOTH nominal IGs
    Assert.assertEquals(
        "kitap+Noun+A3sg+Pnon+Nom^DB+Adj+Without",
        fmt.format(findByDefaultContains("kitapsız", "Without→Adj")));
    Assert.assertEquals(
        "kitap+Noun+A3sg+Pnon+Nom^DB+Noun+Dim+A3sg+Pnon+Nom",
        fmt.format(findByDefaultContains("kitapçık", "Dim→Noun")));
  }

  @Test
  public void verbalPolarityUnableAndVoiceChainsMatchOflazer() {
    OflazerFormatter sak = OflazerFormatter.sak2008();

    // Voiced 'gid' and vowel-dropped 'ar' restore verb roots and emit +Pos
    Assert.assertEquals("git+Verb+Pos+Prog1+A3sg", sak.format(first("gidiyor")));
    Assert.assertEquals("ara+Verb+Pos+Prog1+A3sg", sak.format(first("arıyor")));

    // Voice derivation does not emit redundant +Pos before ^DB+Verb+Caus, only before TAM
    Assert.assertEquals(
        "oku+Verb^DB+Verb+Caus+Pos+Imp+A2sg",
        sak.format(findByDefaultContains("okut", "Caus→Verb+Imp+A2sg")));
    Assert.assertEquals(
        "oku+Verb^DB+Verb+Caus+Neg+Past+A3sg",
        sak.format(findByDefaultContains("okutmadı", "Caus→Verb+ma:Neg+dı:Past")));

    // Infinitive emits +Pos before ^DB+Noun+Inf1 and completes nominal +A3sg+Pnon+Nom
    Assert.assertEquals(
        "git+Verb+Pos^DB+Noun+Inf1+A3sg+Pnon+Nom",
        sak.format(findByDefaultContains("gitmek", "Inf1→Noun")));

    // Unable expands to ^DB+Verb+Able+Neg
    Assert.assertEquals(
        "oku+Verb^DB+Verb+Able+Neg+Prog1+A1sg",
        sak.format(first("okuyamıyorum")));
    Assert.assertEquals(
        "oku+Verb^DB+Verb+Able+Neg^DB+Adv+WithoutHavingDoneSo",
        sak.format(first("okuyamadan")));

    // 'değil' omits explicit +Neg in Sak/Oflazer and normalizes +Cop+A3sg order
    Assert.assertEquals(
        "değil+Verb+Pres+Cop+A3sg",
        sak.format(findByDefaultContains("değildir", "değil:Verb")));
  }

  @Test
  public void dialectsHandleCompoundTensesParticiplesPronounsAndCasing() {
    OflazerFormatter metu = OflazerFormatter.metuSabanci();
    OflazerFormatter sak = OflazerFormatter.sak2008();
    OflazerFormatter ofl94 = OflazerFormatter.oflazer94();
    OflazerFormatter starlang = OflazerFormatter.starlangUpper();

    // Proper noun & abbreviation casing across dialects
    SingleAnalysis ankarada = findByDefaultContains("Ankara'da", "[Ankara:Noun,Prop]");
    Assert.assertEquals("Ankara+Noun+Prop+A3sg+Pnon+Loc", metu.format(ankarada));
    Assert.assertEquals("ankara+Noun+Prop+A3sg+Pnon+Loc", sak.format(ankarada));
    Assert.assertEquals("Ankara+NOUN+PROP+A3SG+PNON+LOC", starlang.format(ankarada));

    SingleAnalysis tbmmde = findByDefaultContains("TBMM'de", "[TBMM:Noun,Abbrv]");
    Assert.assertEquals("TBMM+Noun+Prop+A3sg+Pnon+Loc", metu.format(tbmmde));

    // Pronoun & distributive numeral secondary POS tags
    Assert.assertEquals("bu+Pron+DemonsP+A3sg+Pnon+Acc",
        metu.format(findByDefaultContains("bunu", "[bu:Pron,Demons]")));
    Assert.assertEquals("ben+Pron+PersP+A1sg+Pnon+Dat",
        metu.format(findByDefaultContains("bana", "[ben:Pron,Pers]")));
    Assert.assertEquals("ikişer+Num+Distrib",
        metu.format(findByDefaultContains("ikişer", "[ikişer:Num,Dist]")));

    // Genitive -ki: Pron in Zemberek -> Noun+Rel in METU-Sabancı
    Assert.assertEquals(
        "masa+Noun+A3sg+Pnon+Gen^DB+Noun+Rel+A3sg+Pnon+Nom",
        metu.format(findByDefaultContains("masanınki", "Rel→Pron")));

    // Substantivized participle: ol+Verb^DB+Noun+PresPart+A3pl -> Adj+PresPart^DB+Noun+Zero
    SingleAnalysis olanlar = findByDefaultContains("olanlar", "PresPart→Noun+lar:A3pl");
    Assert.assertEquals(
        "ol+Verb+Pos^DB+Adj+PresPart^DB+Noun+Zero+A3pl+Pnon+Nom",
        metu.format(olanlar));
    Assert.assertEquals(
        "ol+Verb+Pos^DB+Noun+PresPart+A3pl+Pnon+Nom",
        sak.format(olanlar));

    // Compound finite verb: split in METU-Sabancı vs flat in Sak-2008
    SingleAnalysis aglayacakti = findByDefaultContains("ağlayacaktı", "Fut+tı:Past");
    Assert.assertEquals(
        "ağla+Verb+Pos+Fut^DB+Verb+Zero+Past+A3sg",
        metu.format(aglayacakti));
    Assert.assertEquals(
        "ağla+Verb+Pos+Fut+Past+A3sg",
        sak.format(aglayacakti));

    SingleAnalysis basliyorlarmis = findByDefaultContains("başlıyorlarmış", "Prog1+lar:A3pl+mış:Narr");
    Assert.assertEquals(
        "başla+Verb+Pos+Prog1+A3pl^DB+Verb+Zero+Narr+A3sg",
        metu.format(basliyorlarmis));

    // Adverb label in Oflazer-94 vs METU-Sabancı / Sak
    SingleAnalysis erken = first("erken");
    Assert.assertEquals("erken+Adverb", ofl94.format(erken));
    Assert.assertEquals("erken+Adv", metu.format(erken));

    // Punctuation and Unknown
    Assert.assertEquals(".+Punc", metu.format(first(".")));
    Assert.assertEquals("xyzqw+?", metu.format(SingleAnalysis.unknown("xyzqw")));
  }

  @Test
  public void allDialectsSatisfyStructuralInvariantsAcrossDiverseSentences() {
    TurkishMorphology fullMorph = TurkishMorphology.createWithDefaults();
    OflazerFormatter[] dialects = {
      OflazerFormatter.oflazer94(),
      OflazerFormatter.metuSabanci(),
      OflazerFormatter.sak2008(),
      OflazerFormatter.starlangUpper()
    };
    String[] sentences = {
      "Fakülteyi bitirenler en uçtan göreve başlıyorlarmış.",
      "Ankara'da ve İstanbul'daydı; TBMM'de 15'inci madde görüşülecekmiş!",
      "Gerçekleştirilen projelerdeki eksiklikler giderilemediğinden başvurular ertelendi.",
      "Bu kitaplarımızdan birini seninkiyle değiştirebilir miyiz?",
      "Birbirimize güvenmeden, konuşmaksızın ve anlaşamadan ilerleyemeyiz.",
      "QuasarXyz123 bilinmeyen kelimesi de cümlede vardı, yoktur demeyin."
    };
    for (String sentence : sentences) {
      SentenceAnalysis sa = fullMorph.analyzeAndDisambiguate(sentence);
      for (SentenceWordAnalysis swa : sa) {
        List<SingleAnalysis> candidates = new ArrayList<>(swa.getWordAnalysis().getAnalysisResults());
        candidates.add(swa.getBestAnalysis());
        for (SingleAnalysis cand : candidates) {
          for (OflazerFormatter of : dialects) {
            String res = of.format(cand);
            Assert.assertFalse("Empty Oflazer output for " + cand, res.isEmpty());
            Assert.assertFalse("Double '+' in Oflazer output: " + res,
                !cand.getDictionaryItem().lemma.equals("+") && res.contains("++"));
            Assert.assertFalse("Trailing '+' in Oflazer output: " + res, res.endsWith("+"));
          }
        }
      }
    }
  }
}
