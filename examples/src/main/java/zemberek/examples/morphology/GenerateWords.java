package zemberek.examples.morphology;

import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;

public class GenerateWords {

  public static void main(String[] args) {

    String[] number = {"A3sg", "A3pl"};
    String[] possessives = {"P1sg", "P2sg", "P3sg"};
    String[] cases = {"Dat", "Loc", "Abl"};

    TurkishMorphology morphology =
        TurkishMorphology.builder().setLexicon("armut").disableCache().build();

    DictionaryItem item = morphology.getLexicon().getMatchingItems("armut").get(0);
    for (String numberM : number) {
      for (String possessiveM : possessives) {
        for (String caseM : cases) {
          List<Result> results =
              morphology.getWordGenerator().generate(item, numberM, possessiveM, caseM);
          results.forEach(s->System.out.println(s.surface));
        }
      }
    }
  }

}
