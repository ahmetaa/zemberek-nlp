package zemberek.morphology.analysis;

import java.util.Collection;
import java.util.List;
import zemberek.core.text.StringMatcher;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.StemTransition;

public interface StemTransitions {

  Collection<StemTransition> getTransitions();

  RootLexicon getLexicon();

  List<StemTransition> getPrefixMatches(String stem);

  List<StemTransition> getPrefixMatches(String stem, StringMatcher matcher);

  List<StemTransition> getTransitions(DictionaryItem item);

  void addDictionaryItem(DictionaryItem item);

  void removeDictionaryItem(DictionaryItem item);

  List<StemTransition> generate(DictionaryItem item);

}
