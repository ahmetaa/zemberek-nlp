package zemberek.core;


import java.util.*;

/**
 * This is a Set like data structure that adding an element does not overrides if an equivalent element exist in it.
 * Assume Object A and B. LookupSet L contains a. if A.equals(B) is true, L.add(B) does not
 * remove A from L and B is not added.
 * Also L.lookup(B) returns A. Internally it actually is a simple Map wrapper.
 */
public class LookupSet<T> implements Iterable<T> {
    private Map<T, T> map = new LinkedHashMap<>();

    public LookupSet(int size) {
        this.map = new HashMap<>(size);
    }

    public LookupSet(Iterable<T> iterable) {
        for (T t : iterable) {
            add(t);
        }
    }

    public LookupSet() {
        this.map = new HashMap<>();
    }

    public Set<T> getKeys() {
        return map.keySet();
    }

    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    /**
     * @param t t
     * @return if t exists, does nothing and returns false. Otherwise it adds it and returns true.
     */
    public boolean add(T t) {
        if (map.containsKey(t))
            return false;
        map.put(t, t);
        return true;
    }

    @SafeVarargs
    public final void addAll(T... t) {
        for (T t1 : t) {
            add(t1);
        }
    }

    public void addAll(Iterable<T> it) {
        for (T t1 : it) {
            add(t1);
        }
    }

    public T remove(T t) {
        return map.remove(t);
    }

    /**
     * @param t input object.
     * @return If an equivalent object to t exists, returns it. Otherwise returns null.
     */
    public T lookup(T t) {
        if (map.containsKey(t))
            return map.get(t);
        else return null;
    }

    /**
     * @param t input
     * @return @return If an equivalent object to t exists, returns it. Otherwise adds the t and returns t.
     */
    public T getOrAdd(T t) {
        if (map.containsKey(t)) {
            return map.get(t);
        } else {
            map.put(t, t);
            return t;
        }
    }

    public int size() {
        return map.size();
    }
}
