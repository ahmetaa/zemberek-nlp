package zemberek.morphology.analyzer;

import java.util.List;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.GraphVisitor;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * This is a primitive analyzer.
 */
public class InterpretingAnalyzer {

  RootLexicon lexicon;

  GraphVisitor graphVisitor;

  TurkishMorphotactics morphotactics = new TurkishMorphotactics();

  public InterpretingAnalyzer(RootLexicon lexicon,
      GraphVisitor graphVisitor) {
    this.lexicon = lexicon;
    this.graphVisitor = graphVisitor;
  }

  List<AnalysisResult> analyze(String input) {

    return null;
  }


}
