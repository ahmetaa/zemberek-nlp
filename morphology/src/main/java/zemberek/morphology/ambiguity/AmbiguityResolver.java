package zemberek.morphology.ambiguity;

import java.util.List;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public interface AmbiguityResolver {

  SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses);

}
