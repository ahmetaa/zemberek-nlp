package zemberek.morphology.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class StemTransitionsMapBased extends StemTransitionsBase implements StemTransitions {

  // Maps stem surface form to either a single StemTransition or a List<StemTransition>.
  private final Map<String, Object> stems;
  private volatile Map<String, Set<String>> asciiKeys = null;

  // Holds dictionary items that have multiple or different-than-item.root stem surface forms.
  private final Map<DictionaryItem, List<StemTransition>> differentStemItems;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private int maxStemLength = 0;

  public StemTransitionsMapBased(RootLexicon lexicon, TurkishMorphotactics morphotactics) {
    this.lexicon = lexicon;
    this.morphotactics = morphotactics;
    int initialCapacity = Math.max(64, lexicon.size() * 2);
    this.stems = new HashMap<>(initialCapacity);
    this.differentStemItems = new HashMap<>(Math.max(16, lexicon.size() / 8));
    lexicon.forEach(this::addDictionaryItem);
  }

  private void generateAsciiTolerantMap() {
    lock.writeLock().lock();
    try {
      if (asciiKeys != null) {
        return;
      }
      Map<String, Set<String>> map = new HashMap<>(Math.max(64, stems.size() / 2));
      for (String s : stems.keySet()) {
        addAsciiKey(map, s);
      }
      this.asciiKeys = map;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static void addAsciiKey(Map<String, Set<String>> map, String surface) {
    if (TurkishAlphabet.INSTANCE.containsAsciiRelated(surface)) {
      String ascii = TurkishAlphabet.INSTANCE.toAscii(surface);
      map.computeIfAbsent(ascii, k -> new HashSet<>(2)).add(surface);
    }
  }

  private static void removeAsciiKey(Map<String, Set<String>> map, String surface) {
    if (TurkishAlphabet.INSTANCE.containsAsciiRelated(surface)) {
      String ascii = TurkishAlphabet.INSTANCE.toAscii(surface);
      Set<String> set = map.get(ascii);
      if (set != null) {
        set.remove(surface);
        if (set.isEmpty()) {
          map.remove(ascii);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void addEntry(Object entry, Collection<StemTransition> out) {
    if (entry instanceof StemTransition) {
      out.add((StemTransition) entry);
    } else if (entry != null) {
      out.addAll((List<StemTransition>) entry);
    }
  }

  @Override
  public Set<StemTransition> getTransitions() {
    lock.readLock().lock();
    try {
      HashSet<StemTransition> result = new HashSet<>(stems.size() * 2);
      for (Object entry : stems.values()) {
        addEntry(entry, result);
      }
      return result;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public RootLexicon getLexicon() {
    return lexicon;
  }

  @SuppressWarnings("unchecked")
  private void addStemTransition(StemTransition stemTransition) {
    final String surfaceForm = stemTransition.surface;
    if (surfaceForm.length() > maxStemLength) {
      maxStemLength = surfaceForm.length();
    }
    Object existing = stems.get(surfaceForm);
    if (existing == null) {
      stems.put(surfaceForm, stemTransition);
    } else if (existing instanceof StemTransition) {
      List<StemTransition> list = new ArrayList<>(2);
      list.add((StemTransition) existing);
      list.add(stemTransition);
      stems.put(surfaceForm, list);
    } else {
      ((List<StemTransition>) existing).add(stemTransition);
    }
    Map<String, Set<String>> ascii = this.asciiKeys;
    if (ascii != null) {
      addAsciiKey(ascii, surfaceForm);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeStemNode(StemTransition stemTransition) {
    final String surfaceForm = stemTransition.surface;
    boolean stillPresent = false;
    Object existing = stems.get(surfaceForm);
    if (existing instanceof StemTransition) {
      if (((StemTransition) existing).item.equals(stemTransition.item)) {
        stems.remove(surfaceForm);
      } else {
        stillPresent = true;
      }
    } else if (existing != null) {
      List<StemTransition> multi = (List<StemTransition>) existing;
      multi.remove(stemTransition);
      if (multi.isEmpty()) {
        stems.remove(surfaceForm);
      } else if (multi.size() == 1) {
        stems.put(surfaceForm, multi.get(0));
        stillPresent = true;
      } else {
        stillPresent = true;
      }
    }
    List<StemTransition> diff = differentStemItems.get(stemTransition.item);
    if (diff != null) {
      diff.remove(stemTransition);
      if (diff.isEmpty()) {
        differentStemItems.remove(stemTransition.item);
      }
    }
    Map<String, Set<String>> ascii = this.asciiKeys;
    if (ascii != null && !stillPresent) {
      removeAsciiKey(ascii, surfaceForm);
    }
  }

  private void addDirect(String stem, Collection<StemTransition> out) {
    addEntry(stems.get(stem), out);
  }

  @Override
  public List<StemTransition> getPrefixMatches(String input, boolean asciiTolerant) {
    if (asciiTolerant && asciiKeys == null) {
      generateAsciiTolerantMap();
    }
    lock.readLock().lock();
    try {
      int limit = Math.min(input.length(), maxStemLength);
      if (limit == 0) {
        return new ArrayList<>(0);
      }
      List<StemTransition> matches = new ArrayList<>(4);
      if (!asciiTolerant) {
        for (int i = 1; i <= limit; i++) {
          Object entry = stems.get(input.substring(0, i));
          if (entry != null) {
            addEntry(entry, matches);
          }
        }
      } else {
        Map<String, Set<String>> asciiMap = this.asciiKeys;
        String asciiInput = TurkishAlphabet.INSTANCE.toAscii(input);
        LinkedHashSet<StemTransition> seen = null;
        for (int i = 1; i <= limit; i++) {
          String stem = input.substring(0, i);
          String ascii = asciiInput.substring(0, i);
          Set<String> asciiStems = asciiMap.get(ascii);
          if (asciiStems == null) {
            addDirect(stem, matches);
          } else {
            if (seen == null) {
              seen = new LinkedHashSet<>(4);
            } else {
              seen.clear();
            }
            addDirect(stem, seen);
            for (String st : asciiStems) {
              addDirect(st, seen);
            }
            matches.addAll(seen);
          }
        }
      }
      return matches;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<StemTransition> getTransitions(DictionaryItem item) {
    lock.readLock().lock();
    try {
      List<StemTransition> diff = differentStemItems.get(item);
      if (diff != null) {
        return new ArrayList<>(diff);
      }
      Object entry = stems.get(item.root);
      if (entry == null) {
        return Collections.emptyList();
      }
      if (entry instanceof StemTransition) {
        StemTransition single = (StemTransition) entry;
        return single.item.equals(item) ? List.of(single) : Collections.emptyList();
      }
      List<StemTransition> multi = (List<StemTransition>) entry;
      List<StemTransition> result = new ArrayList<>(2);
      for (int i = 0, n = multi.size(); i < n; i++) {
        StemTransition st = multi.get(i);
        if (st.item.equals(item)) {
          result.add(st);
        }
      }
      return result;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void addDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        addStemTransition(transition);
      }
      if (transitions.size() > 1 || (transitions.size() == 1 && !item.root
          .equals(transitions.get(0).surface))) {
        differentStemItems
            .computeIfAbsent(item, k -> new ArrayList<>(transitions.size()))
            .addAll(transitions);
      }
    } catch (Exception e) {
      Log.warn("Cannot generate stem transition for %s with reason %s", item, e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void removeDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        removeStemNode(transition);
      }
      differentStemItems.remove(item);
    } catch (Exception e) {
      Log.warn("Cannot remove %s ", item, e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

}
