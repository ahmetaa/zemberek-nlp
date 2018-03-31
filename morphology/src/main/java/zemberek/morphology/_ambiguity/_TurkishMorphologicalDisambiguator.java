package zemberek.morphology._ambiguity;

import zemberek.morphology._analyzer._SentenceAnalysis;

public interface _TurkishMorphologicalDisambiguator {

  void disambiguate(_SentenceAnalysis sentenceParse);

}
