package zemberek.morphology.ambiguity;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.LineProcessor;
import zemberek.core.io.Strings;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Z3AbstractDisambiguator {


    public static final String NO_IG = "NO_IG";
    public static final String START_IG = "START_IG";
    public static final String END_IG = "END_IG";
    public static final String SENTENCE_START_PARSE = "<s>=[(<s>)(NO_IG)]";
    public static final String SENTENCE_END_PARSE = "</s>=[(</s>)(NO_IG)]";

    static Pattern WHITE_SPACE = Pattern.compile("\\s");

    public static class Z3WordData {
        public String word;
        public String correctParse;
        public List<String> allParses = Lists.newArrayList();

        static Z3WordData SENTENCE_END = new Z3WordData(SENTENCE_END_PARSE);
        static Z3WordData SENTENCE_START = new Z3WordData(SENTENCE_START_PARSE);

        Z3WordData(String line) {
            this.word = Strings.subStringUntilFirst(line, "=");
            String rest = Strings.subStringAfterFirst(line, "=");
            int i = 0;
            for (String s : Splitter.on("|").omitEmptyStrings().trimResults().split(rest)) {
                allParses.add(s);
                if (i == 0)
                    this.correctParse = s;
                i++;
            }
        }

        @Override
        public String toString() {
            return "WordData{" +
                    "word='" + word + '\'' +
                    ", correctParse='" + correctParse + '\'' +
                    ", allParses=" + allParses +
                    '}';
        }
    }

    public static class SentenceData {
        public List<Z3WordData> words;
        public LinkedList<String> correctParse = Lists.newLinkedList();
        public LinkedList<List<String>> allParse = Lists.newLinkedList();

        SentenceData(List<Z3WordData> words) {
            this.words = words;
            for (Z3WordData word : words) {
                correctParse.add(word.correctParse);
            }
            for (Z3WordData word : words) {
                allParse.add(word.allParses);
            }
        }

        public int size() {
            return words.size();
        }
    }

    public static class DataSet implements Iterable<SentenceData> {
        public List<SentenceData> sentences;

        DataSet(List<SentenceData> sentences) {
            this.sentences = sentences;
        }

        int tokenCount() {
            int tc = 0;
            for (SentenceData sentence : sentences) {
                tc += sentence.words.size();
            }
            return tc;
        }

        @Override
        public Iterator<SentenceData> iterator() {
            return sentences.iterator();
        }
    }

    static Set<String> ignoreLines = Sets.newHashSet(
            "<DOC>", "</DOC>", "<TITLE>", "</TITLE>", "<S>", "<doc>", "</doc>", "<title>", "</title>", "<s>");
    static String END_SENTENCE_TAG = "</S>";

    /**
     * Loads ambiguous sentences from a file.
     * File is CWB formatted.
     */
    public static class DataSetLoader implements LineProcessor<DataSet> {
        public List<SentenceData> sentences = Lists.newArrayList();
        public List<Z3WordData> currentWords = Lists.newArrayList();

        @Override
        public boolean processLine(String s) throws IOException {
            s = s.trim();
            if (s.length() == 0)
                return true;
            if (ignoreLines.contains(s))
                return true;
            if (s.equalsIgnoreCase(END_SENTENCE_TAG)) {
                sentences.add(new SentenceData(currentWords));
                currentWords = Lists.newArrayList();
            } else {
                currentWords.add(new Z3WordData(s));
            }
            return true;
        }

        @Override
        public DataSet getResult() {
            return new DataSet(sentences);
        }
    }

    /**
     * Loads ambiguous sentences from a file.
     * File is CWB formatted.
     */
    public static class Z3DataSetLoader implements LineProcessor<DataSet> {
        public List<SentenceData> sentences = Lists.newArrayList();
        public List<Z3WordData> currentWords = Lists.newArrayList();

        @Override
        public boolean processLine(String s) throws IOException {
            s = s.trim();
            String first = Strings.subStringUntilFirst(s, " ");
            if (ignoreLines.contains(first))
                return true;
            if (first.equalsIgnoreCase(END_SENTENCE_TAG)) {
                sentences.add(new SentenceData(currentWords));
                currentWords = Lists.newArrayList();
            } else
                currentWords.add(new Z3WordData(s));
            return true;
        }

        @Override
        public DataSet getResult() {
            return new DataSet(sentences);
        }
    }


    static class Z3WordParse {
        public String root;
        public List<String> igs;
        public String all;

        private static Pattern chunkPattern = Pattern.compile("(?:\\()(.+?)(?:\\))");

        Z3WordParse(String parseString) {
            Matcher m = chunkPattern.matcher(parseString);
            List<String> chunks = Lists.newArrayListWithCapacity(3);
            int k = 0;
            while (m.find()) {
                String group = m.group(0);
                if (k == 0)
                    chunks.add(group.substring(1, group.length() - 1)); //remove paranthesis for lemma.
                else
                    chunks.add(group);
                k++;

            }
            this.root = chunks.get(0);
            this.igs = Lists.newArrayList(chunks.subList(1, chunks.size()));
            all = parseString;
        }

        String getLastIg() {
            if (igs.size() == 0)
                return NO_IG;
            return igs.get(igs.size() - 1);
        }
    }
}
