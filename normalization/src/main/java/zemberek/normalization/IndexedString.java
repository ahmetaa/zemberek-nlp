package zemberek.normalization;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that keeps track of changes made to a string such that each character in the new string can be mapped
 * to a substring in the original string.
 */
public class IndexedString {

    private List<Pair<Integer, Integer>> indices;
    private StringBuilder stringBuilder;

    IndexedString() {
        stringBuilder = new StringBuilder();
        indices = new ArrayList<>();
    }


    IndexedString(String string, List<Pair<Integer, Integer>> indices) {
        assert(string.length() == indices.size());
        stringBuilder = new StringBuilder(string);
        this.indices = indices;
    }

    IndexedString(String string) {
        stringBuilder = new StringBuilder(string);
        this.indices = new ArrayList<>();
        for(int i=0; i < string.length(); i++) {
            this.indices.add(new Pair<>(i, i));
        }
    }

    public Integer getStartIndex(int i) {
        return indices.get(i).getKey();
    }

    public Integer getEndIndex(int i) {
        return indices.get(i).getValue();
    }

    public String toString() {
        return stringBuilder.toString();
    }

    public List<Pair<Integer, Integer>> getIndices() {
        return indices;
    }

    public void add(String string, int startIndex, int endIndex, IndexedString ns) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(" ");
            indices.add(new Pair<>(-1, -1));
        }
        stringBuilder.append(string);
        for(int i = 0; i<string.length(); i++) {
            indices.add(new Pair<>(ns.getStartIndex(startIndex), ns.getEndIndex(endIndex)));
        }
    }

    void reverse() {
        stringBuilder.reverse();
        Collections.reverse(indices);
    }

    /**
     * Returns the substring of a string according to the indices in the normalized string.
     */

    public String getSubstring(String original, int normalizedStartIndex, int normalizedEndIndex) {
        return original.substring(getStartIndex(normalizedStartIndex), getEndIndex(normalizedEndIndex)+1);
    }

}
