/*
 *
 * Copyright (c) 2008, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zemberek.core;

import java.util.*;

/**
 * This is the map based Histogram implementation. It is only used for performance tests internally.
 */
class CountingSet<T> implements Iterable<T> {

    private final Map<T, Integer> map;

    public CountingSet(int initialSize) {
        map = new HashMap<>(initialSize);
    }

    public CountingSet(Map<T, Integer> map) {
        this.map = map;
    }

    public CountingSet() {
        map = new HashMap<>(10000);
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
        if (map.containsKey(t)) {
            int j = map.get(t) + count;
            map.put(t, j);
            return j;
        } else {
            map.put(t, count);
            return count;
        }
    }

    /**
     * merges another CountingSet to this one.
     *
     * @param otherSet another CountingSet
     */
    public void add(CountingSet<T> otherSet) {
        if (otherSet == null)
            throw new NullPointerException("CountingSet cannot be null");
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
        for (T t : collection) {
            add(t);
        }
    }

    /**
     * adds an array of elements.
     *
     * @param array an array of elements to add.
     */
    public void add(T... array) {
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
    public void replace(T t, int c) {
        if (t == null)
            throw new NullPointerException("Element cannot be null");
        if (c < 0)
            throw new IllegalArgumentException("Element count cannot be negative.");
        map.put(t, c);
    }

    /**
     * current count of the given element
     *
     * @param t element
     * @return count of the element. if element does not exist, 0
     */
    public int getCount(T t) {
        if (map.containsKey(t))
            return map.get(t);
        else return 0;
    }


    /**
     * if element exist.
     *
     * @param t element.
     * @return if element exists.
     */
    public boolean contains(T t) {
        return map.containsKey(t);
    }

    /**
     * returns the first of items sorted by natural order.
     * if count is larger than size complete list is returned.
     *
     * @param count amount of items to be fetched.
     * @return returns the sub sorted list.
     */
    public List<T> getFirstSorted(int count) {
        if (count < 0)
            throw new IllegalArgumentException("count cannot be negative.");
        if (count > this.size())
            count = this.size();
        return getSortedList().subList(0, count);
    }

    /**
     * removes the items that has a count smaller than minCount
     *
     * @param minCount minimum count amount to remain in the set.
     * @return reduced set.
     */
    public int removeSmaller(int minCount) {
        Set<T> keys = map.keySet();
        Set<T> toRemove = new HashSet<T>();
        int removeCount = 0;
        for (T key : keys) {
            if (map.get(key) < minCount) {
                toRemove.add(key);
                removeCount++;
            }
        }
        for (T t : toRemove) {
            map.remove(t);
        }
        return removeCount;
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
        CountingSet<Integer> cs = new CountingSet<Integer>();
        for (T key : map.keySet()) {
            cs.add(map.get(key));
        }
        return new TreeMap<Integer, Integer>(cs.map);
    }

    /**
     * removes the items that has a count larger than minCount
     *
     * @param maxCount maximum count amount to remain in the set.
     * @return reduced set.
     */
    public int removeLarger(int maxCount) {
        Set<T> keys = map.keySet();
        Set<T> toRemove = new HashSet<T>();
        int removeCount = 0;
        for (T key : keys) {
            if (map.get(key) > maxCount) {
                toRemove.add(key);
                removeCount++;
            }
        }
        for (T t : toRemove) {
            map.remove(t);
        }
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
        Set<T> keys = map.keySet();
        for (T key : keys) {
            if (getCount(key) < amount)
                count++;
        }
        return count;
    }

    /**
     * removes an item.
     *
     * @param t item to removed.
     * @return count of the item before it is removed (if it exits). -1 otherwise.
     */
    public int remove(T t) {
        Integer i = map.remove(t);
        if (i == null)
            return -1;
        else
            return i;
    }


    /**
     * counts the items those count is smaller than amount
     *
     * @param amount amount to check size
     * @return count.
     */
    public int sizeLarger(int amount) {
        int count = 0;
        Set<T> keys = map.keySet();
        for (T key : keys) {
            if (getCount(key) > amount)
                count++;
        }
        return count;
    }

    /**
     * total count items those value is between "from" and "to"
     *
     * @param from from
     * @param to   to
     * @return total count of items those vlaue is between "from" and "to"
     */
    public int totalCount(int from, int to) {
        int count = 0;
        Set<T> keys = map.keySet();
        for (T key : keys) {
            if (getCount(key) >= from && getCount(key) < to)
                count += map.get(key);
        }
        return count;
    }

    /**
     * returns the max value.
     *
     * @return the max value in the set if set is emtpty, 0 is returned.
     */
    public int maxValue() {
        int max = 0;
        Set<T> keys = map.keySet();
        for (T key : keys) {
            if (getCount(key) > max)
                max = getCount(key);
        }
        return max;
    }

    /**
     * returns the min value.
     *
     * @return the min value in the set, if set is empty, Integer.MAX_VALUE is returned.
     */
    public int minValue() {
        int min = Integer.MAX_VALUE;
        Set<T> keys = map.keySet();
        for (T key : keys) {
            if (getCount(key) < min)
                min = getCount(key);
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
        List<T> keys = new ArrayList<T>();
        for (T key : map.keySet())
            if (map.get(key) == value) {
                keys.add(key);
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
        List<T> keys = new ArrayList<T>();
        for (T key : map.keySet())
            if (getCount(key) >= from && getCount(key) < to)
                keys.add(key);
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
        return (totalCount(from, to) * 100d) / countAll();
    }

    /**
     * returns the Elements in a list sorted by count, descending..
     *
     * @return Elements in a list sorted by count, descending..
     */
    public List<T> getSortedList() {
        List<Map.Entry<T, Integer>> l = new ArrayList<Map.Entry<T, Integer>>(map.entrySet());
        Collections.sort(l, new CountComparator());
        List<T> list = new ArrayList<T>();
        for (Map.Entry<T, Integer> entry : l) {
            list.add(entry.getKey());
        }
        return list;
    }

    /**
     * returns the Elements in a list sorted by the given comparator..
     *
     * @param comp a Comarator of T
     * @return Elements in a list sorted by the given comparator..
     */
    public List<T> getSortedList(Comparator<T> comp) {
        List<T> l = new ArrayList<T>(getSet());
        Collections.sort(l, comp);
        return l;
    }

    /**
     * returns elements in a set.
     *
     * @return a set containing the elements.
     */
    public Set<T> getSet() {
        return map.keySet();
    }


    /**
     * returns an iterator for elements.
     *
     * @return returns an iterator for elements.
     */
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Sums all item's counts.
     *
     * @return sum of all item's count.
     */
    public int countAll() {
        int count = 0;
        for (T t : getSet()) {
            count += map.get(t);
        }
        return count;
    }

    private class CountComparator implements Comparator<Map.Entry<T, Integer>> {
        public int compare(Map.Entry<T, Integer> o1, Map.Entry<T, Integer> o2) {
            return (o2.getValue() < o1.getValue()) ? -1 : ((o2.getValue() > o1.getValue()) ? 1 : 0);
        }
    }
}
