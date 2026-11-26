package zemberek.morphology.lexicon;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

/**
 * This is the collection of all Dictionary Items.
 */
public class RootLexicon implements Iterable<DictionaryItem> {

  private static final int INITIAL_CAPACITY = 1000;

  private enum Singleton {
    Instance;
    RootLexicon defaultLexicon = defaultBinaryLexicon();
  }

  public static RootLexicon getDefault() {
    return Singleton.Instance.defaultLexicon;
  }

  // Stores DictionaryItem directly for single-sense lemmas (~97%) and DictionaryItem[] otherwise.
  private final Map<String, Object> lemmaMap;
  // DictionaryItem equality is defined by item.id; LinkedHashMap preserves insertion order without a duplicate Set.
  private final Map<String, DictionaryItem> idMap;

  public RootLexicon(List<DictionaryItem> dictionaryItems) {
    this(dictionaryItems.size());
    for (DictionaryItem dictionaryItem : dictionaryItems) {
      add(dictionaryItem);
    }
  }

  public RootLexicon(int expectedSize) {
    int mapCapacity = Math.max(16, (int) (expectedSize / 0.75f) + 1);
    this.lemmaMap = new HashMap<>(mapCapacity);
    this.idMap = new LinkedHashMap<>(mapCapacity);
  }

  public RootLexicon() {
    this(INITIAL_CAPACITY);
  }

  public void add(DictionaryItem item) {
    if (idMap.containsKey(item.id)) {
      Log.warn("Duplicated item:" + item + " with " + idMap.get(item.id));
      return;
    }
    idMap.put(item.id, item);
    Object existing = lemmaMap.get(item.lemma);
    if (existing == null) {
      lemmaMap.put(item.lemma, item);
    } else if (existing instanceof DictionaryItem single) {
      lemmaMap.put(item.lemma, new DictionaryItem[] {single, item});
    } else {
      DictionaryItem[] arr = (DictionaryItem[]) existing;
      DictionaryItem[] next = Arrays.copyOf(arr, arr.length + 1);
      next[arr.length] = item;
      lemmaMap.put(item.lemma, next);
    }
  }

  public void addAll(Iterable<DictionaryItem> items) {
    for (DictionaryItem item : items) {
      add(item);
    }
  }

  public Collection<DictionaryItem> getAllItems() {
    return Collections.unmodifiableCollection(idMap.values());
  }

  public List<DictionaryItem> getMatchingItems(String lemma) {
    Object existing = lemmaMap.get(lemma);
    if (existing == null) {
      return new ArrayList<>(0);
    }
    if (existing instanceof DictionaryItem single) {
      List<DictionaryItem> list = new ArrayList<>(1);
      list.add(single);
      return list;
    }
    DictionaryItem[] arr = (DictionaryItem[]) existing;
    List<DictionaryItem> list = new ArrayList<>(arr.length);
    Collections.addAll(list, arr);
    return list;
  }

  public void remove(DictionaryItem item) {
    if (item == null || idMap.remove(item.id) == null) {
      return;
    }
    Object existing = lemmaMap.get(item.lemma);
    if (existing instanceof DictionaryItem single) {
      if (single.equals(item)) {
        lemmaMap.remove(item.lemma);
      }
    } else if (existing instanceof DictionaryItem[] arr) {
      int idx = -1;
      for (int i = 0; i < arr.length; i++) {
        if (arr[i].equals(item)) {
          idx = i;
          break;
        }
      }
      if (idx >= 0) {
        if (arr.length == 2) {
          lemmaMap.put(item.lemma, arr[1 - idx]);
        } else {
          DictionaryItem[] next = new DictionaryItem[arr.length - 1];
          System.arraycopy(arr, 0, next, 0, idx);
          System.arraycopy(arr, idx + 1, next, idx, arr.length - idx - 1);
          lemmaMap.put(item.lemma, next);
        }
      }
    }
  }

  public void removeAllLemmas(String lemma) {
    for (DictionaryItem item : getMatchingItems(lemma)) {
      remove(item);
    }
  }

  public void removeAllLemmas(Iterable<String> lemmas) {
    for (String lemma : lemmas) {
      removeAllLemmas(lemma);
    }
  }

