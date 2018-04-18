package morphology;

import java.util.List;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;

public class GenerateWords {

  public static void main(String[] args) {

    String[] possessives = {"P1sg", "P2sg", "P3sg", "P1pl", "P2pl", "P3pl"};
    String[] cases = {"Dat", "Loc", "Abl"};

    TurkishMorphology morphology =
        TurkishMorphology.builder().addDictionaryLines("armut").disableCache().build();

    DictionaryItem item = morphology.getLexicon().getMatchingItems("armut").get(0);

    for (String possessiveM : possessives) {
      for (String caseM : cases) {
        List<Result> results = morphology.getWordGenerator().generate(item, possessiveM, caseM);
        for (Result result : results) {
          Log.info("%s generated for [%s-%s] with analysis %s ",
              result.surface, possessiveM, caseM, result.analysis.formatLong());
        }
      }
    }
  }

}
