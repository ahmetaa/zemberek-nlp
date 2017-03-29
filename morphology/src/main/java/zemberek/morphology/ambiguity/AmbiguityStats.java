package zemberek.morphology.ambiguity;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import zemberek.core.collections.Histogram;
import zemberek.core.io.LineIterator;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.tokenization.TurkishTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A class for calculating basic morphological ambiguity statistics
 *
 * @author mdakin@gmail.com
 */
public class AmbiguityStats {

    TurkishMorphology parser;
    TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;

    public AmbiguityStats() throws IOException {
        parser = TurkishMorphology.createWithDefaults();
    }

    public List<String> readAll(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        File file = new File(filename);
        LineIterator it = SimpleTextReader.trimmingUTF8Reader(file).getLineIterator();
        while (it.hasNext()) {
            String quotesHyphensNormalzied = TextUtil.normalizeQuotesHyphens(it.next());
            lines.add(Joiner.on(" ").join(lexer.tokenizeToStrings(quotesHyphensNormalzied)));
        }
        return lines;
    }

    class Stats {
        int significantCounts = 0;
        int significantUniques = 0;
        int allCounts = 0;
        int allUniques = 0;
        double cutoff;

        // cutoff: percentage 10 = %10 etc.
        Stats(double cutoff) {
            this.cutoff = cutoff;
        }

        boolean overCutoff(int x) {
            return ((double) x / allCounts * 100) > cutoff;
        }

        void dump() {
            String pCounts = percentStr(significantCounts, allCounts);
            String pUniques = percentStr(significantUniques, allUniques);
            System.out.println();
            System.out.println("Significants/All(Counts): " + significantCounts + "/" + allCounts + pp(pCounts));
            System.out.println("Significants/All(Uniques): " + significantUniques + "/" + allUniques + pp(pUniques));
            System.out.println();
        }
    }

    public String pp(String percent) {
        return " (%" + percent + ")";
    }

    public String percentStr(int p1, int p2) {
        return String.format("%.2f", pct(p1, p2));
    }

    public String percentStr3(int p1, int p2) {
        return String.format("%.3f", pct(p1, p2));
    }

    public double pct(int p1, int p2) {
        return (double) p1 / p2 * 100;
    }

    private static final Splitter splitter = Splitter.on(" ").omitEmptyStrings().trimResults();

    public void ambiguousGroupStats(String filename) throws IOException {
        List<String> lines = readAll(filename);
        Histogram<String> uniques = new Histogram<>(1000000);
        Map<String, Histogram<String>> ambiguityGroups = Maps.newHashMap();
        int total = 0;
        for (String line : lines) {
            for (String s : splitter.split(line)) {
                List<WordAnalysis> results = parser.getWordAnalyzer().analyze(
                        TurkishAlphabet.INSTANCE.normalize(s));
                if (++total % 50000 == 0) {
                    System.out.println("Processed: " + total);
                }
                if (results.size() > 1) {
                    String key = generateKeyFromParse(results);
                    uniques.add(key);
                    Histogram<String> members = ambiguityGroups.get(key);
                    if (members == null) {
                        members = new Histogram<>();
                        ambiguityGroups.put(key, members);
                    }
                    members.add(s);
                }
            }
        }
        System.out.println("Total: " + total);
        Stats st = new Stats(0.1);
        st.allCounts = (int) uniques.totalCount();
        st.allUniques = uniques.size();
        for (String s : uniques.getSortedList()) {
            int count = uniques.getCount(s);
            if (st.overCutoff(count)) {
                String p1 = percentStr(count, st.allCounts);
                st.significantCounts += count;
                st.significantUniques++;
                System.out.println(s + " : " + count + "    " + pp(p1));
                Histogram<String> members = ambiguityGroups.get(s);
                for (String member : members.getSortedList()) {
                    int memberCount = members.getCount(member);
                    if (pct(memberCount, count) > 0.1)
                        System.out.println(member + " : " + members.getCount(member));
                }
                System.out.println();
            }
        }
        st.dump();
    }

    private String generateKeyFromParse(List<WordAnalysis> results) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            key.append(results.get(i).formatOnlyIgs());
            if (i < results.size() - 1) key.append("\n");
        }
        return key.toString();
    }

    public void ambiguousWordStats(String filename) throws IOException {
        List<String> lines = readAll(filename);
        Histogram<String> uniques = new Histogram<>(1000000);
        int total = 0;
        Splitter splitter = Splitter.on(" ").omitEmptyStrings().trimResults();
        for (String line : lines) {
            for (String s : splitter.split(line)) {
                List<WordAnalysis> results =  parser.getWordAnalyzer().analyze(
                        TurkishAlphabet.INSTANCE.normalize(s));
                total++;
                if (total % 50000 == 0) {
                    System.out.println("Processed: " + total);
                }
                if (results.size() > 1) {
                    uniques.add(s);
                }
            }
        }
        System.out.println("Total: " + total);
        Stats st = new Stats(0.002);
        st.allCounts = (int) uniques.totalCount();
        st.allUniques = uniques.size();
        for (String s : uniques.getSortedList()) {
            int count = uniques.getCount(s);
            if (st.overCutoff(count)) {
                String p1 = percentStr3(count, st.allCounts);
                st.significantCounts += count;
                st.significantUniques++;
                System.out.println(s + " : " + count + "    " + pp(p1));
            }
        }
        st.dump();
    }

    public void noParse(String... filename) throws IOException {
        Histogram<String> uniques = new Histogram<>(1000000);
        int total = 0;
        for (String file : filename) {
            List<String> lines = readAll(file);
            Splitter splitter = Splitter.on(" ").omitEmptyStrings().trimResults();
            for (String line : lines) {
                for (String s : splitter.split(line)) {
                    List<WordAnalysis> results =  parser.getWordAnalyzer().analyze(
                            TurkishAlphabet.INSTANCE.normalize(s));
                    total++;
                    if (total % 50000 == 0) {
                        System.out.println("Processed: " + total);
                    }
                    if (results.size() == 0) {
                        uniques.add(s);
                    }
                }
            }

            System.out.println("Total: " + total);
        }
        Stats st = new Stats(0.0002);
        st.allCounts = (int) uniques.totalCount();
        st.allUniques = uniques.size();
        for (String s : uniques.getSortedList()) {
            int count = uniques.getCount(s);
            if (count > 5) {
                st.significantCounts += count;
                st.significantUniques++;
                System.out.println(s + " : " + count);
            }
        }
        st.dump();
    }

    public static void main(String[] args) throws IOException {
        AmbiguityStats parser = new AmbiguityStats();
        Stopwatch w = Stopwatch.createStarted();
        //parser.ambiguousWordStats("File with turkish sentences");
        //parser.ambiguousGroupStats("File with turkish sentences");
        System.out.println(w.elapsed(TimeUnit.MILLISECONDS) + " ms.");
    }
}
