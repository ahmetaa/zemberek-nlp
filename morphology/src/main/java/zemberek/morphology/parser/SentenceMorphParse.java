package zemberek.morphology.parser;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

/**
 * Represents morphological parseCached of a sentence.
 */
public class SentenceMorphParse implements Iterable<SentenceMorphParse.Entry> {
    private List<Entry> parseEntries = Lists.newArrayList();

    public int size() {
        return parseEntries.size();
    }

    public void addParse(String input, List<MorphParse> parses) {
        parseEntries.add(new Entry(input, parses));
    }

    public List<MorphParse> getParses(int index) {
        return parseEntries.get(index).parses;
    }

    public Entry getEntry(int index) {
        return parseEntries.get(index);
    }

    public void dump() {
        for (Entry entry : parseEntries) {
            System.out.println(entry.input + "=");
            for (MorphParse morphParse : entry.parses) {
                System.out.println(morphParse.formatLong());
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
        public final List<MorphParse> parses;

        private Entry(String input, List<MorphParse> parses) {
            this.input = input;
            this.parses = parses;
        }
    }
}
