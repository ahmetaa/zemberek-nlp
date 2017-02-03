package zemberek.morphology.lexicon;

import com.google.common.collect.*;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;

import java.util.*;

/**
 * This is the collection of all Dictionary Items.
 */
public class RootLexicon implements Iterable<DictionaryItem> {
    private Multimap<String, DictionaryItem> itemMap = HashMultimap.create(100000,1);
    private Map<String, DictionaryItem> idMap = Maps.newHashMap();
    private Set<DictionaryItem> itemSet = Sets.newLinkedHashSet();

    public RootLexicon(List<DictionaryItem> dictionaryItems) {
        itemSet.addAll(dictionaryItems);
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

    public void addAll(Iterable<DictionaryItem>items) {
        for(DictionaryItem item : items) {
            add(item);
        }
    }

    public List<DictionaryItem> getMatchingItems(String lemma) {
        Collection<DictionaryItem> items = itemMap.get(lemma);
        if (items == null)
            return Collections.emptyList();
        else return Lists.newArrayList(items);
    }

    public void remove(DictionaryItem item) {
        itemMap.get(item.lemma).remove(item);
        idMap.remove(item.id);
        itemSet.remove(item);
    }

    public void removeAllLemmas(String lemma) {
        for(DictionaryItem item: getMatchingItems(lemma)) {
            remove(item);
        }
    }

    public void removeAllLemmas(Iterable<String> lemmas) {
        for(String lemma: lemmas) {
            removeAllLemmas(lemma);
        }
    }

    public void removeAll(Iterable<DictionaryItem> items) {
        for(DictionaryItem item: items) {
            remove(item);
        }
    }

    public DictionaryItem getItemById(String id) {
        return idMap.get(id);
    }

    public List<DictionaryItem> getMatchingItems(String lemma, PrimaryPos pos) {
        Collection<DictionaryItem> items = itemMap.get(lemma);
        if (items == null)
            return Collections.emptyList();
        List<DictionaryItem> matches = Lists.newArrayListWithCapacity(1);
        for (DictionaryItem item : items) {
            if(item.primaryPos==pos)
              matches.add(item);
        }
        return matches;
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
}
