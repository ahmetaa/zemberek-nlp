package zemberek.core.text;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * TextSegmenter splits block of text (without spaces) to known tokens. This class is not
 * thread-safe
 */
public abstract class TextSegmenter {

  public static final int MAX_TOKENS = 100;

  int maxTokenCount = MAX_TOKENS;

  protected TextSegmenter() {
  }

  protected TextSegmenter(int maxTokenCount) {
    this.maxTokenCount = maxTokenCount;
  }

  public static TextSegmenter getWordSetSegmenter(Collection<String> words) {
    return new WordSetSegmenter(words);
  }

  /**
   * Retrieves all possible segmentation as a string list.
   *
   * @param textToSegment input
   */
  public List<String> findAll(String textToSegment) {
    if (textToSegment.length() == 0) {
      return Collections.emptyList();
    }
    List<String> results = new ArrayList<>(2);
    LinkedList<String> buffer = new LinkedList<>();
    split(textToSegment, 0, 1, buffer, results, false);
    return results;
  }

  public String findFirst(String textToSegment) {
    if (textToSegment.length() == 0) {
      return null;
    }
    List<String> results = new ArrayList<>(2);
    LinkedList<String> buffer = new LinkedList<>();
    split(textToSegment, 0, 1, buffer, results, false);
    if (results.size() > 0) {
      return results.get(0);
    } else {
      return null;
    }
  }

  void split(String full,
      int start,
      int end,
      LinkedList<String> buffer,
      List<String> results,
      boolean findSingle
  ) {

    while (end <= full.length()) {
      String sub = full.substring(start, end);
      if (check(sub)) {
        if (end == full.length()) {
          if (buffer.size() < maxTokenCount) {
            if (buffer.size() == 0) {
              results.add(sub);
            } else {
              results.add(Joiner.on(" ").join(buffer) + " " + sub);
            }
            if (findSingle) {
              return;
            }
          }
        } else {
          buffer.add(sub);
          start = end;
          end = start + 1;
          break;
        }
      }
      end++;
    }
    if (end > full.length()) { // failed to find last word.
      if (buffer.size() == 0 || start == 0) // failed to finish
      {
        return;
      }
      String last = buffer.removeLast();
      start = start - last.length();
      end = start + last.length() + 1;
    }
    split(full, start, end, buffer, results, findSingle);
  }

  // checks if a token is
  protected abstract boolean check(String word);

  public static class WordSetSegmenter extends TextSegmenter {

    Set<String> words;

    public WordSetSegmenter(Collection<String> words) {
      this.words = new HashSet<>(words);
    }

    public WordSetSegmenter(String... words) {
      this.words = new HashSet<>(Arrays.asList(words));
    }

    @Override
    protected boolean check(String word) {
      return words.contains(word);
    }
  }

}
