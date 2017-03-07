package zemberek.core.turkish.hyphenation;

/**
 * Provides syllable related operations.
 */
public interface Hyphenator {

    /**
     * Finds the splitting index of a word for a space constraint.
     * if <code>spaceAvailable</code>
     * is smaller than the length of the string, it will return word's length. if it is not possible to
     * fit first syllable to the <code>spaceAvailable</code> it will return -1.
     * <p>Example for Turkish:
     * <p><code>("merhaba", 4) -> 3 ["mer-haba"]</code>
     * <p><code>("merhaba", 6) -> 5 ["merha-ba"]</code>
     * <p><code>("merhaba", 2) -> -1 []</code>
     * <p><code>("dddaddd", 2) -> -1 []</code>
     * <p><code>("merhaba", 8) -> 7 ["merhaba"]</code>
     *
     * @param input          input String.
     * @param spaceAvailable the available space
     * @return an integer.
     */
    int splitIndex(String input, int spaceAvailable);
}
