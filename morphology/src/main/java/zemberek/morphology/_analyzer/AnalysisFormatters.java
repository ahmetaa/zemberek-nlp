package zemberek.morphology._analyzer;

import java.util.stream.Collectors;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeSurface;

public class AnalysisFormatters {

  public static AnalysisFormatter lexicalForm() {
    return analysis -> String.join(" + ", analysis.getMorphemes().stream()
        .map(s -> s.morpheme.id)
        .collect(Collectors.toList()));
  }

  public static AnalysisFormatter shortForm() {
    return analysis ->
        String.join(" + ", analysis.getMorphemes().stream()
            .map(MorphemeSurface::toMorphemeString)
            .collect(Collectors.toList()));
  }

  static class OflazerFormatter implements AnalysisFormatter {

    @Override
    public String format(_SingleAnalysis analysis) {
      return null;
    }
  }

}
