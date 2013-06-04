package zemberek.morphology.ambiguity;

import zemberek.morphology.parser.SentenceMorphParse;

public interface TurkishMorphDisambiguator {
    void disambiguate(SentenceMorphParse sentenceParse);
}
