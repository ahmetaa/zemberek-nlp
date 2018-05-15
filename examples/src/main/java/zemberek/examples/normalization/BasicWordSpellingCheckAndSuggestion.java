package zemberek.examples.normalization;

import java.io.IOException;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.TurkishSpellChecker;

public class BasicWordSpellingCheckAndSuggestion {

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

    Log.info("Check if written correctly.");
    String[] words = {"Ankara'ya", "Ankar'aya", "yapbileceksen", "yapabileceğinizden"};
    for (String word : words) {
      Log.info(word + " -> " + spellChecker.check(word));
    }
    Log.info();
    Log.info("Give suggestions.");
    String[] toSuggest = {"Kraamanda", "okumuştk", "yapbileceksen", "oukyamıyorum"};
    for (String s : toSuggest) {
      Log.info(s + " -> " + spellChecker.suggestForWord(s));
    }
  }
}
