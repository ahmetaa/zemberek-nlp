package normalization;

import java.io.IOException;
import zemberek.morphology._analyzer._TurkishMorphology;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.normalization.TurkishSpellChecker;

public class SpellingExamples {

  public static void main(String[] args) throws IOException {
    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();
    TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

    System.out.println("Check if written correctly.");
    String[] words = {"Ankara'ya", "Ankar'aya", "yapbileceksen", "yapabileceğinizden"};
    for (String word : words) {
      System.out.println(word + " -> " + spellChecker.check(word));
    }
    System.out.println();
    System.out.println("Give suggestions.");
    String[] toSuggest = {"Kraamanda", "okumuştk", "yapbileceksen", "oukyamıyorum"};
    for (String s : toSuggest) {
      System.out.println(s + " -> " + spellChecker.suggestForWord(s));
    }
  }
}
