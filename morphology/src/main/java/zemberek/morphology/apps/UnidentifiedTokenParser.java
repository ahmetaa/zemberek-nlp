package zemberek.morphology.apps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import zemberek.core.io.Strings;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.SimpleParser;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.morphology.structure.Turkish;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class UnidentifiedTokenParser extends BaseParser {
    SimpleParser parser;
    DynamicLexiconGraph graph;
    TurkishMorphParser turkishParser;


    public UnidentifiedTokenParser(TurkishMorphParser turkishParser) {
        this.turkishParser = turkishParser;
        // generate a parser with an empty graph.
        SuffixProvider suffixProvider = new TurkishSuffixes();
        this.graph = new DynamicLexiconGraph(suffixProvider);
        this.parser = new SimpleParser(graph);
    }

    public List<MorphParse> parse(String word) {
        List<MorphParse> results = Lists.newArrayList();
        if (!Strings.containsNone(word, "0123456789")) {
            results = parseNumeral(word);
            return results;
        }
        if (word.contains("'")) {
            StemAndEnding se = new StemAndEnding(Strings.subStringUntilFirst(word, "'"), Strings.subStringAfterFirst(word, "'"));
            String stem = normalize(se.stem);
            String ending = normalize(se.ending);
            String pron = guessPronunciation(stem);
            DictionaryItem itemProp = new DictionaryItem(Turkish.capitalize(stem), stem, pron, PrimaryPos.Noun, SecondaryPos.ProperNoun);
            String toParse = stem + ending;
            StemNode[] nodes = graph.addDictionaryItem(itemProp);
            parser.addNodes(nodes);
            List<MorphParse> properResults = parser.parse(toParse);
            graph.removeStemNodes(nodes);
            parser.removeStemNodes(nodes);
            results.addAll(properResults);

        } else if (Character.isUpperCase(word.charAt(0))) {
            String normalized = normalize(word);
            String pron = guessPronunciation(normalized);
            DictionaryItem itemProp = new DictionaryItem(Turkish.capitalize(normalized), normalized, pron, PrimaryPos.Noun, SecondaryPos.ProperNoun);
            StemNode[] nodes = graph.addDictionaryItem(itemProp);
            parser.addNodes(nodes);
            //TODO eliminate gross code duplication
            List<MorphParse> properResults = parser.parse(normalized);
            graph.removeStemNodes(nodes);
            parser.removeStemNodes(nodes);
            results.addAll(properResults);
        }
        return results;
    }

    private String guessPronunciation(String stem) {
        String pron = stem;
        if (!Turkish.Alphabet.hasVowel(stem)) {
            pron = Turkish.inferPronunciation(stem);
        }
        return pron;
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

    TurkishNumeralEndingMachine numeralEndingMachine = new TurkishNumeralEndingMachine();

    static Map<String, String> ordinalMap = Maps.newHashMap();

    static {
        String[] cardinals = "sıfır,bir,iki,üç,dört,beş,altı,yedi,sekiz,dokuz,on,yirmi,otuz,kırk,elli,altmış,yetmiş,seksen,doksan,yüz,bin,milyon,milyar,trilyon,katrilyon".split("[,]");
        String[] ordinals = "sıfırıncı,birinci,ikinci,üçüncü,dördüncü,beşinci,altıncı,yedinci,sekizinci,dokuzuncu,onuncu,yirminci,otuzuncu,kırkıncı,ellinci,altmışıncı,yetmişinci,sekseninci,doksanıncı,yüzüncü,bininci,milyonuncu,milyarıncı,trilyonuncu,katrilyonuncu".split("[,]");
        for (int i = 0; i < cardinals.length; i++) {
            ordinalMap.put(cardinals[i], ordinals[i]);
        }
    }

    public List<MorphParse> parseNumeral(String s) {

        StemAndEnding se = getFromNumeral(s);
        String lemma;
        if (se.stem.endsWith(".")) {
            String ss = se.stem.substring(0, se.stem.length() - 1);
            lemma = numeralEndingMachine.find(ss);
            lemma = ordinalMap.get(lemma);
        } else lemma = numeralEndingMachine.find(se.stem);
        List<MorphParse> results = Lists.newArrayListWithCapacity(1);
        for (TurkishDictionaryLoader.Digit digit : TurkishDictionaryLoader.Digit.values()) {
            Matcher m = digit.pattern.matcher(se.stem);
            if (m.find()) {
                String toParse;
                if (se.ending.length() > 0 && lemma.equals("dört") && alphabet.isVowel(se.ending.charAt(0)))
                    toParse = "dörd" + se.ending;
                else
                    toParse = lemma + se.ending;
                List<MorphParse> res = turkishParser.parse(toParse);
                for (MorphParse re : res) {
                    if (re.dictionaryItem.primaryPos != PrimaryPos.Numeral)
                        continue;
                    re.dictionaryItem = new DictionaryItem(se.stem, se.stem, lemma, PrimaryPos.Numeral, digit.spos);
                    re.root = se.stem;
                    results.add(re);
                }
            }
        }
        return results;
    }
}


