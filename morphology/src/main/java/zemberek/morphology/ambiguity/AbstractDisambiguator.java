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
import java.util.regex.Pattern;

public abstract class AbstractDisambiguator {

    public static final String NO_IG = "NO_IG";
    public static final String START_IG = "START_IG";
    public static final String END_IG = "END_IG";
    public static final String END_SENTENCE = "</s> </s>";
    public static final String BEGIN_SENTENCE = "</s> </s>";

    static Pattern WHITE_SPACE = Pattern.compile("\\s");

    public static class WordData {
        public String word;
        public String correctParse;
        public List<String> allParses = Lists.newArrayList();

        static WordData SENTENCE_END = new WordData(END_SENTENCE);
        static WordData SENTENCE_START = new WordData(BEGIN_SENTENCE);

        WordData(String line) {
            int i = 0;
            for (String s : Splitter.on(WHITE_SPACE).omitEmptyStrings().trimResults().split(line)) {
                if (i == 0)
                    this.word = s;
                else allParses.add(s);
                if (i == 1) {
                    this.correctParse = s;
                }
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
        public List<WordData> words;
        public LinkedList<String> correctParse = Lists.newLinkedList();
        public LinkedList<List<String>> allParse = Lists.newLinkedList();

        SentenceData(List<WordData> words) {
            this.words = words;
            for (WordData word : words) {
                correctParse.add(word.correctParse);
            }
            for (WordData word : words) {
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
        public List<WordData> currentWords = Lists.newArrayList();

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
                currentWords.add(new WordData(s));
            return true;
        }

        @Override
        public DataSet getResult() {
            return new DataSet(sentences);
        }
    }

    static class WordParse {
        public String root;
        public String all;
        public List<String> igs = Lists.newArrayList();
        public String allIgs;

        WordParse(String parseString) {
            if (!parseString.contains("+")) {
                root = parseString;
                igs = Lists.newArrayList();
                allIgs = "";
            } else {
                if (parseString.startsWith("+")) {
                    root = "+";
                    igs = Lists.newArrayList("+Punc");
                    allIgs ="+Punc";

                } else {
                    root = Strings.subStringUntilFirst(parseString, "+");
                    String rest = "+" + Strings.subStringAfterFirst(parseString, "+");
                    String igsStr = rest.replaceAll("\\^DB", " ");
                    igs = Lists.newArrayList(Splitter.on(" ").omitEmptyStrings().trimResults().split(igsStr));
                    allIgs =rest.replaceAll("\\^DB", " ");
                }
            }
            all = parseString;
        }

        String getLastIg() {
            if (igs.size() == 0)
                return NO_IG;
            return igs.get(igs.size()-1);
        }
    }
}
