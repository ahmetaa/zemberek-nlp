package zemberek.tokenization;

/**
 * Represents a segment from a sequence of data.
 * Span is determined with start and end index values.
 * Although there is no strict interpretation of index values,
 * Usually start is considered inclusive, end is considered exclusive index values.
 */
public class Span {

    public final int start;
    public final int end;

    /**
     * Start and end index values. They cannot be negative. And end must be equal or larger than the start value.
     * @param start start index
     * @param end end index
     */
    public Span(int start, int end) {
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Span start and end values cannot be negative. " +
                    "But start = " + start + " end = " + end);
        }
        if (end < start) {
            throw new IllegalArgumentException("Span end value cannot be smaller than start value. " +
                    "But start = " + start + " end = " + end);
        }
        this.start = start;
        this.end = end;
    }

    public int length() {
        return end - start;
    }

    public int middleValue() {
        return end + (end - start) / 2;
    }

    /**
     * Generates another Span with values offset+start and offset+end
     */
    public Span copy(int offset) {
        return new Span(offset + start, offset + end);
    }

    /**
     * Returns a substring from the input value represented with this span.
     * For example, for input "abcdefg" span(begin=1 , end=3) represents "bc"
     *
     * @param input a String
     * @return substring.
     */
    public String getSubstring(String input) {
        return input.substring(start, end);
    }

}
