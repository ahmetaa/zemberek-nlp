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

  private final Map<String, StemTransition> singleStems;
  private final Map<String, List<StemTransition>> multiStems;
  private volatile Map<String, Set<String>> asciiKeys = null;

  // Holds dictionary items that have multiple or different-than-item.root stem surface forms.
  private final Map<DictionaryItem, List<StemTransition>> differentStemItems;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private int maxStemLength = 0;

  public StemTransitionsMapBased(RootLexicon lexicon, TurkishMorphotactics morphotactics) {
    this.lexicon = lexicon;
    this.morphotactics = morphotactics;
    int initialCapacity = Math.max(64, lexicon.size() * 2);
    this.singleStems = new HashMap<>(initialCapacity);
    this.multiStems = new HashMap<>(Math.max(16, lexicon.size() / 8));
    this.differentStemItems = new HashMap<>(Math.max(16, lexicon.size() / 8));
    lexicon.forEach(this::addDictionaryItem);
  }

  private void generateAsciiTolerantMap() {
    lock.writeLock().lock();
    try {
      if (asciiKeys != null) {
        return;
      }
      Map<String, Set<String>> map = new HashMap<>(Math.max(64, singleStems.size() / 2));
      for (String s : singleStems.keySet()) {
        addAsciiKey(map, s);
      }
      for (String s : multiStems.keySet()) {
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

  @Override
  public Set<StemTransition> getTransitions() {
    lock.readLock().lock();
    try {
      HashSet<StemTransition> result = new HashSet<>(singleStems.size() + multiStems.size() * 2);
      result.addAll(singleStems.values());
      for (List<StemTransition> list : multiStems.values()) {
        result.addAll(list);
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

  private void addStemTransition(StemTransition stemTransition) {
    final String surfaceForm = stemTransition.surface;
    if (surfaceForm.length() > maxStemLength) {
      maxStemLength = surfaceForm.length();
    }
    List<StemTransition> multi = multiStems.get(surfaceForm);
    if (multi != null) {
      multi.add(stemTransition);
    } else {
      StemTransition existing = singleStems.get(surfaceForm);
      if (existing != null) {
        singleStems.remove(surfaceForm);
        List<StemTransition> list = new ArrayList<>(2);
        list.add(existing);
        list.add(stemTransition);
        multiStems.put(surfaceForm, list);
      } else {
        singleStems.put(surfaceForm, stemTransition);
      }
    }
    Map<String, Set<String>> ascii = this.asciiKeys;
    if (ascii != null) {
      addAsciiKey(ascii, surfaceForm);
    }
  }

  private void removeStemNode(StemTransition stemTransition) {
    final String surfaceForm = stemTransition.surface;
    boolean stillPresent = false;
    List<StemTransition> multi = multiStems.get(surfaceForm);
    if (multi != null) {
      multi.remove(stemTransition);
      if (multi.isEmpty()) {
        multiStems.remove(surfaceForm);
      } else if (multi.size() == 1) {
        multiStems.remove(surfaceForm);
        singleStems.put(surfaceForm, multi.get(0));
        stillPresent = true;
      } else {
        stillPresent = true;
      }
    } else {
      StemTransition existing = singleStems.get(surfaceForm);
      if (existing != null && existing.item.equals(stemTransition.item)) {
        singleStems.remove(surfaceForm);
      } else if (existing != null) {
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
    StemTransition single = singleStems.get(stem);
    if (single != null) {
      out.add(single);
      return;
    }
    List<StemTransition> multi = multiStems.get(stem);
    if (multi != null) {
      out.addAll(multi);
    }
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
          String stem = input.substring(0, i);
          StemTransition single = singleStems.get(stem);
          if (single != null) {
            matches.add(single);
          } else {
            List<StemTransition> multi = multiStems.get(stem);
            if (multi != null) {
              matches.addAll(multi);
            }
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
  public List<StemTransition> getTransitions(DictionaryItem item) {
    lock.readLock().lock();
    try {
      List<StemTransition> diff = differentStemItems.get(item);
      if (diff != null) {
        return new ArrayList<>(diff);
      }
      StemTransition single = singleStems.get(item.root);
      if (single != null) {
        return single.item.equals(item) ? List.of(single) : Collections.emptyList();
      }
      List<StemTransition> multi = multiStems.get(item.root);
      if (multi == null) {
        return Collections.emptyList();
      }
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
