package zemberek.morphology.analyzer;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.Token;
import zemberek.core.io.Strings;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.core.turkish._TurkishAlphabet;
import zemberek.morphology.analysis.tr.TurkishNumbers;
import zemberek.morphology.analysis.tr.TurkishNumeralEndingMachine;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.antlr.TurkishLexer;

//TODO: Code requires serious testing and review.
//TODO: For unknown pronouns, do not analyze as regular nouns if apostrophe is not in the
//        correct place. Such as [obama'ymış] should not have "oba" root solution.

public class UnidentifiedTokenAnalyzer {

  private static Map<String, String> ordinalMap = TurkishNumbers.getOrdinalMap();

  private InterpretingAnalyzer analyzer;
  private _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
  private TurkishNumeralEndingMachine numeralEndingMachine = new TurkishNumeralEndingMachine();

  public UnidentifiedTokenAnalyzer(InterpretingAnalyzer analyzer) {
    this.analyzer = analyzer;
  }


  public static final Pattern nonLettersPattern =
      Pattern.compile("[^" + _TurkishAlphabet.INSTANCE.getAllLetters() + "]");

  public synchronized List<SingleAnalysis> analyze(Token token) {

    SecondaryPos sPos = guessSecondaryPosType(token);
    String word = token.getText();

    // TODO: for now, for regular words and numbers etc, use the analyze method.
    if (sPos == SecondaryPos.None) {
      return analyze(word);
    }

    //TODO: consider returning analysis results without interfering with analyzer.
    String normalized = nonLettersPattern.matcher(word).replaceAll("");
    DictionaryItem item = new DictionaryItem(word, word, normalized, PrimaryPos.Noun, sPos);
    analyzer.getStemTransitions().addDictionaryItem(item);
    return analyzer.analyze(normalized);
  }

  private SecondaryPos guessSecondaryPosType(Token token) {
    switch (token.getType()) {
      case TurkishLexer.Email:
        return SecondaryPos.Email;
      case TurkishLexer.URL:
        return SecondaryPos.Url;
      case TurkishLexer.HashTag:
        return SecondaryPos.HashTag;
      case TurkishLexer.Mention:
        return SecondaryPos.Mention;
      case TurkishLexer.Emoticon:
        return SecondaryPos.Emoticon;
      case TurkishLexer.RomanNumeral:
        return SecondaryPos.RomanNumeral;
      case TurkishLexer.AbbreviationWithDots:
        return SecondaryPos.Abbreviation;

      default:
        return SecondaryPos.None;
    }
  }

  public synchronized List<SingleAnalysis> analyze(String word) {
    if (word.contains("?")) {
      return Collections.emptyList();
    }
    if (alphabet.containsDigit(word)) {
      return tryNumeral(word);
    }
    int index = word.indexOf('\'');
    if (index >= 0) {
      return tryWordWithApostrophe(word);
    } else if (Character.isUpperCase(word.charAt(0))) {
      return tryWithoutApostrophe(word);
    }
    return Collections.emptyList();
  }

  private List<SingleAnalysis> tryWithoutApostrophe(String word) {
    String normalized = TurkishAlphabet.INSTANCE.normalize(word);
    //TODO: should we remove dots with normalization?
    String pronunciation = guessPronunciation(normalized.replaceAll("[.]", ""));
    DictionaryItem itemProp = new DictionaryItem(
        Turkish.capitalize(normalized),
        normalized,
        pronunciation,
        PrimaryPos.Noun,
        normalized.contains(".") ? SecondaryPos.Abbreviation : SecondaryPos.ProperNoun);
    itemProp.attributes.add(RootAttribute.Runtime);
    analyzer.getStemTransitions().addDictionaryItem(itemProp);
    //TODO eliminate gross code duplication
    List<SingleAnalysis> properResults = analyzer.analyze(normalized);
    analyzer.getStemTransitions().removeDictionaryItem(itemProp);
    return properResults;
  }

  private List<SingleAnalysis> tryWordWithApostrophe(String word) {
    int index = word.indexOf('\'');
    if (index < 0 || index == 0 || index == word.length() - 1) {
      return Collections.emptyList();
    }
    String stem = word.substring(0, index);
    String ending = word.substring(index + 1);

    StemAndEnding se = new StemAndEnding(stem, ending);
    //TODO: should we remove dots with normalization?
    String stemNormalized = TurkishAlphabet.INSTANCE.normalize(se.stem).replaceAll("[.]", "");
    String endingNormalized = TurkishAlphabet.INSTANCE.normalize(se.ending);
    String pronunciation = guessPronunciation(stemNormalized);
    DictionaryItem itemProp = new DictionaryItem(
        Turkish.capitalize(stemNormalized),
        stemNormalized,
        pronunciation,
        PrimaryPos.Noun,
        SecondaryPos.ProperNoun);
    itemProp.attributes.add(RootAttribute.Runtime);
    analyzer.getStemTransitions().addDictionaryItem(itemProp);
    String toParse = stemNormalized + endingNormalized;
    List<SingleAnalysis> properResults = analyzer.analyze(toParse);
    analyzer.getStemTransitions().removeDictionaryItem(itemProp);
    return properResults;
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

  private List<SingleAnalysis> tryNumeral(String s) {
    StemAndEnding se = getFromNumeral(s);
    String lemma;
    if (se.stem.endsWith(".")) {
      String ss = se.stem.substring(0, se.stem.length() - 1);
      lemma = numeralEndingMachine.find(ss);
      lemma = ordinalMap.get(lemma);
    } else {
      lemma = numeralEndingMachine.find(se.stem);
    }

    List<SingleAnalysis> results = Lists.newArrayListWithCapacity(1);

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
        List<SingleAnalysis> res = analyzer.analyze(toParse);
        for (SingleAnalysis re : res) {
          if (re.getDictionaryItem().primaryPos != PrimaryPos.Numeral) {
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
