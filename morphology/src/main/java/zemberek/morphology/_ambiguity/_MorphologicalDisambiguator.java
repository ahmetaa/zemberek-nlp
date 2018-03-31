package zemberek.morphology._ambiguity;

import zemberek.morphology._analyzer._SentenceAnalysis;

public interface _MorphologicalDisambiguator {

  void disambiguate(_SentenceAnalysis sentenceParse);

}
