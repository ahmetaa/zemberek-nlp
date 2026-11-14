package zemberek.normalization;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import zemberek.core.io.KeyValueReader;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.normalization.deasciifier.latticedeasciifier.LatticeDeasciifier;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Token-stream preprocessing pipeline for {@link TurkishSentenceNormalizer}: handles multi-word
 * replacements, informal contractions, expressive elongations, detached suffix/compound joining,
 * numeral suffix joining, glued number+unit splitting, and morphotactic clitic splitting.
 */
final class NormalizerPreprocessor {

  private static final Pattern ASCII_DIGITS_PATTERN = Pattern.compile("\\d{1,6}");

  private static final Set<String> UNAMBIGUOUS_NUMERAL_SUFFIXES = Set.of(
      "te", "den", "dan", "ten", "tan",
      "inci", "ıncı", "uncu", "üncü", "nci", "ncı", "ncu", "ncü",
      "deki", "daki", "teki", "taki",
      "leri", "ları", "lerde", "larda", "lerden", "lardan", "lerin", "ların",
      "li", "lı", "lu", "lü", "lik", "lık", "luk", "lük",
      "nin", "nın", "nun", "nün", "yle", "yla",
      "sında", "sinde", "sından", "sinden", "sına", "sine", "sını", "sini");

  private static final Set<String> NUMERAL_LOCATIVE_SUFFIXES = Set.of("de", "da", "ta");

  private static final Set<String> NUMERAL_LOCATIVE_ANCHORS = Set.of(
      "saat", "sayfa", "madde", "bölüm", "numara", "no", "kat", "kanal", "peron", "hat");

  private static final Set<String> GLUED_NUMBER_EXACT_UNITS = Set.of(
      "tl", "ytl", "lira", "kuruş", "kurus", "usd", "dolar", "euro", "cent",
      "gb", "mb", "kb", "tb", "kg", "gr", "mg", "km", "cm", "mm", "lt", "ml",
      "dk", "sn", "ay", "aylık", "aylik", "aydan", "aydır", "aydir", "ayda", "aya", "ayı", "ayi",
      "kere", "kez", "adet", "buçuk", "bucuk", "bin", "milyon", "milyar");

  private static final List<String> GLUED_NUMBER_UNIT_PREFIXES = List.of(
      "saat", "hafta", "gün", "gun", "yıl", "yil", "sene", "dakika", "saniye",
      "yaş", "yas", "tane", "kişi", "kisi", "defa", "sefer", "numara", "puan",
      "derece", "kat", "bölüm", "bolum", "sayfa");

  private static final Set<String> GLUED_KI_HEAD_WORDS = Set.of(
      "var", "yok", "öyle", "oyle", "böyle", "boyle", "şöyle", "soyle", "iyi", "tabii", "tabi");

  private static final Set<String> GLUED_KI_TIME_STEMS = Set.of(
      "bugün", "bugun", "dün", "dun", "gün", "gun", "yarın", "yarin",
      "şimdi", "simdi", "önce", "once", "sonra");

  private static final List<SolidCompoundRule> SOLID_COMPOUND_RULES = List.of(
      SolidCompoundRule.prefix("uluslar", "arası"),
      SolidCompoundRule.prefix("milletler", "arası"),
      SolidCompoundRule.prefix("hiç", "bir"),
      SolidCompoundRule.prefix("bir", "kaç"),
      SolidCompoundRule.exact("bir", "çok"),
      SolidCompoundRule.prefix("her", "hangi"),
      SolidCompoundRule.prefix("vaz", "geç"));

  private record SolidCompoundRule(String first, String second, boolean prefixMatch) {
    static SolidCompoundRule prefix(String first, String second) {
      return new SolidCompoundRule(first, second, true);
    }

    static SolidCompoundRule exact(String first, String second) {
      return new SolidCompoundRule(first, second, false);
    }

    String tryCombine(String left, String right) {
      if (!FormalSurfaceRealizer.matchesCanonicalOrAscii(left, first, false)
          || !FormalSurfaceRealizer.matchesCanonicalOrAscii(right, second, prefixMatch)) {
        return null;
      }
      return first + second + right.substring(second.length());
    }
  }

