package zemberek.morphology.analysis;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

/**
 * Represents morphological analysis of a sentence.
 */
public class SentenceAnalysis implements Iterable<SentenceAnalysis.Entry> {
    private List<Entry> parseEntries = Lists.newArrayList();

    public int size() {
        return parseEntries.size();
    }

    public void addParse(String input, List<WordAnalysis> parses) {
        parseEntries.add(new Entry(input, parses));
    }

    public List<WordAnalysis> getParses(int index) {
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
            for (WordAnalysis wordAnalysis : entry.parses) {
                System.out.println(wordAnalysis.formatLong());
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
        public final List<WordAnalysis> parses;

        private Entry(String input, List<WordAnalysis> parses) {
            this.input = input;
            this.parses = parses;
        }
    }
}
