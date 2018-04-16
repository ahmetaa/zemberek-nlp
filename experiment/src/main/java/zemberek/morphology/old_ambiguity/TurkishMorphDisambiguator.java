package zemberek.morphology.old_ambiguity;

import zemberek.morphology.old_analysis.SentenceAnalysis;

public interface TurkishMorphDisambiguator {

  void disambiguate(SentenceAnalysis sentenceParse);
}
