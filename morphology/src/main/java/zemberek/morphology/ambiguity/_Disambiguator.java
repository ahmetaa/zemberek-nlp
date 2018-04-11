package zemberek.morphology.ambiguity;

import java.util.List;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._WordAnalysis;

public interface _Disambiguator {

  _SentenceAnalysis disambiguate(List<_WordAnalysis> allAnalyses);
}
