package zemberek.morphology.external;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import zemberek.core.io.LineIterator;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DisambiguatorPreprocessor {
    static Set<String> punctuations = Sets.newHashSet("", "...", "!", "?", ":", ";", ",", "\"", "'");

    public void processOflazerAnalyzerOutputSak(File oflazerResult, File out) throws IOException {
        SimpleTextWriter sakFile = SimpleTextWriter.keepOpenUTF8Writer(out);
        LineIterator li = new SimpleTextReader(oflazerResult, "UTF-8").getLineIterator();
        boolean sentenceStarted = false;
        List<String> parses = Lists.newArrayList();
        while (li.hasNext()) {
            String line = li.next().trim();
            String word = Strings.subStringUntilFirst(line, "\t").trim();
            if (line.length() == 0 && !sentenceStarted)
                continue;
            if (line.length() == 0 && parses.size() > 0) {
                sakFile.writeLine(processParses(parses));
                parses = Lists.newArrayList();
            }
            if (line.length() > 0) {
                if (parses.size() == 0) {
                    if (!sentenceStarted)
                        sakFile.writeLine("<S>");
                    sentenceStarted = true;
                }
                if (punctuations.contains(word)) {
                    // because analyser i use does not parse punctuations. i do it myself.
                    parses.add(word + "\t" + word + "\t+Punc");
                } else if (!line.endsWith("?"))
                    parses.add(line);
                else if (!word.equals("#")) {
                    String inferred = inferUnknownWordParse(word);
                    System.out.println("Bad word: [" + line + "] inferred to [" + inferred + "]");
                    parses.add(inferred);
                }
            }
            if (word.equals("#")) {
                sentenceStarted = false;
                sakFile.writeLine("#\t#\t+Punc");
                sakFile.writeLine("</S>");
                parses = new ArrayList<String>();
            }
        }
        sakFile.close();
    }

    public void processOflazerAnalyzerOutputYuret(File oflazerResult, File out) throws IOException {
        SimpleTextWriter yuretFileWriter = SimpleTextWriter.keepOpenWriter(new FileOutputStream(out), "ISO-8859-9");
        yuretFileWriter.writeLine("<DOC>\t<DOC>");
        yuretFileWriter.writeLine();
        LineIterator li = new SimpleTextReader(oflazerResult, "UTF-8").getLineIterator();
        boolean sentenceStarted = false;
        List<String> parses = new ArrayList<String>();
        while (li.hasNext()) {
            String line = li.next().trim().replaceAll("AorPart", "PresPart");

            String word = Strings.subStringUntilFirst(line, "\t").trim();
            if (line.length() == 0 && !sentenceStarted)
                continue;
            if (line.length() == 0 && parses.size() > 0) {
                yuretFileWriter.writeLines(parses);
                yuretFileWriter.writeLine();
                yuretFileWriter.writeLine();
                parses = Lists.newArrayList();
            }
            if (line.length() > 0) {
                if (parses.size() == 0) {
                    if (!sentenceStarted) {
                        yuretFileWriter.writeLine("<S>\t<S>");
                        yuretFileWriter.writeLine();
                    }
                    sentenceStarted = true;
                }
                if (punctuations.contains(word)) {
                    // because analyser i use does not parse punctuations. i do it myself.
                    parses.add(word + "\t" + word + "\t+Punc");
                } else if (!line.endsWith("?"))
                    parses.add(line);
                else if (!word.equals("#")) {
                    String inferred = inferUnknownWordParse(word);
                    System.out.println("Bad word: [" + line + "] inferred to [" + inferred + "]");
                    parses.add(inferred);
                }
            }
            if (word.equals("#")) {
                sentenceStarted = false;
                yuretFileWriter.writeLine("</S>\t</S>\n");
                parses = new ArrayList<String>();
            }
        }
        yuretFileWriter.writeLine("</DOC>\t</DOC>");
        yuretFileWriter.close();
    }


    private String inferUnknownWordParse(String word) {
        if (Character.isUpperCase(word.charAt(0)))
            return word + "\t" + word + "\t+Noun+Prop+A3sg+Pnon+Nom";
        else
            return word + "\t" + word + "\t+Noun+A3sg+Pnon+Nom";
    }

    private String processParses(List<String> parses) {
        if (parses.size() == 0)
            throw new IllegalArgumentException("Zero parse result is not acceptable.");
        String word = Strings.subStringUntilFirst(parses.get(0), "\t");
        StringBuilder sb = new StringBuilder(word).append(" ");
        for (String parse : parses) {
            int i = 0;
            for (String token : Splitter.on("\t").split(parse)) {
                if (i == 0) {
                    i++;
                    continue;
                }
                sb.append(token);
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}
