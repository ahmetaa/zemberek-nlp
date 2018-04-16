package zemberek.morphology.analysis;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class SentenceAnalysis implements Iterable<SentenceWordAnalysis> {

  private String sentence;
  private List<SentenceWordAnalysis> parseEntries;

  public SentenceAnalysis(
      String sentence,
      List<SentenceWordAnalysis> parseEntries) {
    this.sentence = sentence;
    this.parseEntries = parseEntries;
  }

  public int size() {
    return parseEntries.size();
  }

  public String getSentence() {
    return sentence;
  }

  @Override
  public Iterator<SentenceWordAnalysis> iterator() {
    return parseEntries.iterator();
  }

  public List<SentenceWordAnalysis> getParseEntries() {
    return parseEntries;
  }

  public List<SingleAnalysis> bestAnalysis() {
    return parseEntries.stream().map(s -> s.analysis).collect(Collectors.toList());
  }

  public List<WordAnalysis> allAnalyses() {
    return parseEntries.stream().map(s -> s.wordAnalysis).collect(Collectors.toList());
  }

}
