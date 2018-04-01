package zemberek.morphology.ambiguity;

import java.util.ArrayList;
import java.util.List;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;

// Used for collecting training data.

public class RuleBasedDisambiguator {

  _TurkishMorphologicalAnalyzer analyzer;

  public RuleBasedDisambiguator(_TurkishMorphologicalAnalyzer analyzer) {
    this.analyzer = analyzer;
  }

  public ResultSentence disambiguate(String sentence) {
    _SentenceAnalysis ambigious = analyzer.analyzeSentence(sentence);
    ResultSentence s = new ResultSentence(sentence, ambigious);
    s.makeDecisions();
    return s;
  }

  static class ResultSentence {

    String sentence;
    _SentenceAnalysis sentenceAnalysis;
    List<AmbiguityAnalysis> results;

    public ResultSentence(String sentence, _SentenceAnalysis sentenceAnalysis) {
      this.sentence = sentence;
      this.sentenceAnalysis = sentenceAnalysis;
      results = new ArrayList<>();
      for (_WordAnalysis analysis : sentenceAnalysis) {
        results.add(new AmbiguityAnalysis(analysis));
      }
    }

    public void makeDecisions() {
      // TODO: do nothing for now.
      for (int i = 0; i < results.size(); i++) {
      }
    }
  }

  public static class AmbigiousWord {

    AmbiguityAnalysis previous;
    AmbiguityAnalysis current;
    AmbiguityAnalysis next;

    public AmbigiousWord(_WordAnalysis previous, _WordAnalysis current, _WordAnalysis next) {
      this.previous = new AmbiguityAnalysis(previous);
      this.current = new AmbiguityAnalysis(current);
      this.next = new AmbiguityAnalysis(next);
    }
  }

  public static class AmbiguityAnalysis {

    String input;
    List<AnalysisDecision> choices = new ArrayList<>();

    AmbiguityAnalysis(_WordAnalysis wordAnalysis) {
      this.input = wordAnalysis.getInput();
      for (_SingleAnalysis analysis : wordAnalysis) {
        choices.add(new AnalysisDecision(input, analysis, Decision.UNDECIDED));
      }
    }

  }

  enum Decision {
    IGNORE,
    SELECT,
    UNDECIDED
  }

  public static class AnalysisDecision {

    String input;
    _SingleAnalysis analysis;

    Decision decision;

    AnalysisDecision(String input, _SingleAnalysis analysis, Decision decision) {
      this.input = input;
      this.analysis = analysis;
      this.decision = decision;
    }
  }

}
