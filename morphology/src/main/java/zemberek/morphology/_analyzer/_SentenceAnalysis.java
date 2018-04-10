package zemberek.morphology._analyzer;

import java.util.Iterator;
import java.util.List;

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

}
