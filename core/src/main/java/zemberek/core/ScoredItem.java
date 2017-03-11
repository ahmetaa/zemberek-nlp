package zemberek.core;

import java.util.Comparator;

/**
 * Represents an object attached with a double score.
 *
 * @param <T> item
 */
public class ScoredItem<T> implements Comparable<ScoredItem> {
    public final T item;
    public final double score;

    public ScoredItem(T item, double score) {
        this.item = item;
        this.score = score;
    }

    public static final Comparator<ScoredItem<String>> STRING_COMP_DESCENDING = Comparator.comparingDouble(a -> a.score);

    @Override
    public int compareTo(ScoredItem o) {
        return Double.compare(o.score, score);
    }

    @Override
    public String toString() {
        return toString(4);
    }

    public String toString(int fractionDigits) {
        return item.toString() + ":" + String.format("%." + fractionDigits + "f", score);
    }
}
