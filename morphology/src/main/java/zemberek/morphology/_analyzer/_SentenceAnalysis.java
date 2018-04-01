package zemberek.morphology._analyzer;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;

public class _SentenceAnalysis  implements Iterable<_WordAnalysis> {

  private List<_WordAnalysis> parseEntries = Lists.newArrayList();

  public int size() {
    return parseEntries.size();
  }

  public void addParse(_WordAnalysis analysis) {
    parseEntries.add(analysis);
  }

  public _WordAnalysis getParses(int index) {
    return parseEntries.get(index);
  }

  public String getInput(int index) {
    return parseEntries.get(index).input;
  }

  public _WordAnalysis getEntry(int index) {
    return parseEntries.get(index);
  }

  public void dump() {
    for (_WordAnalysis entry : parseEntries) {
      System.out.println(entry.input + "=");
      for (_SingleAnalysis wordAnalysis : entry.analysisResults) {
        System.out.println(wordAnalysis.format());
      }
      System.out.println();
    }
  }

  @Override
  public Iterator<_WordAnalysis> iterator() {
    return parseEntries.iterator();
  }

}
