package zemberek.morphology._analyzer;

import java.util.List;


// This class contains all morphological analyses of a word.
// TODO: Not Yet In Use
public class _WordAnalysis {

  static class Input {
    //this is actual input.
    String input;
    // this is the input that is prepared for analysis.
    String normalizedInput;
    // This shows location of the word if used in a sentence or other context.
    int index;

    public Input(String input, String normalizedInput, int index) {
      this.input = input;
      this.normalizedInput = normalizedInput;
      this.index = index;
    }
  }

  Input input;
  List<_SingleAnalysis> analysisResults;

  public _WordAnalysis(Input input, List<_SingleAnalysis> analysisResults) {
    this.input = input;
    this.analysisResults = analysisResults;
  }
}
