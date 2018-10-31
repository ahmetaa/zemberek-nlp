package zemberek.examples.morphology;

import java.io.IOException;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * This is example for morphological analysis that ignores diacritics marks. So, for input `kisi`
 * analysis will include `kişi` and `kışı`.
 */
public class AnalyzeIgnoreDiacritics {

  public static void main(String[] args) throws IOException {

    TurkishMorphology morphology = TurkishMorphology.builder()
        .ignoreDiacriticsInAnalysis()
        .setLexicon(RootLexicon.getDefault())
        .build();

    morphology.analyze("kisi").forEach(System.out::println);
  }

}
