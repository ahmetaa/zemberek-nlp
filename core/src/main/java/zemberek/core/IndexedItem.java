package zemberek.core;

/**
 * An item with an index. This is useful when we want to keep track of a sequence of items processed by a system that
 * would change the processing order.
 *
 * @param <T>
 */
public class IndexedItem<T> implements Comparable<IndexedItem> {

    public final T item;
    public final int index;

    public IndexedItem(T item, int index) {
        this.item = item;
        this.index = index;
    }

    @Override
    public int compareTo(IndexedItem o) {
        if (o.index > index) return 1;
        else if (o.index < index) return -1;
        return 0;
    }

    @Override
    public String toString() {
        return index + ":" + item.toString();
    }
}
