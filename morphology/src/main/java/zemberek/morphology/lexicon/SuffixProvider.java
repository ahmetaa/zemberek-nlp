package zemberek.morphology.lexicon;

import zemberek.morphology.lexicon.graph.SuffixData;

public interface SuffixProvider {

    SuffixForm getSuffixFormById(String suffixId);

    Iterable<SuffixForm> getAllForms();

    SuffixData[] defineSuccessorSuffixes(DictionaryItem item);

    SuffixForm getRootSet(DictionaryItem item, SuffixData successors);

}
