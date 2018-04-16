package zemberek.morphology.ambiguity;

import java.util.List;
import zemberek.morphology._analyzer.SentenceAnalysis;
import zemberek.morphology._analyzer.WordAnalysis;

public interface AmbiguityResolver {

  SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses);

}
