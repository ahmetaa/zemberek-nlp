package zemberek.morphology._analyzer;

import zemberek.morphology._morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.RootLexicon;

public class _TurkishMorphologicalAnalyzer {

  RootLexicon lexicon;
  TurkishMorphotactics morphotactics;
  InterpretingAnalyzer analyzer;
  _UnidentifiedTokenAnalyzer unidentifiedTokenAnalyzer;

  public _TurkishMorphologicalAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    morphotactics = new TurkishMorphotactics(lexicon);
    analyzer = new InterpretingAnalyzer(lexicon);
    unidentifiedTokenAnalyzer = new _UnidentifiedTokenAnalyzer(analyzer);
  }



}
