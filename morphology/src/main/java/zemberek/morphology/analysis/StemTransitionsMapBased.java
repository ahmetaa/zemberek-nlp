package zemberek.morphology.analysis;

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
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class StemTransitionsMapBased extends StemTransitionsBase implements StemTransitions {

  // TODO: this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems =
      ArrayListMultimap.create(1000, 2);
  private ArrayListMultimap<DictionaryItem, StemTransition> dictionaryMap =
      ArrayListMultimap.create(1000, 1);

  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();
  private Set<StemTransition> stemTransitions = Sets.newConcurrentHashSet();

  //TODO: check the lock mechanism
  private ReadWriteLock lock = new ReentrantReadWriteLock();

  RootLexicon lexicon;

  public StemTransitionsMapBased(RootLexicon lexicon, TurkishMorphotactics morphotactics) {
    this.lexicon = lexicon;
    this.morphotactics = morphotactics;
    generateStemTransitions();
  }

  public Set<StemTransition> getTransitions() {
    return stemTransitions;
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
      stemTransitions.add(stemTransition);
      if (!dictionaryMap.containsEntry(stemTransition.item, stemTransition)) {
        dictionaryMap.put(stemTransition.item, stemTransition);
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
      if (!dictionaryMap.containsEntry(stemTransition.item, stemTransition)) {
        dictionaryMap.remove(stemTransition.item, stemTransition);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private List<StemTransition> getTransitions(String stem) {
    if (singleStems.containsKey(stem)) {
      return Lists.newArrayList(singleStems.get(stem));
    } else if (multiStems.containsKey(stem)) {
      return Lists.newArrayList(multiStems.get(stem));
    } else {
      return Collections.emptyList();
    }
  }

  public List<StemTransition> getPrefixMatches(String input) {
    List<StemTransition> matches = Lists.newArrayListWithCapacity(3);
    for (int i = 1; i <= input.length(); i++) {
      String stem = input.substring(0, i);
      matches.addAll(getTransitions(stem));
    }
    return matches;
  }

  public List<StemTransition> getTransitions(DictionaryItem item) {
    if (dictionaryMap.containsKey(item)) {
      return dictionaryMap.get(item);
    } else {
      return Collections.emptyList();
    }
  }

  public void addDictionaryItem(DictionaryItem item) {
    try {
      List<StemTransition> transitions = generate(item);
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
      List<StemTransition> transitions = generate(item);
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

