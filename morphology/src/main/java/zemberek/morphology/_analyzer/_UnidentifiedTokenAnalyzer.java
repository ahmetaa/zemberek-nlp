package zemberek.morphology._analyzer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import zemberek.core.io.Strings;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish._TurkishAlphabet;
import zemberek.morphology.analysis.tr.TurkishNumeralEndingMachine;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.morphology.structure.Turkish;

//TODO: Code requires serious testing and review.
//TODO: For unknown pronouns, do not analyze as regular nouns if apostrophe is not in the
//        correct place. Such as [obama'ymış] should not have "oba" root solution.

public class _UnidentifiedTokenAnalyzer {

  private static Map<String, String> ordinalMap = Maps.newHashMap();

  static {
    String[] cardinals = ("sıfır,bir,iki,üç,dört,beş,altı,yedi,sekiz,dokuz,on,yirmi,otuz,kırk,elli,"
        +
        "altmış,yetmiş,seksen,doksan,yüz,bin,milyon,milyar,trilyon,katrilyon").split("[,]");
    String[] ordinals = (
        "sıfırıncı,birinci,ikinci,üçüncü,dördüncü,beşinci,altıncı,yedinci,sekizinci,dokuzuncu," +
            "onuncu,yirminci,otuzuncu,kırkıncı,ellinci,altmışıncı,yetmişinci,sekseninci,doksanıncı,yüzüncü,"
            +
            "bininci,milyonuncu,milyarıncı,trilyonuncu,katrilyonuncu").split("[,]");
    for (int i = 0; i < cardinals.length; i++) {
      ordinalMap.put(cardinals[i], ordinals[i]);
    }
  }

  private InterpretingAnalyzer analyzer;
  private _TurkishAlphabet alphabet;
  private TurkishNumeralEndingMachine numeralEndingMachine = new TurkishNumeralEndingMachine();

  public _UnidentifiedTokenAnalyzer(InterpretingAnalyzer analyzer) {
    this.analyzer = analyzer;
  }

  public synchronized List<_SingleAnalysis> analyze(String word) {
    if (word.contains("?")) {
      return Collections.emptyList();
    }
    if (alphabet.containsDigit(word)) {
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
      analyzer.addDictionaryItem(itemProp);
      String toParse = stem + ending;
      List<_SingleAnalysis> properResults = analyzer.analyze(toParse);
      analyzer.removeDictionaryItem(itemProp);
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
      analyzer.addDictionaryItem(itemProp);
      //TODO eliminate gross code duplication
      List<_SingleAnalysis> properResults = analyzer.analyze(normalized);
      analyzer.removeDictionaryItem(itemProp);
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

  private StemAndEnding getFromNumeral(String s) {
    if (s.contains("'")) {
      return new StemAndEnding(Strings.subStringUntilFirst(s, "'"),
          Strings.subStringAfterFirst(s, "'"));
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
      } else {
        break;
      }
    }
    int cutPoint = s.length() - j;
    return new StemAndEnding(s.substring(0, cutPoint), s.substring(cutPoint));
  }

  private List<_SingleAnalysis> parseNumeral(String s) {
    StemAndEnding se = getFromNumeral(s);
    String lemma;
    if (se.stem.endsWith(".")) {
      String ss = se.stem.substring(0, se.stem.length() - 1);
      lemma = numeralEndingMachine.find(ss);
      lemma = ordinalMap.get(lemma);
    } else {
      lemma = numeralEndingMachine.find(se.stem);
    }

    List<_SingleAnalysis> results = Lists.newArrayListWithCapacity(1);

    for (TurkishDictionaryLoader.Digit digit : TurkishDictionaryLoader.Digit.values()) {
      Matcher m = digit.pattern.matcher(se.stem);
      if (m.find()) {
        String toParse;
        if (se.ending.length() > 0 && lemma.equals("dört") && TurkishAlphabet.INSTANCE
            .isVowel(se.ending.charAt(0))) {
          toParse = "dörd" + se.ending;
        } else {
          toParse = lemma + se.ending;
        }
        List<_SingleAnalysis> res = analyzer.analyze(toParse);
        for (_SingleAnalysis re : res) {
          if (re.getItem().primaryPos != PrimaryPos.Numeral) {
            continue;
          }
          DictionaryItem runTimeItem = new DictionaryItem(
              se.stem,
              se.stem,
              s + lemma,
              PrimaryPos.Numeral,
              digit.secondaryPos);
          runTimeItem.attributes.add(RootAttribute.Runtime);
          results.add(re.copyFor(runTimeItem, se.stem));
        }
      }
    }
    return results;
  }

}
