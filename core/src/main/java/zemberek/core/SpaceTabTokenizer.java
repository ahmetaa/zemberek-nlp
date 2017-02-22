package zemberek.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits a sentence to words from spaces or tabs. Multiple space/tabs are ignored.
 * This class is slightly faster than using String split method.
 */
public class SpaceTabTokenizer {

    static final String[] EMPTY_STRING_ARRAY = new String[0];

    public String[] split(String line) {
        int wordCount = 0;
        int[] spacePointers = new int[line.length() / 3];
        int start = 0;
        int end = 0;
        for (int i = 0; i < line.length(); i++) {

            if (line.charAt(i) == ' ' || line.charAt(i) == '\t') {
                if (i == start) {
                    start++;
                    end++;
                    continue;
                }
                end = i;
                if (wordCount == spacePointers.length >>> 2) {
                    spacePointers = Arrays.copyOf(spacePointers, spacePointers.length + 10);
                }
                spacePointers[wordCount * 2] = start;
                spacePointers[wordCount * 2 + 1] = end;
                end++;
                start = end;
                wordCount++;
            } else
                end++;
        }
        if (start != end) {
            if (wordCount == spacePointers.length >>> 2) {
                spacePointers = Arrays.copyOf(spacePointers, spacePointers.length + 2);
            }
            spacePointers[wordCount * 2] = start;
            spacePointers[wordCount * 2 + 1] = end;
            wordCount++;
        }
        if (wordCount == 0)
            return EMPTY_STRING_ARRAY;
        String[] words = new String[wordCount];
        for (int i = 0; i < wordCount; i++) {
            words[i] = line.substring(spacePointers[i * 2], spacePointers[i * 2 + 1]);
        }
        return words;
    }

    // TODO: write a better one.
    public List<String> splitToList(String line) {
        int wordCount = 0;
        int[] spaces = new int[line.length() / 3];
        int start = 0;
        int end = 0;
        for (int i = 0; i < line.length(); i++) {

            if (line.charAt(i) == ' ' || line.charAt(i) == '\t') {
                if (i == start) {
                    start++;
                    end++;
                    continue;
                }
                end = i;
                if (wordCount == spaces.length >>> 2) {
                    spaces = Arrays.copyOf(spaces, spaces.length + 10);
                }
                spaces[wordCount * 2] = start;
                spaces[wordCount * 2 + 1] = end;
                end++;
                start = end;
                wordCount++;
            } else
                end++;
        }
        if (start != end) {
            if (wordCount == spaces.length >>> 2) {
                spaces = Arrays.copyOf(spaces, spaces.length + 2);
            }
            spaces[wordCount * 2] = start;
            spaces[wordCount * 2 + 1] = end;
            wordCount++;
        }
        if (wordCount == 0)
            return new ArrayList<>(0);
        List<String> words = new ArrayList<>(wordCount + 1);
        for (int i = 0; i < wordCount; i++) {
            words.add(line.substring(spaces[i * 2], spaces[i * 2 + 1]));
        }
        return words;
    }
}
