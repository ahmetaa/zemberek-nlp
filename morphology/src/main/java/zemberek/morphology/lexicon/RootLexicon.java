package zemberek.morphology.lexicon;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private Multimap<String, DictionaryItem> itemMap = HashMultimap.create(INITIAL_CAPACITY, 1);
  private Map<String, DictionaryItem> idMap = new HashMap<>(INITIAL_CAPACITY);
  private Set<DictionaryItem> itemSet = new LinkedHashSet<>(INITIAL_CAPACITY);

  public RootLexicon(List<DictionaryItem> dictionaryItems) {
    for (DictionaryItem dictionaryItem : dictionaryItems) {
      add(dictionaryItem);
    }
  }

  public RootLexicon() {
  }

  public void add(DictionaryItem item) {
    if (itemSet.contains(item)) {
      Log.warn("Duplicated item:" + item);
      return;
    }
    if (idMap.containsKey(item.id)) {
      Log.warn("Duplicated item id of:" + item + " with " + idMap.get(item.id));
      return;
    }
    this.itemSet.add(item);
    idMap.put(item.id, item);
    itemMap.put(item.lemma, item);
  }

  public void addAll(Iterable<DictionaryItem> items) {
    for (DictionaryItem item : items) {
      add(item);
    }
  }

  public Collection<DictionaryItem> getAllItems() {
    return itemMap.values();
  }

  public List<DictionaryItem> getMatchingItems(String lemma) {
    Collection<DictionaryItem> items = itemMap.get(lemma);
    if (items == null) {
      return Collections.emptyList();
    } else {
      return Lists.newArrayList(items);
    }
  }

  public void remove(DictionaryItem item) {
    itemMap.get(item.lemma).remove(item);
    idMap.remove(item.id);
    itemSet.remove(item);
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
    return itemSet.contains(item);
  }

  public DictionaryItem getItemById(String id) {
    return idMap.get(id);
  }

  public List<DictionaryItem> getMatchingItems(String lemma, PrimaryPos pos) {
    Collection<DictionaryItem> items = itemMap.get(lemma);
    if (items == null) {
      return Collections.emptyList();
    }
    List<DictionaryItem> matches = Lists.newArrayListWithCapacity(1);
    for (DictionaryItem item : items) {
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
    return itemSet.isEmpty();
  }

  public int size() {
    return itemSet.size();
  }

  @Override
  public Iterator<DictionaryItem> iterator() {
    return itemSet.iterator();
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
