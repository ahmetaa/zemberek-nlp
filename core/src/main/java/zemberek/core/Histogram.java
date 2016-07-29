package zemberek.core;

import java.util.*;
import java.util.stream.Collectors;


/**
 * A simple set like data structure for counting unique elements. Not thread safe.
 */
public class Histogram<T> implements Iterable<T> {

    private final CountSet<T> vector;

    public Histogram(int initialSize) {
        vector = new CountSet<>(initialSize);
    }

    public Histogram(Map<T, Integer> countMap) {
        this.vector = new CountSet<>(countMap.size());
        for (T t : countMap.keySet()) {
            this.vector.incrementByAmount(t, countMap.get(t));
        }
    }

    public Histogram() {
        vector = new CountSet<>();
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
        if (t == null)
            throw new NullPointerException("Element cannot be null");
        if (count < 0)
            throw new IllegalArgumentException("Element count cannot be negative.");
        return vector.incrementByAmount(t, count);
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
        return vector.size();
    }

    /**
     * inserts the element and its value. it overrides the current count
     *
     * @param t element
     * @param c count value which will override the current count value.
     */
    public void set(T t, int c) {
        if (t == null)
            throw new NullPointerException("Element cannot be null");
        if (c < 0)
            throw new IllegalArgumentException("Element count cannot be negative.");
        vector.set(t, c);
    }

    /**
     * current count of the given element
     *
     * @param t element
     * @return count of the element. if element does not exist, 0
     */
    public int getCount(T t) {
        return vector.get(t);
    }


    /**
     * if element exist.
     *
     * @param t element.
     * @return if element exists.
     */
    public boolean contains(T t) {
        return vector.contains(t);
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<T> getTop(int n) {
        if (n > size())
            n = size();
        List<CountSet.Entry<T>> l = vector.getAsEntryList();
        Collections.sort(l);
        List<T> result = l.stream().map(e -> e.key).collect(Collectors.toList());
        return new ArrayList<>(result.subList(0, n));
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
        for (T key : vector) {
            if (vector.get(key) < minCount) {
                toRemove.add(key);
                removeCount++;
            }
        }
        toRemove.forEach(vector::remove);
        return removeCount;
    }

    public void removeAll(Iterable<T> iterable) {
        for (T t : iterable) {
            vector.remove(t);
        }
    }

    /**
     * this method returns a TreeMap that has :
     * - keys are the counts of the items which has same "counts" in the Counting set. For example if Set includes
     * {abc:4, cde:3, efg:4, jkl:1, mno:1, xyz:1}
     * then the TreeMap will have this values {1:3, 3:1, 4:2} saying that there are 3 items with count of 1, 1 item with count 3,
     * and 2 items with count of 4.
     *
     * @return a TreeMap containing count of items and their total counts.
     */
    public SortedMap<Integer, Integer> sortedCountMap() {
        SparseIntVector cs = new SparseIntVector(vector.size());
        for (CountSet.Entry<T> entry : vector.iterableEntries()) {
            cs.increment(entry.count);
        }
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (SparseIntVector.TableEntry entry : cs) {
            map.put(entry.key, entry.value);
        }
        return map;
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
        for (T key : vector) {
            if (vector.get(key) > maxCount) {
                toRemove.add(key);
                removeCount++;
            }
        }
        toRemove.forEach(vector::remove);
        return removeCount;
    }

    /**
     * counts the items those count is smaller than amount
     *
     * @param amount to check size
     * @return count.
     */
    public int sizeSmaller(int amount) {
        int count = 0;
        for (int val : vector.copyOfValues()) {
            if (val < amount)
                count++;
        }
        return count;
    }

    /**
     * removes an item.
     *
     * @param t item to removed.
     */
    public void remove(T t) {
        vector.remove(t);
    }


    /**
     * counts the items those count is smaller than amount
     *
     * @param amount amount to check size
     * @return count.
     */
    public int sizeLarger(int amount) {
        int count = 0;
        for (int val : vector.copyOfValues()) {
            if (val > amount)
                count++;
        }
        return count;
    }

    /**
     * total count items those value is between "from" and "to"
     *
     * @param from from inclusive
     * @param to   to exclusive
     * @return total count of items those value is between "from" and "to"
     */
    public long totalCount(int from, int to) {
        long count = 0;
        for (int val : vector.copyOfValues()) {
            if (val >= from && val < to)
                count += val;
        }
        return count;
    }

    /**
     * returns the max count value.
     *
     * @return the max value in the set if set is empty, 0 is returned.
     */
    public int maxValue() {
        int max = 0;
        for (int val : vector.copyOfValues()) {
            if (val > max)
                max = val;
        }
        return max;
    }

    /**
     * returns the min count value.
     *
     * @return the min value in the set, if set is empty, Integer.MAX_VALUE is returned.
     */
    public int minValue() {
        int min = 0;
        for (int val : vector.copyOfValues()) {
            if (val < min)
                min = val;
        }
        return min;
    }

    /**
     * returns the list of elements whose count is equal to "value"
     *
     * @param value the value for the keys
     * @return the list of elements whose count is equal to "value"
     */
    public List<T> getItemsForValue(int value) {
        List<T> keys = new ArrayList<>();
        for (T key : vector) {
            if (vector.get(key) == value) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * returns the list of elements whose count is from "from" to "to" ("to" exclusive)
     *
     * @param from form
     * @param to   to
     * @return the list of elements whose count is from "from" to "to" ("to" exclusive)
     */
    public List<T> getItemsForValue(int from, int to) {
        List<T> keys = new ArrayList<>();
        for (T key : vector) {
            int value = vector.get(key);
            if (value >= from && value < to) {
                keys.add(key);
            }
        }
        return keys;
    }


    /**
     * counts the items those count is smaller than amount
     *
     * @param from from
     * @param to   to
     * @return count.
     */
    public double countPercent(int from, int to) {
        return (totalCount(from, to) * 100d) / totalCount();
    }

    /**
     * returns the Elements in a list sorted by count, descending.
     *
     * @return Elements in a list sorted by count, descending.
     */
    public List<T> getSortedList() {
        List<T> l = new ArrayList<>(vector.size());
        l.addAll(getSortedEntryList().stream().map(entry -> entry.key).collect(Collectors.toList()));
        return l;
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<CountSet.Entry<T>> getSortedEntryList() {
        List<CountSet.Entry<T>> l = vector.getAsEntryList();
        Collections.sort(l);
        return l;
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<CountSet.Entry<T>> getEntryList() {
        return vector.getAsEntryList();
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
        List<T> l = new ArrayList<>(vector.getKeyList());
        Collections.sort(l, comp);
        return l;
    }

    /**
     * returns an iterator for elements.
     *
     * @return returns an iterator for elements.
     */
    public Iterator<T> iterator() {
        return vector.iterator();
    }

    /**
     * Sums all item's counts.
     *
     * @return sum of all item's count.
     */
    public long totalCount() {
        return totalCount(this);
    }

    public Set<T> getKeySet() {
        return vector.getKeys();
    }

    private class CountComparator implements Comparator<Map.Entry<T, Integer>> {
        public int compare(Map.Entry<T, Integer> o1, Map.Entry<T, Integer> o2) {
            return (o2.getValue() < o1.getValue()) ? -1 : ((o2.getValue() > o1.getValue()) ? 1 : 0);
        }
    }
}