package zemberek.core;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

/**
 * Defines an integer range.
 * TODO: move Span from tokenizer package to here and elimninate this.
 */
public class Range implements Comparable<Range> {
    public final int from;
    public final int to;

    public Range(int from, int to) {
        Preconditions.checkArgument(from < to,
                "Range start cannot be larger than end. But start=" + from + "end=" + to);
        this.to = to;
        this.from = from;
    }

    public int mid() {
        return from + (to - from) / 2;
    }

    public int length() {
        return to - from;
    }

    public Range copy(int offset) {
        return new Range(offset + from, offset + to);
    }

    @Override
    public String toString() {
        return from + "-" + to;
    }

    @Override
    public int compareTo(Range range) {
        return Ints.compare(length(), range.length());
    }
}
