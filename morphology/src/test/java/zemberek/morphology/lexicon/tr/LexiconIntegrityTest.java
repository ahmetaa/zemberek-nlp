package zemberek.morphology.lexicon.tr;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

public class LexiconIntegrityTest {

  private static boolean hasValidLexicalAnalysis(TurkishMorphology morph, String word) {
    WordAnalysis wa = morph.analyze(word);
    if (wa == null || wa.analysisCount() == 0) {
      return false;
    }
    for (SingleAnalysis sa : wa) {
      if (!sa.isUnknown() && !sa.isRuntime()) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void allDefaultLexiconItemsShouldSelfParse() {
    TurkishMorphology morph = TurkishMorphology.createWithDefaults();
    RootLexicon lexicon = morph.getLexicon();
    Assert.assertTrue("Default lexicon looks unexpectedly small", lexicon.size() > 67_000);

    List<String> failures = new ArrayList<>();
    for (DictionaryItem item : lexicon) {
      if (item.hasAttribute(RootAttribute.Dummy)) {
        continue;
      }
      WordAnalysis wa = morph.analyze(item.lemma);
      boolean ok = false;
      for (SingleAnalysis sa : wa) {
        if (!sa.isUnknown()) {
          ok = true;
          break;
        }
      }
      if (!ok) {
        failures.add(item.id + "(" + item.lemma + ")");
      }
    }
    Assert.assertTrue(
        "Items in default lexicon failed to analyze their own lemma: " + failures,
        failures.isEmpty());
  }

  @Test
  public void pr386AndModernVocabularyRegressionsShouldPass() {
    TurkishMorphology morph = TurkishMorphology.createWithDefaults();

    String[] mustParseLexically = {
        "bollar", "bolca", "bolluk",
        "semptomatiği", "probiyotiği", "fotoniği",
        "vlog", "vlogu",
        "API", "api", "API'ye", "api'ye",
        "WIFI", "wifi", "WIFI'ın", "WIFI'da",
        "IBAN", "iban", "IBAN'a",
        "GIF", "gif", "PIN", "pin", "SIM", "sim", "HIV", "HIV'in",
        "COVID", "covid", "COVID'İN", "BİST'te", "BIST'te", "BIST'İN",
        "TOGG", "TOGG'un", "TOGG'a",
        "SSH'te", "SSH'ten",
        "e-posta", "e-postaya", "Hint-Avrupa",
        "Netflix'te", "Ajax'ta",
        "bebişimiz", "tatlışım", "ghosting", "fintek", "blokzincir", "kriptopara"
    };
    for (String form : mustParseLexically) {
      Assert.assertTrue(
          "Expected valid lexical parse for: " + form,
          hasValidLexicalAnalysis(morph, form));
    }

    // Open TDK compounds must remain separate so spellcheck/normalizer can split glued typos
    String[] gluedOpenCompounds = {
        "yapayzeka", "sosyalmedya", "akıllıtelefon", "filtrekahve", "yoğunbakım", "şehirlerarası"
    };
    for (String glued : gluedOpenCompounds) {
      Assert.assertFalse(
          "Open compound should not be accepted as a closed dictionary word: " + glued,
          hasValidLexicalAnalysis(morph, glued));
    }
  }

  @Test
  public void corpusMinedModernLexiconAndBimodalAcronymsShouldPass() {
    TurkishMorphology morph = TurkishMorphology.createWithDefaults();

    String[] mustParseLexically = {
        // PR 2: Foreign Pr: fixes & bimodal acronyms + Abbreviation -lIk / P3sg
        "Trump'ın", "Biden'ın", "Musk'ın", "Google'ın", "Apple'ın", "YouTube'da", "Stockholm'ün",
        "TSK'nın", "TSK'nin", "SGK'ya", "SGK'ye", "TDK'ya", "TDK'ye", "KHK'nin", "KHK'nın",
        "BMGK'nin", "BMGK'nın", "TL'lik", "km'lik", "kg'lık", "CEO'su", "GSYH'si", "OHAL'in",
        "MİT'in", "MİT'ten", "MIT'de", "MIT'NİN", "BRICS'e", "FIA'nın", "MRI'ın", "FIFA'nın", "HDMI'ın", "Universiade'da",
        // PR 3: Modern/scientific vocabulary & attribute fixes
        "müsilajın", "filyasyonu", "megavatsaatlik", "gigavatlık", "emojisi", "oligarkların",
        "maruziyeti", "motokuryelerin", "pnömoniye", "sepsise", "kuarklar", "fotonların",
        "mühimmatları", "icraatları", "mevzuatları", "tedariki", "tedariği", "aktivisti", "implantı",
        "hackerlar", "single'ı", "blockchainde", "kombinleri", "kilovatsaati", "megabaytı", "minimalistler",
        // PR 4: 2020-2026 entities, acronyms, hyphenated places, Tier-A names
        "ChatGPT'nin", "OpenAI'nin", "OpenAI'ın", "iPhone'un", "TikTok'ta", "BioNTech'in",
        "Zelenskiy'nin", "Zelenskiy'in", "Blinken'ın", "Blinken'in", "Starmer'ın", "Meloni'nin",
        "AİHM'in", "AİHM'nin", "GSYİH'nın", "GSYİH'nin", "AKTS'lik", "DW'ye", "AfD'nin",
        "Bosna-Hersek'te", "Hakkari'de", "Tahran'da", "Venezuela'nın", "Kahramankazan'da",
        "Ali", "Ali'ye", "Ali'nin", "Salih'in", "Ecrin'in", "Elif'in", "Kerem'in"
    };
    for (String form : mustParseLexically) {
      Assert.assertTrue(
          "Expected valid lexical parse for: " + form,
          hasValidLexicalAnalysis(morph, form));
    }

    // Glued apostrophe-dropped ghosts removed from proper-from-corpus.dict must not parse as bare lemmas
    String[] removedGluedGhosts = {
        "Angelesta", "Angelesın", "Googleın", "Forbesun", "Manchesterın"
    };
    for (String ghost : removedGluedGhosts) {
      Assert.assertFalse(
          "Glued apostrophe ghost should not remain in lexicon: " + ghost,
          morph.getLexicon().getMatchingItems(ghost.toLowerCase()).stream()
              .anyMatch(i -> i.lemma.equalsIgnoreCase(ghost)));
    }

    // Roman numerals must not be shadowed by Abbrv entries and must resolve as RomanNumeral
    for (String roman : new String[]{"II.", "II'nin", "III'te", "IV'ün", "VI'nın", "VII'nin"}) {
      Assert.assertTrue(
          "Expected RomanNumeral analysis for: " + roman,
          morph.analyze(roman).stream()
              .anyMatch(a -> a.getDictionaryItem().secondaryPos == zemberek.core.turkish.SecondaryPos.RomanNumeral));
    }
    Assert.assertEquals("bosna-hersek", morph.analyze("Bosna-Hersek'te").getAnalysisResults().get(0).getStem());
    Assert.assertTrue(
        "Expected non-runtime ProperNoun analysis for bare Ali",
        morph.analyze("Ali").stream()
            .anyMatch(a -> !a.isRuntime()
                && a.getDictionaryItem().lemma.equals("Ali")
                && a.getDictionaryItem().secondaryPos == zemberek.core.turkish.SecondaryPos.ProperNoun));
    Assert.assertEquals(1, morph.analyze("Hakkari'de").analysisCount());
    Assert.assertEquals(1, morph.analyze("BİST'te").analysisCount());
    Assert.assertTrue(morph.analyze("MİT'ten").stream().anyMatch(
        a -> a.getDictionaryItem().secondaryPos == zemberek.core.turkish.SecondaryPos.Abbreviation));
    Assert.assertFalse(hasValidLexicalAnalysis(morph, "Universiade'de"));
 
    for (String singleItem : new String[] {"BTK", "TSK", "SGK", "CIA", "AI", "AİHM", "GSYİH", "OpenAI", "Arsenal", "Reuters"}) {
      long lexicalCount = morph.analyze(singleItem).getAnalysisResults().stream()
          .filter(sa -> !sa.isUnknown() && !sa.isRuntime())
          .count();
      Assert.assertEquals("Expected single lexical analysis for bare " + singleItem, 1L, lexicalCount);
    }
  }
}
