package zemberek.morphology.analysis;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.StemAndEnding;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.tr.PronunciationGuesser;
import zemberek.morphology.analysis.tr.TurkishNumbers;
import zemberek.morphology.analysis.tr.TurkishNumeralEndingMachine;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.tokenization.Token;


//TODO: Code requires serious testing and review.
//TODO: For unknown pronouns, do not analyze as regular nouns if apostrophe is not in the
//        correct place. Such as [obama'ymış] should not have "oba" root solution.

public class UnidentifiedTokenAnalyzer {

  public static final TurkishAlphabet ALPHABET = TurkishAlphabet.INSTANCE;
  private static Map<String, String> ordinalMap = TurkishNumbers.getOrdinalMap();

  private RuleBasedAnalyzer analyzer;
  private RootLexicon lexicon;
  private TurkishNumeralEndingMachine numeralEndingMachine = new TurkishNumeralEndingMachine();

  public UnidentifiedTokenAnalyzer(RuleBasedAnalyzer analyzer) {
    this.analyzer = analyzer;
    this.lexicon = analyzer.getLexicon();
  }

  public synchronized List<SingleAnalysis> analyze(Token token) {
    SecondaryPos sPos = guessSecondaryPosType(token);
    String word = token.getText();

    // TODO: for now, for regular words and numbers etc, use the analyze method.
    if (sPos == SecondaryPos.None) {
      if (word.contains("?")) {
        return Collections.emptyList();
      }
      if (ALPHABET.containsDigit(word)) {
        List<SingleAnalysis> numeralAnalyses = tryNumeral(token);
        if (!numeralAnalyses.isEmpty()) {
          return numeralAnalyses;
        }
      }
      return analyzeWord(word,
          word.contains(".") ? SecondaryPos.Abbreviation : SecondaryPos.ProperNoun);
    }

    if (sPos == SecondaryPos.RomanNumeral) {
      return getForRomanNumeral(token);
    }
    if (sPos == SecondaryPos.Date || sPos == SecondaryPos.Clock) {
      return tryNumeral(token);
    }

    if (sPos == SecondaryPos.HashTag ||
        sPos == SecondaryPos.Email ||
        sPos == SecondaryPos.Url ||
        sPos == SecondaryPos.Mention) {
      return analyzeWord(word, sPos);
    }

    //TODO: consider returning analysis results without interfering with analyzer.
    String normalized = stripNonLetters(word);
    DictionaryItem item = new DictionaryItem(word, word, normalized, PrimaryPos.Noun, sPos);

    boolean itemDoesNotExist = !lexicon.containsItem(item);
    if (itemDoesNotExist) {
      item.attributes.add(RootAttribute.Runtime);
      analyzer.getStemTransitions().addDictionaryItem(item);
    }
    List<SingleAnalysis> results = analyzer.analyze(word);
    if (itemDoesNotExist) {
      analyzer.getStemTransitions().removeDictionaryItem(item);
    }
    return results;
  }

