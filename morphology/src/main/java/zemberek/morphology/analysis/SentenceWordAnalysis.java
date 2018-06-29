package zemberek.morphology.analysis;

public class SentenceWordAnalysis {
  SingleAnalysis bestAnalysis;
  WordAnalysis wordAnalysis;

  public SentenceWordAnalysis(SingleAnalysis bestAnalysis,
      WordAnalysis wordAnalysis) {
    this.bestAnalysis = bestAnalysis;
    this.wordAnalysis = wordAnalysis;
  }

  public SingleAnalysis getBestAnalysis() {
    return bestAnalysis;
  }

  public WordAnalysis getWordAnalysis() {
    return wordAnalysis;
  }
}
