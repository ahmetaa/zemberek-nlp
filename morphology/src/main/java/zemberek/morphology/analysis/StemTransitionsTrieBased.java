package zemberek.morphology.analysis;

import com.google.common.collect.ArrayListMultimap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import zemberek.core.collections.Trie;
import zemberek.core.logging.Log;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class StemTransitionsTrieBased extends StemTransitionsBase implements StemTransitions {

  private Trie<StemTransition> stemTransitionTrie = new Trie<>();

  // contains a map that holds dictionary items that has multiple or
  // different than item.root stem surface forms.
  private ArrayListMultimap<DictionaryItem, StemTransition> differentStemItems =
      ArrayListMultimap.create(1000, 2);

  private ReadWriteLock lock = new ReentrantReadWriteLock();

  public StemTransitionsTrieBased(RootLexicon lexicon, TurkishMorphotactics morphotactics) {
    this.lexicon = lexicon;
    this.morphotactics = morphotactics;
    lexicon.forEach(this::addTransitions);
  }

  public List<StemTransition> getTransitions() {
    lock.readLock().lock();
    try {
      return stemTransitionTrie.getAll();
    } finally {
      lock.readLock().unlock();
    }
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  public List<StemTransition> getPrefixMatches(String stem, boolean asciiTolerant) {
    lock.readLock().lock();
    try {
      return stemTransitionTrie.getPrefixMatchingItems(stem);
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<StemTransition> getTransitions(DictionaryItem item) {
    if (differentStemItems.containsKey(item)) {
      return differentStemItems.get(item);
    } else {
      List<StemTransition> transitions = stemTransitionTrie.getItems(item.root);
      return transitions.stream().filter(s -> s.item.equals(item)).collect(Collectors.toList());
    }
  }

  public void addDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      addTransitions(item);
    } catch (Exception e) {
      Log.warn("Cannot generate stem transition for %s with reason %s", item, e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void addTransitions(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        stemTransitionTrie.add(transition.surface, transition);
      }
      if (transitions.size() > 1 || (transitions.size() == 1 && !item.root
          .equals(transitions.get(0).surface))) {
        differentStemItems.putAll(item, transitions);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void removeDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        stemTransitionTrie.remove(transition.surface, transition);
      }
      if (differentStemItems.containsKey(item)) {
        differentStemItems.removeAll(item);
      }
    } catch (Exception e) {
      Log.warn("Cannot remove %s ", e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

}

