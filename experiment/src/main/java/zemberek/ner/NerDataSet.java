package zemberek.ner;

import com.google.common.base.Splitter;
import org.antlr.v4.runtime.Token;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.Regexps;
import zemberek.core.text.TextUtil;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NerDataSet {

    Set<String> types = new HashSet<>();
    Set<String> typeIds = new HashSet<>();
    List<NerSentence> sentences = new ArrayList<>();

    public NerDataSet(List<NerSentence> sentences) {
        this.sentences = sentences;

        for (NerSentence sentence : sentences) {
            for (NerToken token : sentence.tokens) {
                types.add(token.type);
                typeIds.add(token.tokenId);
            }
        }
    }

    void addSet(NerDataSet set) {
        this.sentences.addAll(set.sentences);
        types.addAll(set.types);
        typeIds.addAll(set.typeIds);
    }

    public NerDataSet getSubSet(int from, int to) {
        return new NerDataSet(new ArrayList<>(sentences.subList(from, to)));
    }

    static class Info {
        int numberOfSentences;
        Set<String> types;
        Histogram<String> typeHistogram = new Histogram<>();
        Histogram<String> tokenHistogram = new Histogram<>();
        int numberOfTokens;

        Info(NerDataSet set) {
            this.types = set.types;
            this.numberOfSentences = set.sentences.size();
            for (NerSentence sentence : set.sentences) {
                numberOfTokens += sentence.tokens.size();
                for (NerToken token : sentence.tokens) {
                    tokenHistogram.add(token.type);
                    if (token.position == NePosition.OUTSIDE ||
                            token.position == NePosition.BEGIN ||
                            token.position == NePosition.UNIT) {
                        typeHistogram.add(token.type);
                    }
                }
            }
        }

        void log() {
            Log.info("Number of sentences      = %d", numberOfSentences);
            Log.info("Number of tokens         = %d", numberOfTokens);
            for (String type : typeHistogram.getSortedList()) {
                Log.info("Type = %s (Count = %d, Token Count = %d Av. Token = %.2f )",
                        type,
                        typeHistogram.getCount(type),
                        tokenHistogram.getCount(type),
                        tokenHistogram.getCount(type) * 1f / typeHistogram.getCount(type));
            }
        }
    }

    public static final String OUT_TOKEN_TYPE = "OUT";

    private static Pattern enamexNePattern = Pattern.compile(
            "((<b_enamex TYPE=\")(?<TYPE>.+?)(\">)(?<CONTENT>.+?)(<e_enamex>))");
    private static Pattern enamexNeSplitPattern = Pattern.compile(
            "((<b_enamex TYPE=\")(?<TYPE>.+?)(\">)(?<CONTENT>.+?)(<e_enamex>))|([^ ]+)");

    private static Pattern bracketNePattern = Pattern.compile(
            "((\\[)(?<TYPE>PER|ORG|LOC)( )(?<CONTENT>.+?)(]))");
    private static Pattern bracketNeSplitPattern = Pattern.compile(
            "((\\[)(?<TYPE>PER|ORG|LOC)( )(?<CONTENT>.+?)(]))|([^ ]+)");

    static NerDataSet loadBracketTurkishCorpus(Path path) throws IOException {
        return loadTurkishNERCorpus(path, bracketNePattern, bracketNeSplitPattern);
    }

    static NerDataSet loadEnamexTurkishNERCorpus(Path path) throws IOException {
        return loadTurkishNERCorpus(path, enamexNePattern, enamexNeSplitPattern);
    }

    static String normalizeForNer(String input) {
        input = input.toLowerCase(Turkish.LOCALE);
        List<String> result = new ArrayList<>();
        for (Token t : TurkishTokenizer.DEFAULT.tokenize(input)) {
            String s = t.getText();
            if (t.getType() == TurkishLexer.Date || t.getType() == TurkishLexer.Number || t.getType() == TurkishLexer.Time) {
                s = "*" + s.replaceAll("[0-9]", "D") + "*";
            }
            result.add(s);
        }
        return String.join("", result);
    }

    static NerDataSet loadTurkishNERCorpus(
            Path path,
            Pattern nePattern,
            Pattern splitPattern) throws IOException {

        Log.info("Extracting data from %s", path);

        List<String> lines = TextUtil.loadLinesWithText(path);

        List<NerSentence> nerSentences = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line.trim().length() < 2) {
                continue;
            }
            List<String> tokens = Regexps.allMatches(splitPattern, line);
            List<NerToken> nerTokens = new ArrayList<>(tokens.size());
            int index = 0;
            for (String token : tokens) {
                //combine apostrophe suffix to previous word.
                if (token.startsWith("'") && !token.endsWith("'")) {
                    nerTokens.get(index - 1).word = nerTokens.get(index - 1).word + token;
                    nerTokens.get(index - 1).normalized = nerTokens.get(index - 1).normalized + token;
                    continue;
                }

                // not a ner word.
                if (!nePattern.matcher(token).matches()) {
                    nerTokens.add(new NerToken(index, token, normalizeForNer(token), OUT_TOKEN_TYPE, NePosition.OUTSIDE));
                    index++;
                    continue;
                }
                Matcher matcher = nePattern.matcher(token);
                if (matcher.find()) {
                    String type = matcher.group("TYPE");
                    String content = matcher.group("CONTENT");
                    List<String> neWords = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(content);
                    for (int i = 0; i < neWords.size(); i++) {
                        String s = neWords.get(i);
                        NePosition position;
                        if (neWords.size() == 1) {
                            position = NePosition.UNIT;
                        } else if (i == 0) {
                            position = NePosition.BEGIN;
                        } else if (i == neWords.size() - 1) {
                            position = NePosition.LAST;
                        } else {
                            position = NePosition.INSIDE;
                        }
                        nerTokens.add(new NerToken(index, s, normalizeForNer(s), type, position));
                        index++;
                    }
                }
            }
            nerSentences.add(new NerSentence(line, nerTokens));
        }
        return new NerDataSet(nerSentences);
    }
}
