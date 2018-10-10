package zemberek.morphology.analysis;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

// This class contains all morphological analyses of a word.
public class WordAnalysis implements Iterable<SingleAnalysis> {

  public static final WordAnalysis EMPTY_INPUT_RESULT =
      new WordAnalysis("", Collections.emptyList());

  //this is actual input.
  String input;
  // this is the input that is prepared for analysis.
  String normalizedInput;

  List<SingleAnalysis> analysisResults;

  public WordAnalysis(String input, List<SingleAnalysis> analysisResults) {
    this.input = input;
    this.normalizedInput = input;
    this.analysisResults = analysisResults;
  }

  public WordAnalysis(String input, String normalizedInput, List<SingleAnalysis> analysisResults) {
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
    return analysisResults.size() > 0 && !analysisResults.get(0).isUnknown();
  }

  public int analysisCount() {
    return analysisResults.size();
  }

  @Override
  public Iterator<SingleAnalysis> iterator() {
    return analysisResults.iterator();
  }

  public Stream<SingleAnalysis> stream() {
    return analysisResults.stream();
  }

  public WordAnalysis copyFor(List<SingleAnalysis> analyses) {
    return new WordAnalysis(this.input, this.normalizedInput, analyses);
  }

  public List<SingleAnalysis> getAnalysisResults() {
    return analysisResults;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WordAnalysis analyses = (WordAnalysis) o;

    if (!input.equals(analyses.input)) {
      return false;
    }
    if (!normalizedInput.equals(analyses.normalizedInput)) {
      return false;
    }
    return analysisResults.equals(analyses.analysisResults);
  }

  @Override
  public int hashCode() {
    int result = input.hashCode();
    result = 31 * result + normalizedInput.hashCode();
    result = 31 * result + analysisResults.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "WordAnalysis{" +
        "input='" + input + '\'' +
        ", normalizedInput='" + normalizedInput + '\'' +
        ", analysisResults=" + analysisResults +
        '}';
  }
}