  private final TurkishMorphology morphology;
  private final FormalSurfaceRealizer surfaceRealizer;
  private final SmoothLm lm;
  private final ImmutableListMultimap<String, String> lookupManual;
  private final Map<String, String> commonSplits;
  private final Map<String, String> replacements;
  private final Set<String> commonConnectedSuffixes;
  private final Set<String> noSplitWords;
  private final InformalContractionExpander contractionExpander;
  private final ElongationNormalizer elongationNormalizer;

  NormalizerPreprocessor(
      TurkishMorphology morphology,
      FormalSurfaceRealizer surfaceRealizer,
      SmoothLm lm,
      ImmutableListMultimap<String, String> lookupManual,
      Path dataRoot) throws IOException {
    this.morphology = morphology;
    this.surfaceRealizer = surfaceRealizer;
    this.lm = lm;
    this.lookupManual = lookupManual;

    KeyValueReader kvReader = new KeyValueReader("=");
    this.commonSplits = kvReader.loadFromFile(dataRoot.resolve("split").toFile());
    try (InputStream is =
        Resources.getResource("normalization/multi-word-replacements").openStream()) {
      this.replacements = kvReader.loadFromStream(is);
    }

    Set<String> suffixes =
        new HashSet<>(TextIO.loadLinesFromResource("normalization/question-suffixes"));
    suffixes.addAll(List.of("de", "da", "ki"));
    this.commonConnectedSuffixes = Collections.unmodifiableSet(suffixes);
    this.noSplitWords = Set.copyOf(TextIO.loadLinesFromResource("normalization/no-split"));

    this.contractionExpander = new InformalContractionExpander(morphology, surfaceRealizer);
    this.elongationNormalizer = new ElongationNormalizer(
        surfaceRealizer::isValidLexicalWord,
        this::unigramScore);
  }

  String preProcess(
      String sentence,
      boolean alwaysApplyDeasciifier,
      LatticeDeasciifier latticeDeasciifier) {
    boolean requiresDeasciifier = alwaysApplyDeasciifier || probablyRequiresDeasciifier(sentence);
    boolean preserveValidWords = !alwaysApplyDeasciifier && hasTurkishSpecificChars(sentence);
    if (requiresDeasciifier && latticeDeasciifier != null) {
      // Deasciify before Turkish.LOCALE lowercasing so uppercase ASCII 'I' ("Istanbul", "Isik",
      // "TUIK", "BIOS") and TitleCase proper-noun cues are not destroyed into dotless 'ı'.
      // Known-word mode leaves OOV typos and foreign words untouched for downstream spell-checking.
      sentence = latticeDeasciifier.deasciifyKnownWords(sentence);
    }
    sentence = sentence.toLowerCase(Turkish.LOCALE);
    String s = replaceCommon(TurkishTokenizer.DEFAULT.tokenize(sentence));
    s = normalizeElongations(TurkishTokenizer.DEFAULT.tokenize(s));
    s = combineNecessaryWords(TurkishTokenizer.DEFAULT.tokenize(s));
    s = splitNecessaryWords(TurkishTokenizer.DEFAULT.tokenize(s), false);
    if (requiresDeasciifier) {
      if (latticeDeasciifier == null) {
        s = deasciifyValidWords(s, preserveValidWords);
      } else if (!s.equals(sentence)) {
        s = latticeDeasciifier.deasciifyKnownWords(s);
      }
    }
    s = combineNecessaryWords(TurkishTokenizer.DEFAULT.tokenize(s));
    return splitNecessaryWords(TurkishTokenizer.DEFAULT.tokenize(s), true);
  }

  String deasciifyValidWords(String s, boolean preserveValidWords) {
    String deasciified = Deasciifier.deasciify(s);
    if (deasciified.equals(s) || deasciified.length() != s.length()) {
      return deasciified;
    }
    StringBuilder sb = new StringBuilder(s);
    for (Token token : TurkishTokenizer.DEFAULT.tokenize(s)) {
      int start = token.getStart();
      int end = token.getEnd() + 1;
      if (start < 0 || end > deasciified.length()) {
        continue;
      }
      String origWord = s.substring(start, end);
      String deascWord = deasciified.substring(start, end);
      if (!origWord.equals(deascWord)
          && isWord(token)
          && !(preserveValidWords && surfaceRealizer.hasRegularAnalysis(origWord))
          && morphology.analyze(deascWord).isCorrect()) {
        sb.replace(start, end, deascWord);
      }
    }
    return sb.toString();
  }

