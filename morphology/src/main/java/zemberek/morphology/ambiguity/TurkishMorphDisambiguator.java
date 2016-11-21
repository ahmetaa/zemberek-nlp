package zemberek.morphology.ambiguity;

import zemberek.morphology.analysis.SentenceAnalysis;

public interface TurkishMorphDisambiguator {
    void disambiguate(SentenceAnalysis sentenceParse);
}
