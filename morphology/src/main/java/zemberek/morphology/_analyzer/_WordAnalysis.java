package zemberek.morphology._analyzer;

import java.util.List;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology._morphotactics.Morpheme;


// This class represents the morphological analysis of a single word.
// TODO: Not Yet In Use
public class _WordAnalysis {

  //this is actual input.
  String input;

  // this is the input that is prepared for analysis.
  String normalizedInput;

  List<_SingleAnalysis> analysisResults;

  // This shows location of the word if used in a sentence or other context.
  int index;


}
