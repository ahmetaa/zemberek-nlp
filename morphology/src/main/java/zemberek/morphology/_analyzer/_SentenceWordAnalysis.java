package zemberek.morphology._analyzer;

public class _SentenceWordAnalysis {
  _SingleAnalysis analysis;
  _WordAnalysis wordAnalysis;

  public _SentenceWordAnalysis(_SingleAnalysis analysis,
      _WordAnalysis wordAnalysis) {
    this.analysis = analysis;
    this.wordAnalysis = wordAnalysis;
  }

  public _SingleAnalysis getAnalysis() {
    return analysis;
  }

  public _WordAnalysis getWordAnalysis() {
    return wordAnalysis;
  }
}
