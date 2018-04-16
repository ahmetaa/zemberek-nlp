package zemberek.morphology.old_lexicon;

import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.old_lexicon.graph.SuffixData;

public interface SuffixProvider {

  SuffixForm getSuffixFormById(String suffixId);

  Iterable<SuffixForm> getAllForms();

  SuffixData[] defineSuccessorSuffixes(DictionaryItem item);

  SuffixForm getRootSet(DictionaryItem item, SuffixData successors);

}
