package zemberek.morphology._analyzer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import zemberek.core.logging.Log;
import zemberek.morphology._morphotactics.StemTransition;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * Hold stem->root-suffix transitions.
 * Such as
 * elma->Noun
 * kitab->Noun
 * kitap->Noun
 * oku->Verb
 */
public class StemTransitions {

  // TODO: this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems =
      ArrayListMultimap.create(1000, 2);
  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();
  private Set<StemTransition> stemTransitions = Sets.newConcurrentHashSet();

  //TODO: check the lock mechanism
  private ReadWriteLock lock = new ReentrantReadWriteLock();
  private StemTransitionGenerator generator;

  RootLexicon lexicon;

  public StemTransitions(RootLexicon lexicon, StemTransitionGenerator generator) {
    this.lexicon = lexicon;
    this.generator = generator;
    generateStemTransitions();
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  private void generateStemTransitions() {
    for (DictionaryItem item : lexicon) {
      addDictionaryItem(item);
    }
  }

  private synchronized void addStemTransition(StemTransition stemTransition) {
    lock.writeLock().lock();
    try {
      final String surfaceForm = stemTransition.surface;
      if (multiStems.containsKey(surfaceForm)) {
        multiStems.put(surfaceForm, stemTransition);
      } else if (singleStems.containsKey(surfaceForm)) {
        multiStems.put(surfaceForm, singleStems.get(surfaceForm));
        singleStems.remove(surfaceForm);
        multiStems.put(surfaceForm, stemTransition);
      } else {
        singleStems.put(surfaceForm, stemTransition);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private synchronized void removeStemNode(StemTransition stemTransition) {
    lock.writeLock().lock();
    try {
      final String surfaceForm = stemTransition.surface;
      if (multiStems.containsKey(surfaceForm)) {
        multiStems.remove(surfaceForm, stemTransition);
      } else if (singleStems.containsKey(surfaceForm)
          && singleStems.get(surfaceForm).item.equals(stemTransition.item)) {
        singleStems.remove(surfaceForm);
      }
      stemTransitions.remove(stemTransition);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public List<StemTransition> getMatchingStemTransitions(String stem) {
    if (singleStems.containsKey(stem)) {
      return Lists.newArrayList(singleStems.get(stem));
    } else if (multiStems.containsKey(stem)) {
      return Lists.newArrayList(multiStems.get(stem));
    } else {
      return Collections.emptyList();
    }
  }

  public void addDictionaryItem(DictionaryItem item) {
    try {
      List<StemTransition> transitions = generator.generate(item);
      for (StemTransition transition : transitions) {
        addStemTransition(transition);
      }
    } catch (Exception e) {
      Log.warn("Cannot generate stem transition for %s with reason %s", item, e.getMessage());
    }
  }

  public void removeDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generator.generate(item);
      for (StemTransition transition : transitions) {
        removeStemNode(transition);
      }
    } catch (Exception e) {
      Log.warn("Cannot remove %s ", e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }


}
