package zemberek.morphology.ambiguity;

import java.util.List;
import zemberek.morphology.analyzer.SentenceAnalysis;
import zemberek.morphology.analyzer.WordAnalysis;

public interface AmbiguityResolver {

  SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses);

}