  private static String stripNonLetters(String word) {
    StringBuilder sb = new StringBuilder(word.length());
    for (int i = 0; i < word.length(); i++) {
      char c = word.charAt(i);
      if (ALPHABET.isDictionaryLetter(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private SecondaryPos guessSecondaryPosType(Token token) {
    switch (token.getType()) {
      case Email:
        return SecondaryPos.Email;
      case URL:
        return SecondaryPos.Url;
      case HashTag:
        return SecondaryPos.HashTag;
      case Mention:
        return SecondaryPos.Mention;
      case Emoji:
      case Emoticon:
        return SecondaryPos.Emoticon;
      case RomanNumeral:
        return SecondaryPos.RomanNumeral;
      case Abbreviation:
        return SecondaryPos.Abbreviation;
      case Date:
        return SecondaryPos.Date;
      case Time: // TODO: consider SecondaryPos.Time -> Temporal and Clock -> Time
        return SecondaryPos.Clock;

      default:
        return SecondaryPos.None;
    }
  }


  public synchronized List<SingleAnalysis> analyzeWord(String word, SecondaryPos secondaryPos) {
    int index = word.indexOf('\'');
    if (index >= 0) {
      return tryWordWithApostrophe(word, secondaryPos);
    } else if (secondaryPos == SecondaryPos.ProperNoun
        || secondaryPos == SecondaryPos.Abbreviation) {
      // TODO: should we allow analysis of unknown words that starts wıth a capital letter
      // without apostrophe as Proper nouns?
      return Collections.emptyList();
    } else {
      return tryWithoutApostrophe(word, secondaryPos);
    }
  }

  private List<SingleAnalysis> tryWithoutApostrophe(String word, SecondaryPos secondaryPos) {
    String normalized = null;
    if (ALPHABET.containsForeignDiacritics(word)) {
      normalized = ALPHABET.foreignDiacriticsToTurkish(word);
    }
    normalized = normalized == null ?
        ALPHABET.normalize(word) :
        ALPHABET.normalize(normalized);

    boolean capitalize = secondaryPos == SecondaryPos.ProperNoun ||
        secondaryPos == SecondaryPos.Abbreviation;

    //TODO: should we remove dots with normalization?
    String pronunciation = guessPronunciation(normalized.replace(".", ""));

    DictionaryItem item = new DictionaryItem(
        capitalize ? Turkish.capitalize(normalized) : normalized,
        normalized,
        pronunciation,
        PrimaryPos.Noun,
        secondaryPos);

    if (!ALPHABET.containsVowel(pronunciation)) {
      List<SingleAnalysis> result = new ArrayList<>(1);
      result.add(SingleAnalysis.dummy(word, item));
      return result;
    }

    boolean itemDoesNotExist = !lexicon.containsItem(item);

    if (itemDoesNotExist) {
      item.attributes.add(RootAttribute.Runtime);
      analyzer.getStemTransitions().addDictionaryItem(item);
    }
    List<SingleAnalysis> results = analyzer.analyze(normalized);
    if (itemDoesNotExist) {
      analyzer.getStemTransitions().removeDictionaryItem(item);
    }
    return results;
  }

  private List<SingleAnalysis> tryWordWithApostrophe(String word, SecondaryPos secondaryPos) {
    String normalized = ALPHABET.normalizeApostrophe(word);

    int index = normalized.lastIndexOf('\'');
    if (index <= 0 || index == normalized.length() - 1) {
      return Collections.emptyList();
    }

    String stem = normalized.substring(0, index);
    String ending = normalized.substring(index + 1);

    StemAndEnding se = new StemAndEnding(stem, ending);
    //TODO: should we remove dots with normalization?
    String cleanStem = se.stem.replace("'", "");
    boolean alphanumericStem =
        (secondaryPos == SecondaryPos.ProperNoun || secondaryPos == SecondaryPos.Abbreviation)
            && ALPHABET.containsDigit(cleanStem);
    String stemNormalized = alphanumericStem
        ? ALPHABET.normalizeCircumflex(cleanStem.toLowerCase(Turkish.LOCALE)).replace(".", "")
        : ALPHABET.normalize(cleanStem).replace(".", "");
    String endingNormalized = ALPHABET.normalize(se.ending);
    String pronunciation = guessStemPronunciation(cleanStem, stemNormalized, secondaryPos);

    boolean capitalize = secondaryPos == SecondaryPos.ProperNoun ||
        secondaryPos == SecondaryPos.Abbreviation;

    boolean pronunciationPossible = ALPHABET.containsVowel(pronunciation);

    DictionaryItem item = new DictionaryItem(
        capitalize ? Turkish.capitalize(stem) : (pronunciationPossible ? stem : word),
        stemNormalized,
        pronunciation,
        PrimaryPos.Noun,
        secondaryPos);

    if (!pronunciationPossible) {
      List<SingleAnalysis> result = new ArrayList<>(1);
      result.add(SingleAnalysis.dummy(word, item));
      return result;
    }

    boolean itemDoesNotExist = !lexicon.containsItem(item);
    if (itemDoesNotExist) {
      item.attributes.add(RootAttribute.Runtime);
      analyzer.getStemTransitions().addDictionaryItem(item);
    }
    String toParse = stemNormalized + endingNormalized;

    List<SingleAnalysis> noQuotesParses = analyzer.analyze(toParse);

    if (itemDoesNotExist) {
      analyzer.getStemTransitions().removeDictionaryItem(item);
    }

    List<SingleAnalysis> analyses = noQuotesParses.stream()
        .filter(noQuotesParse -> noQuotesParse.getStem().equals(stemNormalized))
        .collect(Collectors.toList());

    return analyses;
  }

  PronunciationGuesser guesser = new PronunciationGuesser();

  private String guessPronunciation(String stem) {
    if (!ALPHABET.containsVowel(stem)) {
      return guesser.toTurkishLetterPronunciations(stem);
    } else {
      return guesser.replaceEnglishSpecificChars(stem);
    }
  }

  /**
   * Estimates the Turkish pronunciation of an unidentified stem before suffix analysis so that
   * vowel harmony and consonant voicing match how the stem is read aloud:
   * <ul>
   *   <li>Alphanumeric stems (e.g. {@code "S3"}, {@code "3G"}, {@code "F-16"}) are expanded using
   *       Turkish letter and number pronunciations.</li>
   *   <li>Dot-separated stems ending with a vowelless segment (e.g. {@code ".tr"} in
   *       {@code "meb.gov.tr"}) pronounce the trailing segment letter-by-letter.</li>
   *   <li>Abbreviations (e.g. {@code "Ö.D."}) use abbreviation syllable/letter pronunciation rules.</li>
   *   <li>All other stems fall back to {@link #guessPronunciation(String)}.</li>
   * </ul>
   */
  private String guessStemPronunciation(
      String rawStem, String stemNormalized, SecondaryPos secondaryPos) {
    if ((secondaryPos == SecondaryPos.ProperNoun || secondaryPos == SecondaryPos.Abbreviation)
        && ALPHABET.containsDigit(rawStem)) {
      return guesser.toTurkishLetterPronunciations(
          rawStem.toLowerCase(Turkish.LOCALE).replace(".", ""));
    }
    int lastDot = rawStem.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < rawStem.length() - 1) {
      String lastSegment = ALPHABET.normalize(rawStem.substring(lastDot + 1));
      if (!lastSegment.isEmpty() && !ALPHABET.containsVowel(lastSegment)) {
        String prefix = ALPHABET.normalize(rawStem.substring(0, lastDot)).replace(".", "");
        return prefix + guesser.toTurkishLetterPronunciations(lastSegment);
      }
    }
    if (secondaryPos == SecondaryPos.Abbreviation) {
      return guesser.guessForAbbreviation(stemNormalized);
    }
    return guessPronunciation(stemNormalized);
  }

  private StemAndEnding getFromNumeral(String s) {
    if (s.contains("'")) {
      int i = s.indexOf('\'');
      return new StemAndEnding(s.substring(0, i), s.substring(i + 1));
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

  private List<SingleAnalysis> getForRomanNumeral(Token token) {
    String content = token.getText();
    StemAndEnding se;
    if (content.contains("'")) {
      int i = content.indexOf('\'');
      se = new StemAndEnding(content.substring(0, i), content.substring(i + 1));
    } else {
      se = new StemAndEnding(content, "");
    }
    String ss = se.stem;
    if (se.stem.endsWith(".")) {
      ss = se.stem.substring(0, se.stem.length() - 1);
    }
    int decimal = TurkishNumbers.romanToDecimal(ss);
    if (decimal == -1) {
      return new ArrayList<>(0);
    }

    String lemma = numeralEndingMachine.find(String.valueOf(decimal));
    if (se.stem.endsWith(".")) {
      lemma = ordinalMap.get(lemma);
    }
    if (lemma == null || lemma.isEmpty()) {
      return Collections.emptyList();
    }
    List<SingleAnalysis> results = Lists.newArrayListWithCapacity(1);
    List<SingleAnalysis> res = analyzeNumeralLemma(lemma, se.ending);
    for (SingleAnalysis re : res) {
      if (re.getDictionaryItem().primaryPos != PrimaryPos.Numeral) {
        continue;
      }
      DictionaryItem runTimeItem = new DictionaryItem(
          se.stem,
          se.stem,
          content + lemma,
          PrimaryPos.Numeral,
          SecondaryPos.RomanNumeral);
      runTimeItem.attributes.add(RootAttribute.Runtime);
      results.add(re.copyFor(runTimeItem, se.stem));
    }
    return results;
  }

  private List<SingleAnalysis> tryNumeral(Token token) {
    String s = token.getText().toLowerCase(TurkishAlphabet.TR);
    StemAndEnding se = getFromNumeral(s);
    List<SecondaryPos> secondaryPosList = classifyNumeral(se.stem);
    if (secondaryPosList.isEmpty()) {
      return Collections.emptyList();
    }

    String lemma;
    if (se.stem.endsWith(".")) {
      String ss = se.stem.substring(0, se.stem.length() - 1);
      lemma = ordinalMap.get(numeralEndingMachine.find(ss));
    } else {
      lemma = numeralEndingMachine.find(se.stem);
    }
    if (lemma == null || lemma.isEmpty()) {
      return Collections.emptyList();
    }

    List<SingleAnalysis> results = Lists.newArrayListWithCapacity(secondaryPosList.size());
    List<SingleAnalysis> defaultAnalyses = null;

    for (SecondaryPos secondaryPos : secondaryPosList) {
      String effectiveLemma = lemma;
      List<SingleAnalysis> res;
      if (secondaryPos == SecondaryPos.Clock
          && se.stem.endsWith("00")
          && se.stem.length() >= 4
          && se.stem.length() <= 5) {
        effectiveLemma = numeralEndingMachine.find(se.stem.substring(0, se.stem.length() - 3));
        res = analyzeNumeralLemma(effectiveLemma, se.ending);
      } else {
        if (defaultAnalyses == null) {
          defaultAnalyses = analyzeNumeralLemma(lemma, se.ending);
        }
        res = defaultAnalyses;
      }
      for (SingleAnalysis re : res) {
        if (re.getDictionaryItem().primaryPos != PrimaryPos.Numeral) {
          continue;
        }
        DictionaryItem runTimeItem = new DictionaryItem(
            se.stem,
            se.stem,
            s + effectiveLemma,
            PrimaryPos.Numeral,
            secondaryPos);
        runTimeItem.attributes.add(RootAttribute.Runtime);
        results.add(re.copyFor(runTimeItem, se.stem));
      }
    }
    return results;
  }

  private List<SingleAnalysis> analyzeNumeralLemma(String lemma, String ending) {
    String toParse =
        (!ending.isEmpty() && lemma.equals("dört") && ALPHABET.isVowel(ending.charAt(0)))
            ? "dörd" + ending
            : lemma + ending;
    return analyzer.analyze(toParse);
  }

  public enum Numerals {
    CARDINAL("#", "^[+\\-]?\\d+$", SecondaryPos.Cardinal),
    ORDINAL("#.", "^[+\\-]?\\d+\\.$", SecondaryPos.Ordinal),
    RANGE("#-#", "^[+\\-]?\\d+-\\d+$", SecondaryPos.Range),
    RATIO("#/#", "^[+\\-]?\\d+/\\d+$", SecondaryPos.Ratio),
    REAL("#,#", "^[+\\-]?\\d+[.,]\\d+$", SecondaryPos.Real),
    PERCENTAGE_BEFORE("%#", "^[+\\-]?%\\d+(?:[.,]\\d+)?$", SecondaryPos.Percentage),
    TIME("#:#",
        "^(?:(?:[01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?|(?:[01]\\d|2[0-3]|[1-9])\\.[0-5]\\d)$",
        SecondaryPos.Clock),
    DATE("##.##.####",
        "^(?:0?[1-9]|[12]\\d|3[01])([./])(?:0?[1-9]|1[0-2])\\1(?:\\d{4}|\\d{2})$",
        SecondaryPos.Date);

    public final String lemma;
    public final Pattern pattern;
    public final SecondaryPos secondaryPos;

    Numerals(String lemma, String patternStr, SecondaryPos secondaryPos) {
      this.lemma = lemma;
      this.pattern = Pattern.compile(patternStr);
      this.secondaryPos = secondaryPos;
    }
  }

  static List<SecondaryPos> classifyNumeral(String stem) {
    List<SecondaryPos> matches = new ArrayList<>(2);
    for (Numerals n : Numerals.values()) {
      if (n.pattern.matcher(stem).matches()) {
        matches.add(n.secondaryPos);
      }
    }
    return matches;
  }

}
