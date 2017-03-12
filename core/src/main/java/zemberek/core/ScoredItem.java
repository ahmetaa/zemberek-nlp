package zemberek.core;

import java.util.Comparator;

/**
 * Represents an object attached with a float score.
 *
 * @param <T> item
 */
public class ScoredItem<T> implements Comparable<ScoredItem> {
    public final T item;
    public final float score;

    public ScoredItem(T item, float score) {
        this.item = item;
        this.score = score;
    }

    public static final Comparator<ScoredItem<String>> STRING_COMP_DESCENDING =
            (a, b) -> Float.compare(b.score, a.score);
    public static final Comparator<ScoredItem<String>> STRING_COMP_ASCENDING =
            (a, b) -> Float.compare(a.score, b.score);

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
