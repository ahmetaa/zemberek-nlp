package zemberek.morphology.ambiguity;

import zemberek.morphology._analyzer._SentenceAnalysis;

public interface _Disambiguator {

  _SentenceAnalysis disambiguate(String sentence);
}