  static boolean hasTurkishSpecificChars(String sentence) {
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (c != 'I' && TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        return true;
      }
    }
    return false;
  }

  static boolean probablyRequiresDeasciifier(String sentence) {
    if (sentence.isEmpty()) {
      return false;
    }
    int turkishSpecCount = 0;
    int letterCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (Character.isLetter(c)) {
        letterCount++;
      }
      if (c != 'I' && TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        turkishSpecCount++;
      }
    }
    return letterCount > 0 && (turkishSpecCount * 1d / letterCount) < 0.05;
  }

  static boolean isWord(Token token) {
    Type type = token.getType();
    return type == Type.Word
        || type == Type.WordWithSymbol
        || type == Type.WordAlphanumerical
        || type == Type.UnknownWord;
  }

  String combineCommon(String i1, String i2) {
    String solidCompound = combineSolidCompound(i1, i2);
    if (solidCompound != null) {
      return solidCompound;
    }
    String combined = i1 + i2;
    if ((i2.startsWith("'") || i2.startsWith("bil"))
        && hasKnownLexicalAnalysis(morphology.analyze(combined))) {
      return combined;
    }
    if (!isStandaloneToken(i2) && surfaceRealizer.hasRegularAnalysis(combined)) {
      return combined;
    }
    return "";
  }

  private String combineSolidCompound(String i1, String i2) {
    String left = expandInformalBir(i1);
    String right = expandInformalBir(i2);
    for (SolidCompoundRule rule : SOLID_COMPOUND_RULES) {
      String candidate = rule.tryCombine(left, right);
      if (candidate != null) {
        return surfaceRealizer.realizeRegularSurface(candidate);
      }
    }
    return null;
  }

  private static String expandInformalBir(String token) {
    return ("bi".equals(token) || "bı".equals(token)) ? "bir" : token;
  }

  private boolean isStandaloneToken(String token) {
    return surfaceRealizer.hasRegularAnalysis(token)
        || lookupManual.containsKey(token)
        || surfaceRealizer.hasAsciiRootWordAnalysis(token);
  }

  private static boolean hasKnownLexicalAnalysis(WordAnalysis analyses) {
    for (SingleAnalysis analysis : analyses) {
      if (FormalSurfaceRealizer.isKnownLexical(analysis)) {
        return true;
      }
    }
    return false;
  }

  String separateCommon(String input, boolean useLookup) {
    if (noSplitWords.contains(input)) {
      return input;
    }
    if (useLookup && commonSplits.containsKey(input)) {
      return commonSplits.get(input);
    }
    String unitSplit = separateGluedNumberUnit(input);
    if (unitSplit != null) {
      return unitSplit;
    }
    if (!surfaceRealizer.hasRegularOrAsciiAnalysis(input)) {
      for (int i = 1; i < input.length() - 1; i++) {
        String tail = input.substring(i);
        boolean isAsciiKi = "kı".equals(tail);
        if (!commonConnectedSuffixes.contains(tail) && !isAsciiKi) {
          continue;
        }
        String head = input.substring(0, i);
        if (!isAsciiKi
            && (tail.length() >= 3 || lm.ngramExists(lm.getVocabulary().toIndexes(head, tail)))
            && surfaceRealizer.hasRegularAnalysis(head)) {
          return head + " " + tail;
        }
        int minHeadLen = tail.length() >= 3 ? 2 : 3;
        if (head.length() < minHeadLen) {
          continue;
        }
        if ("ki".equals(tail) || isAsciiKi) {
          if (canTakeDetachedKi(head)) {
            return head + " ki";
          }
        } else if ("de".equals(tail) || "da".equals(tail)) {
          if (canTakeDetachedDeDa(head)) {
            return head + " " + tail;
          }
        } else if (surfaceRealizer.hasRegularOrAsciiAnalysis(head)) {
          return head + " " + harmonizeQuestionClitic(head, tail);
        }
      }
    }
    return input;
  }

  private String separateGluedNumberUnit(String input) {
    int digitCount = 0;
    while (digitCount < input.length() && Character.isDigit(input.charAt(digitCount))) {
      digitCount++;
    }
    if (digitCount == 0 || digitCount > 6) {
      return null;
    }
    String unitPart = input.substring(digitCount);
    if (unitPart.length() < 2
        || UNAMBIGUOUS_NUMERAL_SUFFIXES.contains(unitPart)
        || NUMERAL_LOCATIVE_SUFFIXES.contains(unitPart)) {
      return null;
    }
    for (int i = 0; i < unitPart.length(); i++) {
      if (!Character.isLetter(unitPart.charAt(i))) {
        return null;
      }
    }
    if (GLUED_NUMBER_EXACT_UNITS.contains(unitPart)) {
      return input.substring(0, digitCount) + " " + unitPart;
    }
    for (String prefix : GLUED_NUMBER_UNIT_PREFIXES) {
      if (unitPart.startsWith(prefix) && surfaceRealizer.hasRegularOrAsciiAnalysis(unitPart)) {
        return input.substring(0, digitCount) + " " + unitPart;
      }
    }
    return null;
  }

  private boolean canTakeDetachedKi(String head) {
    if (GLUED_KI_TIME_STEMS.contains(head)) {
      return false;
    }
    if (GLUED_KI_HEAD_WORDS.contains(head)) {
      return true;
    }
    for (SingleAnalysis a : surfaceRealizer.informalAsciiTolerantMorphology().analyze(head)) {
      if (!FormalSurfaceRealizer.isRegularAnalysis(a)) {
        continue;
      }
      if (a.getLastGroup().getPos() == PrimaryPos.Verb
          || a.containsMorpheme(TurkishMorphotactics.inf1)) {
        return true;
      }
    }
    return false;
  }

  private boolean canTakeDetachedDeDa(String head) {
    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    boolean endsVoiceless =
        !head.isEmpty() && alphabet.isVoicelessConsonant(alphabet.lastChar(head));
    boolean hasUnambiguousHost = false;
    boolean hasAmbiguousSingularP3sgNom = false;
    for (SingleAnalysis a : surfaceRealizer.informalAsciiTolerantMorphology().analyze(head)) {
      if (!FormalSurfaceRealizer.isRegularAnalysis(a)) {
        continue;
      }
      boolean hasOvertCase = containsOvertCase(a);
      if (!hasOvertCase
          && a.containsMorpheme(TurkishMorphotactics.a3sg)
          && a.containsMorpheme(TurkishMorphotactics.p3sg)) {
        hasAmbiguousSingularP3sgNom = true;
      }
      if (a.containsMorpheme(TurkishMorphotactics.afterDoingSo)) {
        hasUnambiguousHost = true;
      } else if (!endsVoiceless) {
        if (a.getLastGroup().getPos() == PrimaryPos.Verb
            || a.containsMorpheme(TurkishMorphotactics.when)
            || hasOvertCase
            || (a.containsMorpheme(TurkishMorphotactics.a3pl)
                && a.containsAnyMorpheme(TurkishMorphotactics.p3sg, TurkishMorphotactics.p3pl))) {
          hasUnambiguousHost = true;
        }
      }
    }
    return hasUnambiguousHost && !hasAmbiguousSingularP3sgNom;
  }

  private static boolean containsOvertCase(SingleAnalysis a) {
    return a.containsAnyMorpheme(
        TurkishMorphotactics.acc,
        TurkishMorphotactics.dat,
        TurkishMorphotactics.abl,
        TurkishMorphotactics.ins,
        TurkishMorphotactics.gen,
        TurkishMorphotactics.loc);
  }

  private String harmonizeQuestionClitic(String head, String tail) {
    if (!"mı".equals(tail) && !"mu".equals(tail)) {
      return tail;
    }
    String realized = surfaceRealizer.realizeRegularSurfaceOrIdentity(head);
    char lastVowel = TurkishAlphabet.INSTANCE.getLastVowel(realized).charValue;
    if ("mı".equals(tail) && (lastVowel == 'e' || lastVowel == 'i')) {
      return "mi";
    }
    if ("mu".equals(tail) && (lastVowel == 'ö' || lastVowel == 'ü')) {
      return "mü";
    }
    return tail;
  }

  String combineNumeralSuffix(String prevWord, String number, String suffix) {
    if (suffix.length() < 2) {
      return "";
    }
    boolean allowed = UNAMBIGUOUS_NUMERAL_SUFFIXES.contains(suffix)
        || (NUMERAL_LOCATIVE_SUFFIXES.contains(suffix)
            && NUMERAL_LOCATIVE_ANCHORS.contains(prevWord));
    if (!allowed) {
      return "";
    }
    String result = tryNumeralAnalysis(number + "'" + suffix);
    if (!result.isEmpty()) {
      return result;
    }
    char firstChar = suffix.charAt(0);
    if (firstChar == 'd' || firstChar == 't') {
      char toggled = (firstChar == 'd') ? 't' : 'd';
      result = tryNumeralAnalysis(number + "'" + toggled + suffix.substring(1));
      if (!result.isEmpty()) {
        return result;
      }
    }
    if ("inci".equals(suffix) || "ıncı".equals(suffix)
        || "uncu".equals(suffix) || "üncü".equals(suffix)) {
      result = tryNumeralAnalysis(number + "'" + suffix.substring(1));
      if (!result.isEmpty()) {
        return result;
      }
    }
    return "";
  }

  private String tryNumeralAnalysis(String candidate) {
    boolean validNumeral = morphology.analyze(candidate).stream()
        .anyMatch(k -> !k.isUnknown() && k.getDictionaryItem().primaryPos == PrimaryPos.Numeral);
    return validNumeral ? candidate : "";
  }

  String combineNecessaryWords(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    List<String> result = new ArrayList<>(tokens.size());
    for (int i = 0; i < tokens.size(); i++) {
      if (i + 1 < tokens.size()) {
        String combined = tryCombineTokenPair(tokens, i);
        if (!combined.isEmpty()) {
          result.add(combined);
          i++;
          continue;
        }
      }
      result.add(tokens.get(i).getText());
    }
    return String.join(" ", result);
  }

  private String tryCombineTokenPair(List<Token> tokens, int index) {
    Token first = tokens.get(index);
    Token second = tokens.get(index + 1);
    String firstText = first.getText();
    String secondText = second.getText();
    if (first.getType() == Type.Number
        && isWord(second)
        && ASCII_DIGITS_PATTERN.matcher(firstText).matches()) {
      String prevWord = index > 0 ? tokens.get(index - 1).getText() : "";
      String combinedNum = combineNumeralSuffix(prevWord, firstText, secondText);
      if (!combinedNum.isEmpty()) {
        return combinedNum;
      }
    }
    if (isWord(first) && isWord(second)) {
      return combineCommon(firstText, secondText);
    }
    return "";
  }

  String normalizeElongations(List<Token> tokens) {
    return rewriteTokens(
        tokens,
        token -> isWord(token)
            ? elongationNormalizer.normalizeWord(token.getText())
            : token.getText());
  }

  String splitNecessaryWords(List<Token> tokens, boolean useLookup) {
    return rewriteTokens(
        tokens,
        token -> isWord(token)
            ? separateCommon(token.getText(), useLookup)
            : token.getText());
  }

  String replaceCommon(List<Token> tokens) {
    return rewriteTokens(tokens, token -> {
      String text = token.getText();
      String replacement = replacements.get(text);
      if (replacement != null) {
        return replacement;
      }
      return isWord(token) ? contractionExpander.expandWord(text) : text;
    });
  }

  private static String rewriteTokens(List<Token> tokens, Function<Token, String> mapper) {
    List<String> result = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      result.add(mapper.apply(token));
    }
    return String.join(" ", result);
  }

  private double unigramScore(String word) {
    if (!lm.getVocabulary().contains(word)) {
      return Double.NEGATIVE_INFINITY;
    }
    return lm.getUnigramProbability(lm.getVocabulary().indexOf(word));
  }
}
