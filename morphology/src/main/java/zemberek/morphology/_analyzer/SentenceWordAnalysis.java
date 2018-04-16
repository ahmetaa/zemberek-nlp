package zemberek.morphology._analyzer;

public class SentenceWordAnalysis {
  SingleAnalysis analysis;
  WordAnalysis wordAnalysis;

  public SentenceWordAnalysis(SingleAnalysis analysis,
      WordAnalysis wordAnalysis) {
    this.analysis = analysis;
    this.wordAnalysis = wordAnalysis;
  }

  public SingleAnalysis getAnalysis() {
    return analysis;
  }

  public WordAnalysis getWordAnalysis() {
    return wordAnalysis;
  }
}
