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
}
