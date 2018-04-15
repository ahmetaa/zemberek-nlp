package zemberek.morphology._analyzer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

// This class contains all morphological analyses of a word.
public class _WordAnalysis implements Iterable<_SingleAnalysis> {

  static final _WordAnalysis EMPTY_INPUT_RESULT =
      new _WordAnalysis("", Collections.emptyList());

  //this is actual input.
  String input;
  // this is the input that is prepared for analysis.
  String normalizedInput;

  List<_SingleAnalysis> analysisResults;

  public _WordAnalysis(String input, List<_SingleAnalysis> analysisResults) {
    this.input = input;
    this.normalizedInput = input;
    this.analysisResults = analysisResults;
  }

  public _WordAnalysis(String input, String normalizedInput,
      List<_SingleAnalysis> analysisResults) {
    this.input = input;
    this.normalizedInput = normalizedInput;
    this.analysisResults = analysisResults;
  }

  public String getInput() {
    return input;
  }

  public String getNormalizedInput() {
    return normalizedInput;
  }

  public boolean isCorrect() {
    return analysisResults.size() > 0;
  }

  public int analysisCount() {
    return analysisResults.size();
  }

  @Override
  public Iterator<_SingleAnalysis> iterator() {
    return analysisResults.iterator();
  }

  public Stream<_SingleAnalysis> stream() {
    return analysisResults.stream();
  }

  public _WordAnalysis copyFor(List<_SingleAnalysis> analyses) {
    return new _WordAnalysis(this.input, this.normalizedInput, analyses);
  }

  public List<_SingleAnalysis> getAnalysisResults() {
    return analysisResults;
  }
}
