package zemberek.morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.morphotactics.Morpheme;

/**
 * Used for converting informal morphological analysis results to formal analysis using mappings and
 * word generation.
 */
public class InformalAnalysisConverter {

  private WordGenerator generator;

  public InformalAnalysisConverter(WordGenerator generator) {
    this.generator = generator;
  }

  /**
   * Converts the input and it's analysis SingleAnalysis to formal surface form and SingleAnalysis
   * object.
   *
   * @return converted single analysis object and new surface form. If input does not contain any
   * informal morpheme, it returns input without any changes. If generation does not work, returns
   * null.
   */
  public WordGenerator.Result convert(String input, SingleAnalysis a) {
    if (!a.containsInformalMorpheme()) {
      return new Result(input, a);
    }
    List<Morpheme> formalMorphemes = toFormalMorphemeNames(a);
    List<WordGenerator.Result> generations =
        generator.generate(a.getDictionaryItem(), formalMorphemes);
    if (generations.size() > 0) {
      return generations.get(0);
    } else {
      return null;
    }
  }

  /**
   * Converts informal morphemes to formal morphemes.
   */
  public static List<Morpheme> toFormalMorphemeNames(SingleAnalysis a) {
    List<Morpheme> transform = new ArrayList<>();
    for (Morpheme m : a.getMorphemes()) {
      if (m.informal && m.mappedMorpheme != null) {
        transform.add(m.mappedMorpheme);
      } else {
        transform.add(m);
      }
    }
    return transform;
  }
}
