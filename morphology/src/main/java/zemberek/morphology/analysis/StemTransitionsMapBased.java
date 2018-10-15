package zemberek.morphology.analysis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class StemTransitionsMapBased extends StemTransitionsBase implements StemTransitions {

  private ArrayListMultimap<String, StemTransition> multiStems =
      ArrayListMultimap.create(1000, 2);
  private HashMultimap<String, String> asciiKeys = null;

  // contains a map that holds dictionary items that has
  // multiple or different than item.root stem surface forms.
  private ArrayListMultimap<DictionaryItem, StemTransition> differentStemItems =
      ArrayListMultimap.create(1000, 2);

  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();

  private ReadWriteLock lock = new ReentrantReadWriteLock();

  public StemTransitionsMapBased(RootLexicon lexicon, TurkishMorphotactics morphotactics) {
    this.lexicon = lexicon;
    this.morphotactics = morphotactics;
    lexicon.forEach(this::addDictionaryItem);
  }

  //TODO: this is kind of a hack. Because StemTransitions may be shared between
  // analyzer classes, this may be necessary when one of them happens to be ascii tolerant
  // and other is not.
  private void generateAsciiTolerantMap() {
    lock.writeLock().lock();
    asciiKeys = HashMultimap.create(1000, 2);
    try {

      // generate MultiMap for ascii tolerant keys
      for (String s : singleStems.keySet()) {
        String ascii = TurkishAlphabet.INSTANCE.toAscii(s);

        if (TurkishAlphabet.INSTANCE.containsAsciiRelated(s)) {
          asciiKeys.put(ascii, s);
        }
      }
      for (StemTransition st : multiStems.values()) {
        String s = st.surface;
        String ascii = TurkishAlphabet.INSTANCE.toAscii(s);
        if (TurkishAlphabet.INSTANCE.containsAsciiRelated(s)) {
          asciiKeys.put(ascii, s);
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public synchronized Set<StemTransition> getTransitions() {
    HashSet<StemTransition> result = new HashSet<>(singleStems.values());
    result.addAll(multiStems.values());
    return result;
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  private synchronized void addStemTransition(StemTransition stemTransition) {
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
  }

  private synchronized void removeStemNode(StemTransition stemTransition) {
    final String surfaceForm = stemTransition.surface;
    if (multiStems.containsKey(surfaceForm)) {
      multiStems.remove(surfaceForm, stemTransition);
    } else if (singleStems.containsKey(surfaceForm)
        && singleStems.get(surfaceForm).item.equals(stemTransition.item)) {
      singleStems.remove(surfaceForm);
    }
    if (!differentStemItems.containsEntry(stemTransition.item, stemTransition)) {
      differentStemItems.remove(stemTransition.item, stemTransition);
    }
  }

  private LinkedHashSet<StemTransition> getTransitionsAsciiTolerant(String stem) {
    lock.readLock().lock();
    try {
      // add actual
      LinkedHashSet<StemTransition> result = new LinkedHashSet<>();
      if (singleStems.containsKey(stem)) {
        result.add(singleStems.get(stem));
      } else if (multiStems.containsKey(stem)) {
        result.addAll(multiStems.get(stem));
      }
      Set<String> asciiStems = asciiKeys.get(TurkishAlphabet.INSTANCE.toAscii(stem));
      for (String st : asciiStems) {
        if (singleStems.containsKey(st)) {
          result.add(singleStems.get(st));
        } else if (multiStems.containsKey(st)) {
          result.addAll(multiStems.get(st));
        }
      }
      return result;
    } finally {
      lock.readLock().unlock();
    }
  }

  private List<StemTransition> getTransitions(String stem) {
    lock.readLock().lock();
    try {
      if (singleStems.containsKey(stem)) {
        return Lists.newArrayList(singleStems.get(stem));
      } else if (multiStems.containsKey(stem)) {
        return Lists.newArrayList(multiStems.get(stem));
      } else {
        return Collections.emptyList();
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<StemTransition> getPrefixMatches(String input, boolean asciiTolerant) {
    if (asciiKeys == null && asciiTolerant) {
      generateAsciiTolerantMap();
    }
    lock.readLock().lock();
    try {
      List<StemTransition> matches = Lists.newArrayListWithCapacity(3);
      for (int i = 1; i <= input.length(); i++) {
        String stem = input.substring(0, i);
        if (asciiTolerant) {
          matches.addAll(getTransitionsAsciiTolerant(stem));
        } else {
          matches.addAll(getTransitions(stem));
        }
      }
      return matches;
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<StemTransition> getTransitions(DictionaryItem item) {
    lock.readLock().lock();
    try {
      if (differentStemItems.containsKey(item)) {
        return differentStemItems.get(item);
      } else {

        List<StemTransition> transitions = getTransitions(item.root);
        return transitions.stream().filter(s -> s.item.equals(item)).collect(Collectors.toList());
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  public void addDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        addStemTransition(transition);
      }
      if (transitions.size() > 1 || (transitions.size() == 1 && !item.root
          .equals(transitions.get(0).surface))) {
        differentStemItems.putAll(item, transitions);
      }
    } catch (Exception e) {
      Log.warn("Cannot generate stem transition for %s with reason %s", item, e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void removeDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        removeStemNode(transition);
      }
      if (differentStemItems.containsKey(item)) {
        differentStemItems.removeAll(item);
      }
    } catch (Exception e) {
      Log.warn("Cannot remove %s ", item, e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

}

