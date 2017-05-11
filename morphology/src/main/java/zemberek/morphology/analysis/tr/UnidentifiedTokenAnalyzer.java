package zemberek.morphology.analysis.tr;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import zemberek.core.io.Strings;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalyzer;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.morphology.structure.Turkish;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class UnidentifiedTokenAnalyzer extends BaseParser {
    WordAnalyzer parser;
    DynamicLexiconGraph graph;
    TurkishMorphology turkishParser;


    public UnidentifiedTokenAnalyzer(TurkishMorphology turkishParser) {
        this.turkishParser = turkishParser;
        // generate a parser with an empty graph.
        SuffixProvider suffixProvider = new TurkishSuffixes();
        this.graph = new DynamicLexiconGraph(suffixProvider);
        this.parser = new WordAnalyzer(graph);
    }

    public synchronized List<WordAnalysis> analyze(String word) {
        if (word.contains("?")) {
            return Collections.emptyList();
        }
        if (!Strings.containsNone(word, "0123456789")) {
            return parseNumeral(word);
        }
        int index = word.indexOf('\'');
        if (index >= 0) {

            if (index == 0 || index == word.length() - 1) {
                return Collections.emptyList();
            }
            StemAndEnding se = new StemAndEnding(word.substring(0, index), word.substring(index + 1));
            String stem = TurkishAlphabet.INSTANCE.normalize(se.stem);
            String ending = TurkishAlphabet.INSTANCE.normalize(se.ending);
            String pronunciation = guessPronunciation(stem);
            DictionaryItem itemProp = new DictionaryItem(
                    Turkish.capitalize(stem),
                    stem,
                    pronunciation,
                    PrimaryPos.Noun,
                    SecondaryPos.ProperNoun);
            itemProp.attributes.add(RootAttribute.Runtime);
            graph.addDictionaryItem(itemProp);
            String toParse = stem + ending;
            List<WordAnalysis> properResults = parser.analyze(toParse);
            graph.removeDictionaryItem(itemProp);
            return properResults;
        } else if (Character.isUpperCase(word.charAt(0))) {
            String normalized = TurkishAlphabet.INSTANCE.normalize(word);
            String pronunciation = guessPronunciation(normalized);
            DictionaryItem itemProp = new DictionaryItem(
                    Turkish.capitalize(normalized),
                    normalized,
                    pronunciation,
                    PrimaryPos.Noun,
                    SecondaryPos.ProperNoun);
            itemProp.attributes.add(RootAttribute.Runtime);
            graph.addDictionaryItem(itemProp);
            //TODO eliminate gross code duplication
            List<WordAnalysis> properResults = parser.analyze(normalized);
            graph.removeDictionaryItem(itemProp);
            return properResults;
        }
        return Collections.emptyList();
    }

    private String guessPronunciation(String stem) {
        if (!Turkish.Alphabet.hasVowel(stem)) {
            return Turkish.inferPronunciation(stem);
        } else {
            return stem;
        }
    }

    public StemAndEnding getFromNumeral(String s) {
        if (s.contains("'")) {
            return new StemAndEnding(Strings.subStringUntilFirst(s, "'"), Strings.subStringAfterFirst(s, "'"));
        }
        int j = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            int k = c - '0';
            if (c == '.') { // ordinal
                break;
            }
            if (k < 0 || k > 9) {
                j++;
            } else
                break;
        }
        int cutPoint = s.length() - j;
        return new StemAndEnding(s.substring(0, cutPoint), s.substring(cutPoint));
    }

    private TurkishNumeralEndingMachine numeralEndingMachine = new TurkishNumeralEndingMachine();

    private static Map<String, String> ordinalMap = Maps.newHashMap();

    static {
        String[] cardinals = ("sıfır,bir,iki,üç,dört,beş,altı,yedi,sekiz,dokuz,on,yirmi,otuz,kırk,elli," +
                "altmış,yetmiş,seksen,doksan,yüz,bin,milyon,milyar,trilyon,katrilyon").split("[,]");
        String[] ordinals = ("sıfırıncı,birinci,ikinci,üçüncü,dördüncü,beşinci,altıncı,yedinci,sekizinci,dokuzuncu," +
                "onuncu,yirminci,otuzuncu,kırkıncı,ellinci,altmışıncı,yetmişinci,sekseninci,doksanıncı,yüzüncü," +
                "bininci,milyonuncu,milyarıncı,trilyonuncu,katrilyonuncu").split("[,]");
        for (int i = 0; i < cardinals.length; i++) {
            ordinalMap.put(cardinals[i], ordinals[i]);
        }
    }

    public List<WordAnalysis> parseNumeral(String s) {
        StemAndEnding se = getFromNumeral(s);
        String lemma;
        if (se.stem.endsWith(".")) {
            String ss = se.stem.substring(0, se.stem.length() - 1);
            lemma = numeralEndingMachine.find(ss);
            lemma = ordinalMap.get(lemma);
        } else {
            lemma = numeralEndingMachine.find(se.stem);
        }
        List<WordAnalysis> results = Lists.newArrayListWithCapacity(1);
        for (TurkishDictionaryLoader.Digit digit : TurkishDictionaryLoader.Digit.values()) {
            Matcher m = digit.pattern.matcher(se.stem);
            if (m.find()) {
                String toParse;
                if (se.ending.length() > 0 && lemma.equals("dört") && TurkishAlphabet.INSTANCE.isVowel(se.ending.charAt(0)))
                    toParse = "dörd" + se.ending;
                else
                    toParse = lemma + se.ending;
                List<WordAnalysis> res = turkishParser.getWordAnalyzer().analyze(toParse);
                for (WordAnalysis re : res) {
                    if (re.dictionaryItem.primaryPos != PrimaryPos.Numeral) {
                        continue;
                    }
                    re.dictionaryItem = new DictionaryItem(
                            se.stem,
                            se.stem,
                            s + lemma,
                            PrimaryPos.Numeral,
                            digit.secondaryPos);
                    re.dictionaryItem.attributes.add(RootAttribute.Runtime);
                    re.root = se.stem;
                    results.add(re);
                }
            }
        }
        return results;
    }
}