  public void removeAll(Iterable<DictionaryItem> items) {
    for (DictionaryItem item : items) {
      remove(item);
    }
  }

  public boolean containsItem(DictionaryItem item) {
    return item != null && idMap.containsKey(item.id);
  }

  public DictionaryItem getItemById(String id) {
    return idMap.get(id);
  }

  public List<DictionaryItem> getMatchingItems(String lemma, PrimaryPos pos) {
    Object existing = lemmaMap.get(lemma);
    if (existing == null) {
      return new ArrayList<>(0);
    }
    List<DictionaryItem> matches = new ArrayList<>(1);
    if (existing instanceof DictionaryItem single) {
      if (single.primaryPos == pos) {
        matches.add(single);
      }
      return matches;
    }
    for (DictionaryItem item : (DictionaryItem[]) existing) {
      if (item.primaryPos == pos) {
        matches.add(item);
      }
    }
    return matches;
  }

  public static RootLexicon fromLines(String... lines) {
    return builder().addDictionaryLines(lines).build();
  }

  public static RootLexicon fromResources(String... resources) throws IOException {
    return builder().addTextDictionaryResources(resources).build();
  }

  public static RootLexicon fromResources(Collection<String> resources) throws IOException {
    return builder().addTextDictionaryResources(resources).build();
  }

  public boolean isEmpty() {
    return idMap.isEmpty();
  }

  public int size() {
    return idMap.size();
  }

  @Override
  public Iterator<DictionaryItem> iterator() {
    return idMap.values().iterator();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    RootLexicon lexicon = new RootLexicon();

    public Builder addBinaryDictionary(Path dictionaryPath) throws IOException {
      lexicon.addAll(DictionarySerializer.load(dictionaryPath).getAllItems());
      return this;
    }

    public Builder addDefaultLexicon() {
      if(lexicon.size()==0) {
        lexicon = getDefault();
      } else {
        addLexicon(getDefault());
      }
      return this;
    }

    public Builder setLexicon(RootLexicon lexicon) {
      this.lexicon = lexicon;
      return this;
    }

    public Builder addLexicon(RootLexicon lexicon) {
      this.lexicon.addAll(lexicon.getAllItems());
      return this;
    }

    public Builder addTextDictionaries(File... dictionaryFiles) throws IOException {
      List<String> lines = new ArrayList<>();
      for (File file : dictionaryFiles) {
        lines.addAll(Files.readAllLines(file.toPath()));
      }
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public Builder addTextDictionaries(Path... dictionaryPaths) throws IOException {
      for (Path dictionaryPath : dictionaryPaths) {
        addTextDictionaries(dictionaryPath.toFile());
      }
      return this;
    }

    public Builder addDictionaryLines(String... lines) {
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public Builder removeDictionaryFiles(File... dictionaryFiles) throws IOException {
      for (File file : dictionaryFiles) {
        lexicon.removeAll(TurkishDictionaryLoader.load(file));
      }
      return this;
    }

    public Builder addTextDictionaryResources(Collection<String> resources) throws IOException {
      Log.info("Dictionaries :%s", String.join(", ", resources));
      List<String> lines = new ArrayList<>();
      for (String resource : resources) {
        lines.addAll(TextIO.loadLinesFromResource(resource));
      }
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public Builder addTextDictionaryResources(String... resources) throws IOException {
      return addTextDictionaryResources(Arrays.asList(resources));
    }

    public Builder removeItems(Iterable<String> dictionaryString) {
      lexicon.removeAll(TurkishDictionaryLoader.load(dictionaryString));
      return this;
    }

    public Builder removeAllLemmas(Iterable<String> lemmas) {
      lexicon.removeAllLemmas(lemmas);
      return this;
    }

    public Builder addDictionaryLines(Collection<String> lines) {
      lexicon.addAll(TurkishDictionaryLoader.load(lines));
      return this;
    }

    public RootLexicon build() {
      return lexicon;
    }
  }

  private static RootLexicon defaultBinaryLexicon() {
    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
      Log.info("Dictionary generated in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
      return lexicon;
    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot load default binary dictionary. Reason:" + e.getMessage(), e);
    }
  }
}
