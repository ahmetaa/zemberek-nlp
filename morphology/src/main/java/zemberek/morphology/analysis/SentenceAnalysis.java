package zemberek.morphology.analysis;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class holds the result of a morphological analysis and disambiguation results of words in
 * a sentence.
 */
public class SentenceAnalysis implements Iterable<SentenceWordAnalysis> {

  private String sentence;
  private List<SentenceWordAnalysis> wordAnalyses;

  public SentenceAnalysis(
      String sentence,
      List<SentenceWordAnalysis> wordAnalyses) {
    this.sentence = sentence;
    this.wordAnalyses = wordAnalyses;
  }

  public int size() {
    return wordAnalyses.size();
  }

  public String getSentence() {
    return sentence;
  }

  @Override
  public Iterator<SentenceWordAnalysis> iterator() {
    return wordAnalyses.iterator();
  }

  /**
   * Returns a list of SentenceWordAnalysis objects. Objects holds all and best analysis of a token
   * in the sentence.
   *
   * @return SentenceWordAnalysis list.
   */
  public List<SentenceWordAnalysis> getWordAnalyses() {
    return wordAnalyses;
  }

  /**
   * Returns a list of SentenceWordAnalysis objects. Objects holds all and best analysis of a token
   * in the sentence.
   *
   * @return SentenceWordAnalysis list.
   * @deprecated Use {@link #getWordAnalyses()}. This will be removed in 0.16.0
   */
  public List<SentenceWordAnalysis> parseEntries() {
    return wordAnalyses;
  }

  /**
   * Returns only the best SingleAnalysis results for each token in the sentence.
   * If used wants to access word string,
   */
  public List<SingleAnalysis> bestAnalysis() {
    return wordAnalyses.stream().map(s -> s.bestAnalysis).collect(Collectors.toList());
  }

  /**
   * Returns all analyses of all words as a list.
   */
  public List<WordAnalysis> ambiguousAnalysis() {
    return wordAnalyses.stream().map(s -> s.wordAnalysis).collect(Collectors.toList());
  }

  /**
   * Returns all analyses of all words as a list.
   * @deprecated Use {@link #ambiguousAnalysis()}. This will be removed in 0.16.0
   */
  public List<WordAnalysis> allAnalyses() {
    return wordAnalyses.stream().map(s -> s.wordAnalysis).collect(Collectors.toList());
  }


}
