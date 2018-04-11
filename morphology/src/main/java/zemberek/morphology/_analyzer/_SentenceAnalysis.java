package zemberek.morphology._analyzer;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class _SentenceAnalysis implements Iterable<_SentenceWordAnalysis> {

  private List<_SentenceWordAnalysis> parseEntries;

  public _SentenceAnalysis(
      List<_SentenceWordAnalysis> parseEntries) {
    this.parseEntries = parseEntries;
  }

  public int size() {
    return parseEntries.size();
  }

  @Override
  public Iterator<_SentenceWordAnalysis> iterator() {
    return parseEntries.iterator();
  }


  public List<_SentenceWordAnalysis> getParseEntries() {
    return parseEntries;
  }

  public List<_SingleAnalysis> bestAnalysis() {
    return parseEntries.stream().map(s -> s.analysis).collect(Collectors.toList());
  }

  public List<_WordAnalysis> allAnalyses() {
    return parseEntries.stream().map(s -> s.wordAnalysis).collect(Collectors.toList());
  }

}
