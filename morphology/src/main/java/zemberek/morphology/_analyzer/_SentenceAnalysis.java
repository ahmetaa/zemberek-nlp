package zemberek.morphology._analyzer;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import zemberek.morphology._analyzer._SentenceAnalysis.Entry;

// TODO: This should contain _WordAnalysis instead of Entry.
public class _SentenceAnalysis  implements Iterable<Entry> {

  private List<Entry> parseEntries = Lists.newArrayList();

  public int size() {
    return parseEntries.size();
  }

  public void addParse(String input, List<_SingleAnalysis> parses) {
    parseEntries.add(new Entry(input, parses));
  }

  public List<_SingleAnalysis> getParses(int index) {
    return parseEntries.get(index).parses;
  }

  public String getInput(int index) {
    return parseEntries.get(index).input;
  }

  public Entry getEntry(int index) {
    return parseEntries.get(index);
  }

  public void dump() {
    for (Entry entry : parseEntries) {
      System.out.println(entry.input + "=");
      for (_SingleAnalysis wordAnalysis : entry.parses) {
        System.out.println(wordAnalysis.format());
      }
      System.out.println();
    }
  }

  @Override
  public Iterator<Entry> iterator() {
    return parseEntries.iterator();
  }

  public static class Entry {

    public final String input;
    public final List<_SingleAnalysis> parses;

    private Entry(String input, List<_SingleAnalysis> parses) {
      this.input = input;
      this.parses = parses;
    }
  }

}
