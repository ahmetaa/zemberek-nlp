package zemberek.core.collections;

import com.google.common.collect.Lists;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple set like data structure for counting unique elements. Not thread safe.
 */
public class Histogram<T> implements Iterable<T> {

    private final UIntValueMap<T> map;

    public Histogram(int initialSize) {
        map = new UIntValueMap<>(initialSize);
    }

    public Histogram(Map<T, Integer> countMap) {
        this.map = new UIntValueMap<>(countMap.size());
        for (T t : countMap.keySet()) {
            this.map.put(t, countMap.get(t));
        }
    }

    public Histogram() {
        map = new UIntValueMap<>();
    }

    /**
     * Loads a String Histogram from a file. Counts are supposedly delimited with `delimiter` character.
     *
     * @param path      file path
     * @param delimiter delimiter
     * @return a Histogram.
     */
    public static Histogram<String> loadFromUtf8File(Path path, char delimiter) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Histogram<String> result = new Histogram<>(lines.size());
        for (String s : lines) {
            int index = s.indexOf(delimiter);
            if (index <= 0) {
                throw new IllegalStateException("Bad histogram line = " + s);
            }
            String item = s.substring(0, index);
            String countStr = s.substring(index + 1);
            int count = Integer.parseInt(countStr);
            result.add(item, count);
        }
        return result;
    }

    public void saveSortedByCounts(Path path, String delimiter)
            throws IOException {
        try (PrintWriter pw = new PrintWriter(path.toFile(), StandardCharsets.UTF_8.name())) {
            List<T> sorted = getSortedList();
            for (T t : sorted) {
                pw.println(t + delimiter + getCount(t));
            }
        }
    }

    public void saveSortedByKeys(Path path, String delimiter, Comparator<T> comparator)
            throws IOException {
        try (PrintWriter pw = new PrintWriter(path.toFile(), StandardCharsets.UTF_8.name())) {
            List<T> sorted = getSortedList(comparator);
            for (T t : sorted) {
                pw.println(t + delimiter + getCount(t));
            }
        }
    }

    public static void serializeStringHistogram(Histogram<String> h, DataOutputStream dos) throws IOException {
        dos.writeInt(h.size());
        for (String key : h.map) {
            dos.writeUTF(key);
            dos.writeInt(h.getCount(key));
        }
    }

    public static Histogram<String> deserializeStringHistogram(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        if (size < 0) {
            throw new IllegalStateException("Cannot deserialize String histogram. Count value is negative : " + size);
        }
        Histogram<String> result = new Histogram<>(size);
        for (int i = 0; i < size; i++) {
            result.set(dis.readUTF(), dis.readInt());
        }
        return result;
    }


    /**
     * adds an element. and increments it's count.
     *
     * @param t element to add.
     * @return the count of the added element.
     * @throws NullPointerException if element is null.
     */
    public int add(T t) {
        return add(t, 1);
    }

    /**
     * adds an element. and increments it's count.
     *
     * @param t     element to add.
     * @param count the count of the element to add.
     * @return the count of the added element.
     * @throws NullPointerException if element is null.
     */
    public int add(T t, int count) {
        return map.incrementByAmount(t, count);
    }

    /**
     * merges another Histogram to this one.
     *
     * @param otherSet another Histogram
     */
    public void add(Histogram<T> otherSet) {
        if (otherSet == null)
            throw new NullPointerException("Histogram cannot be null");
        for (T t : otherSet) {
            add(t, otherSet.getCount(t));
        }
    }

    public T lookup(T item) {
        return map.lookup(item);
    }

    /**
     * adds a collection of elements.
     *
     * @param collection a collection of elements.
     */
    public void add(Collection<T> collection) {

        if (collection == null)
            throw new NullPointerException("collection cannot be null");
        collection.forEach(this::add);
    }

    /**
     * adds an array of elements.
     *
     * @param array an array of elements to add.
     */
    @SafeVarargs
    public final void add(T... array) {
        if (array == null)
            throw new NullPointerException("array cannot be null");
        for (T t : array) {
            add(t);
        }
    }

    /**
     * returns the total element count of the counting set.
     *
     * @return element count.
     */
    public int size() {
        return map.size();
    }

    /**
     * inserts the element and its value. it overrides the current count
     *
     * @param t element
     * @param c count value which will override the current count value.
     */
    public void set(T t, int c) {
        map.put(t, c);
    }

    public int decrementIfPositive(T t) {
        if (t == null)
            throw new NullPointerException("Element cannot be null");
        int c = map.get(t);
        if (c > 0) {
            return map.decrement(t);
        } else {
            return 0;
        }
    }

    /**
     * current count of the given element
     *
     * @param t element
     * @return count of the element. if element does not exist, 0
     */
    public int getCount(T t) {
        int i = map.get(t);
        return i < 0 ? 0 : i;
    }


    /**
     * if element exist.
     *
     * @param t element.
     * @return if element exists.
     */
    public boolean contains(T t) {
        return map.contains(t);
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<T> getTop(int n) {
        if (n > size())
            n = size();
        List<IntValueMap.Entry<T>> l = map.getAsEntryList();
        Collections.sort(l);
        List<T> result = l.stream().map(e -> e.key).collect(Collectors.toList());
        return Lists.newArrayList(result.subList(0, n));
    }

    /**
     * removes the items that has a count smaller than minCount
     *
     * @param minCount minimum count amount to remain in the set.
     * @return reduced set.
     */
    public int removeSmaller(int minCount) {
        Set<T> toRemove = new HashSet<>();
        int removeCount = 0;
        for (T key : map) {
            if (map.get(key) < minCount) {
                toRemove.add(key);
                removeCount++;
            }
        }
        toRemove.forEach(map::remove);
        return removeCount;
    }


    /**
     * removes the items that has a count larger than minCount
     *
     * @param maxCount maximum count amount to remain in the set.
     * @return reduced set.
     */
    public int removeLarger(int maxCount) {
        Set<T> toRemove = new HashSet<>();
        int removeCount = 0;
        for (T key : map) {
            if (map.get(key) > maxCount) {
                toRemove.add(key);
                removeCount++;
            }
        }
        toRemove.forEach(map::remove);
        return removeCount;
    }

    /**
     * counts the items those count is smaller than amount
     *
     * @param amount to check size
     * @return count.
     */
    public int sizeSmaller(int amount) {
        return map.sizeSmaller(amount);
    }

    /**
     * removes an item.
     *
     * @param t item to removed.
     */
    public void remove(T t) {
        map.remove(t);
    }

    /**
     * removes all items.
     *
     * @param items item to removed.
     */
    public void removeAll(Iterable<T> items) {
        for (T t : items) {
            map.remove(t);
        }
    }

    /**
     * counts the items those count is smaller than amount
     *
     * @param amount amount to check size
     * @return count.
     */
    public int sizeLarger(int amount) {
        return map.sizeLarger(amount);
    }

    /**
     * total count of items those value is between "minValue" and "maxValue"
     *
     * @param minValue minValue inclusive
     * @param maxValue maxValue inclusive
     * @return total count of items those value is between "minValue" and "maxValue"
     */
    public long totalCount(int minValue, int maxValue) {
        return map.sumOfValues(minValue, maxValue);
    }

    /**
     * returns the max count value.
     *
     * @return the max value in the set if set is empty, 0 is returned.
     */
    public int maxValue() {
        return map.maxValue();
    }

    /**
     * returns the min count value.
     *
     * @return the min value in the set, if set is empty, Integer.MAX_VALUE is returned.
     */
    public int minValue() {
        return map.minValue();
    }

    /**
     * returns the list of elements whose count is equal to "value"
     *
     * @param value the value for the keys
     * @return the list of elements whose count is equal to "value"
     */
    public List<T> getItemsWithCount(int value) {
        List<T> keys = new ArrayList<>();
        for (T key : map) {
            if (map.get(key) == value) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * Returns the list of elements whose count is between "min" and "max" (both inclusive)
     *
     * @param min min
     * @param max max
     * @return Returns the list of elements whose count is between "min" and "max" (both inclusive)
     */
    public List<T> getItemsWithCount(int min, int max) {
        List<T> keys = new ArrayList<>();
        for (T key : map) {
            int value = map.get(key);
            if (value >= min && value <= max) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * Percentage of total count of [min-max] to total counts.
     *
     * @param min min (inclusive)
     * @param max max (inclusive)
     * @return count.
     */
    public double countPercent(int min, int max) {
        return (totalCount(min, max) * 100d) / totalCount();
    }

    /**
     * returns the Elements in a list sorted by count, descending.
     *
     * @return Elements in a list sorted by count, descending.
     */
    public List<T> getSortedList() {
        List<T> l = Lists.newArrayListWithCapacity(map.size());
        l.addAll(getSortedEntryList().stream().map(entry -> entry.key).collect(Collectors.toList()));
        return l;
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<IntValueMap.Entry<T>> getSortedEntryList() {
        List<IntValueMap.Entry<T>> l = map.getAsEntryList();
        Collections.sort(l);
        return l;
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<IntValueMap.Entry<T>> getEntryList() {
        return map.getAsEntryList();
    }


    /**
     * @return total count of the items in the input Iterable.
     */
    public long totalCount(Iterable<T> it) {
        long count = 0;
        for (T t : it) {
            count += getCount(t);
        }
        return count;
    }

    /**
     * returns the Elements in a list sorted by the given comparator..
     *
     * @param comp a Comarator of T
     * @return Elements in a list sorted by the given comparator..
     */
    public List<T> getSortedList(Comparator<T> comp) {
        List<T> l = Lists.newArrayList(map);
        l.sort(comp);
        return l;
    }

    /**
     * returns elements in a set.
     *
     * @return a set containing the elements.
     */
    public Set<T> getKeySet() {
        return map.getKeySet();
    }


    /**
     * returns an iterator for elements.
     *
     * @return returns an iterator for elements.
     */
    public Iterator<T> iterator() {
        return map.iterator();
    }

    /**
     * Sums all item's counts.
     *
     * @return sum of all item's count.
     */
    public long totalCount() {
        return map.sumOfValues();
    }

    private class CountComparator implements Comparator<Map.Entry<T, Integer>> {
        public int compare(Map.Entry<T, Integer> o1, Map.Entry<T, Integer> o2) {
            return (o2.getValue() < o1.getValue()) ? -1 : ((o2.getValue() > o1.getValue()) ? 1 : 0);
        }
    }
}