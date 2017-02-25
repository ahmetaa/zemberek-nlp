package zemberek.morphology.external;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.turkish.TurkicSeq;
import zemberek.core.turkish.TurkishAlphabet;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Runs the binary
 */
public class OflazerAnalyzerRunner {
    File xeroxToolRoot;
    File binaryFstFile;
    ProcessRunner runner;

    static Collator ctr = Collator.getInstance(new Locale("tr"));

    public OflazerAnalyzerRunner(File xeroxToolRoot, File binaryFstFile) {
        this.xeroxToolRoot = xeroxToolRoot;
        this.binaryFstFile = binaryFstFile;
        runner = new ProcessRunner(xeroxToolRoot);
    }

    static Pattern p = Pattern.compile("[ ]+");

    public void parseSentences(File input, File output) throws IOException {
        File temp = File.createTempFile("temp", ".txt");
        prepareForAnalysis(temp, SimpleTextReader.trimmingUTF8Reader(input).asStringList());
        runParser(temp, output);
    }

    public List<String> parseSentences(List<String> sentences) throws IOException {

        // prepare the file which contains a word per line.
        File f = new File("toMorph.txt");
        prepareForAnalysis(f, sentences);
        File out = new File("out.txt");
        runParser(f, out);
        // run the engine.
        return SimpleTextReader.trimmingUTF8Reader(out).asStringList();
    }

    private void prepareForAnalysis(File f, List<String> sentences) throws IOException {
        SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(f);
        for (String sentence : sentences) {
            for (String s : Splitter.on(p).omitEmptyStrings().split(sentence)) {
                stw.writeLine(s);
            }
            stw.writeLine("#");
        }
        stw.close();
    }

    public void runParser(File input, File output) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "./lookup",
                binaryFstFile.getAbsolutePath()
        );
        try {
            runner.execute(builder, input, output);
        } catch (InterruptedException e) {
            System.out.println("Operation interrupted unexpectedly!\n");
            e.printStackTrace();
        }
    }

    public static void cleanUnparseables(File input, File output) throws IOException {
        List<String> all = SimpleTextReader.trimmingUTF8Reader(input).asStringList();
        List<String> remains = Lists.newArrayList();
        for (String s : all) {
            if (s.contains("?") || s.contains("#"))
                continue;
            remains.add(s);
        }
        SimpleTextWriter.oneShotUTF8Writer(output).writeLines(remains);
    }

    public static void matchLines(File input, File output, String... keyWords) throws IOException {
        List<String> all = SimpleTextReader.trimmingUTF8Reader(input).asStringList();
        Set<String> remains = Sets.newHashSet();
        for (String s : all) {
            for (String keyWord : keyWords) {
                if (s.contains(keyWord)) {
                    remains.add(s);
                }
            }
        }
        List<String> sorted = Lists.newArrayList(remains);
        Collections.sort(sorted, Collator.getInstance(new Locale("tr")));
        SimpleTextWriter.oneShotUTF8Writer(output).writeLines(sorted);
    }

    public static void extractRootsFromParse(File input, File output) throws IOException {
        System.out.println("Extracting root words from parse list");
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        List<String> all = SimpleTextReader.trimmingUTF8Reader(input).asStringList();
        Set<String> roots = Sets.newHashSet();
        for (String s : all) {
            List<String> lst = Lists.newArrayList(Splitter.on("\t").split(s));
            String root = lst.get(1);
            if (root.contains("+")) {
                Iterator<String> iterator = Splitter.on("+").split(root).iterator();
                root = iterator.next();
                String pos = iterator.next();
                if (pos.equals("Verb")) {
                    if (new TurkicSeq(root, alphabet).lastVowel().isFrontal()) root = root + "mek";
                    else root = root + "mak";
                }
            }
            roots.add(root);
        }
        ArrayList<String> sorted = Lists.newArrayList(roots);
        Collections.sort(sorted, ctr);
        SimpleTextWriter.oneShotUTF8Writer(output).writeLines(sorted);
    }

    static Set<String> secondaryPosSet = Sets.newHashSet("Ordinal", "Cardinal", "Percentage", "Range", "Real",
            "Ratio", "Distribution", "Time", "Prop", "DemonsP", "QuesP", "ReflexP", "PersP", "QuantP");

    public static void extractDictItems(File input, File output) throws IOException {
        System.out.println("Extracting dict items from parse list");
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        List<String> all = SimpleTextReader.trimmingUTF8Reader(input).asStringList();
        Set<String> roots = Sets.newHashSet();
        for (String s : all) {
            s = s.replaceAll("\\^DB", "");
            List<String> lst = Lists.newArrayList(Splitter.on("\t").split(s));
            String root = lst.get(1);
            StringBuilder data = new StringBuilder();
            if (root.contains("+")) {
                Iterator<String> iterator = Splitter.on("+").split(root).iterator();
                root = iterator.next();
                String pos = iterator.next();
                String secPos = "";
                if (iterator.hasNext()) {
                    String c = iterator.next();
                    if (secondaryPosSet.contains(c))
                        secPos = c;
                }
                if (pos.equals("Verb")) {
                    if (new TurkicSeq(root, alphabet).lastVowel().isFrontal()) root = root + "mek";
                    else root = root + "mak";
                }
                data.append(root);
                if (!pos.equals("Noun") && !pos.equals("Verb")) {
                    if (pos.equals("Adverb"))
                        pos = "Adv";
                    data.append(" [P:").append(pos);
                    if (secPos.length() > 1)
                        data.append(" ,").append(secPos);
                    data.append("; A:Ext]");

                } else {
                    if (secPos.length() > 0)
                        data.append(" [P:").append(secPos).append("; A:Ext]");
                    else
                        data.append(" [A:Ext]");
                }

            } else data = new StringBuilder(root).append(" [A:Ext]");
            roots.add(data.toString());
        }
        ArrayList<String> sorted = Lists.newArrayList(roots);
        Collections.sort(sorted, ctr);
        SimpleTextWriter.oneShotUTF8Writer(output).writeLines(sorted);
    }

    public static void extractWords(File input, File output) throws IOException {
        System.out.println("Extracting words");
        List<String> all = SimpleTextReader.trimmingUTF8Reader(input).asStringList();
        Set<String> words = Sets.newHashSet();
        for (String s : all) {
            List<String> lst = Lists.newArrayList(Splitter.on("\t").split(s));
            String root = lst.get(0);
            words.add(root);
        }
        ArrayList<String> sorted = Lists.newArrayList(words);
        Collections.sort(sorted, ctr);
        SimpleTextWriter.oneShotUTF8Writer(output).writeLines(sorted);
    }
}
