package zemberek.normalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.NgramLanguageModel;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Normalizes Turkish {@code de/da} clitic vs. locative case errors in both directions:
 *
 * <ul>
 *   <li>Merges mistakenly separated locative suffixes ({@code birlik te} &rarr; {@code birlikte},
 *       {@code hayat ta} &rarr; {@code hayatta}, {@code arasın da} &rarr; {@code arasında}).
 *   <li>Splits mistakenly attached conjunctive {@code de/da} clitics ({@code başlasada} &rarr;
 *       {@code başlasa da}, {@code olarakda} &rarr; {@code olarak da}, {@code manisa'dada} &rarr;
 *       {@code manisa'da da}, and LM-gated {@code yada} &rarr; {@code ya da}).
 * </ul>
 */
final class CliticDeDaNormalizer {

  private static final float MERGE_DEFAULT_LLR_THRESHOLD = 5.8f;
  private static final float MERGE_AMBIGUOUS_HEAD_LLR_THRESHOLD = 7.5f;
  private static final float MERGE_VOICELESS_ABORT_SPLIT_LLR = 1.5f;
  private static final float SPLIT_AMBIGUOUS_LLR_THRESHOLD = 4.0f;

  private static final Set<String> NON_MERGE_HEADS = Set.of(
      "hem", "ve", "ya", "ki", "ne", "değil", "bile", "daha", "sonra", "önce",
      "ama", "fakat", "ancak", "ise", "öyle", "böyle", "şöyle", "belki", "sanki",
      "keşke", "zaten", "hatta", "yine", "gene", "hala", "hâlâ", "artık", "tabii");

  private static final Set<String> PRONOUN_OR_POSTP_HEADS = Set.of(
      "ben", "sen", "o", "biz", "siz", "onlar", "bu", "şu", "kendi", "biri", "kim", "için");

  private final TurkishMorphology morphology;
  private final NgramLanguageModel lm;
  private final Set<String> noSplitWords;
  private final boolean hasBigramLm;
  private final String startSym;
  private final String endSym;
  private final float toNats;

  CliticDeDaNormalizer(
      TurkishMorphology morphology, NgramLanguageModel lm, Set<String> noSplitWords) {
    this.morphology = morphology;
    this.lm = lm;
    this.noSplitWords = noSplitWords;
    this.hasBigramLm = lm != null && lm.getOrder() >= 2;
    this.startSym = hasBigramLm ? lm.getVocabulary().getSentenceStart() : "<s>";
    this.endSym = hasBigramLm ? lm.getVocabulary().getSentenceEnd() : "</s>";
    double base = (lm instanceof SmoothLm) ? ((SmoothLm) lm).getLogBase() : 1.0;
    this.toNats = base > 1.0001 ? (float) Math.log(base) : 1.0f;
  }

  String normalize(String s) {
    if (!hasDeDaTokenOrSuffix(s)) {
      return s;
    }
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    int n = tokens.size();
    if (n == 0) {
      return s;
    }
    String[] texts = new String[n];
    boolean[] wordFlags = new boolean[n];
    for (int i = 0; i < n; i++) {
      Token t = tokens.get(i);
      texts[i] = t.getText();
      wordFlags[i] = TurkishSentenceNormalizer.isWord(t);
    }

    boolean changed = false;
    int m = 0;
    for (int i = 0; i < n; i++) {
      if (i + 1 < n && wordFlags[i] && wordFlags[i + 1] && isLocativeCandidate(texts[i + 1])) {
        String prev = (m > 0 && wordFlags[m - 1]) ? texts[m - 1] : startSym;
        String next = (i + 2 < n && wordFlags[i + 2]) ? texts[i + 2] : endSym;
        if (shouldMerge(prev, texts[i], texts[i + 1], next)) {
          texts[m] = texts[i] + texts[i + 1];
          wordFlags[m] = true;
          m++;
          i++;
          changed = true;
          continue;
        }
      }
      texts[m] = texts[i];
      wordFlags[m] = wordFlags[i];
      m++;
    }

    List<String> result = null;
    for (int i = 0; i < m; i++) {
      String word = texts[i];
      String prev = (i > 0 && wordFlags[i - 1]) ? texts[i - 1] : startSym;
      String next = (i + 1 < m && wordFlags[i + 1]) ? texts[i + 1] : endSym;
      String head = findSplittableHead(word, wordFlags[i], prev, next);
      if (head != null) {
        if (result == null) {
          result = new ArrayList<>(m + 2);
          for (int j = 0; j < i; j++) {
            result.add(texts[j]);
          }
        }
        String tail = word.substring(word.length() - 2);
        result.add(head);
        result.add(("de".equals(tail) || "te".equals(tail)) ? "de" : "da");
        changed = true;
      } else if (result != null) {
        result.add(word);
      }
    }

    if (!changed) {
      return s;
    }
    return result != null
        ? String.join(" ", result)
        : String.join(" ", Arrays.asList(texts).subList(0, m));
  }

  private boolean shouldMerge(String prev, String first, String second, String next) {
    if (first.length() < 2) {
      return false;
    }
    String combined = first + second;
    boolean validMerged =
        TurkishSentenceNormalizer.hasRegularAnalysis(morphology, combined)
            || (first.indexOf('\'') > 0
                && TurkishSentenceNormalizer.hasAnalysis(morphology.analyze(combined)));
    if (!validMerged) {
      return false;
    }

    if ("te".equals(second) || "ta".equals(second)) {
      if (("ta".equals(second) && "ki".equals(next))
          || "ta".equals(first)
          || !matchesVoicelessHarmony(first, second)) {
        return false;
      }
      if (!hasBigramLm || !TurkishSentenceNormalizer.hasRegularAnalysis(morphology, first)) {
        return true;
      }
      String clitic = "te".equals(second) ? "de" : "da";
      return splitMinusMergedNats(prev, first, clitic, combined, next)
          <= MERGE_VOICELESS_ABORT_SPLIT_LLR;
    }

    if (NON_MERGE_HEADS.contains(first)) {
      return false;
    }
    if (!TurkishSentenceNormalizer.hasRegularAnalysis(morphology, first)
        && !TurkishSentenceNormalizer.hasProperNounOrAbbrvAnalysis(morphology, first)) {
      return true;
    }
    if (!hasBigramLm || !lm.getVocabulary().contains(normalizeForLm(combined))) {
      return false;
    }
    float threshold = PRONOUN_OR_POSTP_HEADS.contains(first)
        ? MERGE_AMBIGUOUS_HEAD_LLR_THRESHOLD
        : MERGE_DEFAULT_LLR_THRESHOLD;
    return -splitMinusMergedNats(prev, first, second, combined, next) > threshold;
  }

  private String findSplittableHead(String word, boolean isWord, String prev, String next) {
    if (!isWord || word.length() < 4 || noSplitWords.contains(word)) {
      return null;
    }
    String tail = word.substring(word.length() - 2);
    if (!isLocativeCandidate(tail)) {
      return null;
    }
    String head = word.substring(0, word.length() - 2);
    String clitic = ("de".equals(tail) || "te".equals(tail)) ? "de" : "da";

    if (!isValidSingleWord(word)) {
      boolean validHead = head.indexOf('\'') > 0
          ? TurkishSentenceNormalizer.hasProperNounOrAbbrvAnalysis(morphology, head)
          : TurkishSentenceNormalizer.hasRegularAnalysis(morphology, head);
      return (validHead && matchesCliticHarmony(head, clitic)) ? head : null;
    }

    // "yada" is a valid archaic noun in the lexicon; split to "ya da" when LM strongly prefers it.
    if ("yada".equals(word)
        && hasBigramLm
        && lm.ngramExists(lmIndex("ya"), lmIndex("da"))
        && splitMinusMergedNats(prev, "ya", "da", word, next) > SPLIT_AMBIGUOUS_LLR_THRESHOLD) {
      return "ya";
    }
    return null;
  }

  private boolean isValidSingleWord(String word) {
    return morphology.analyze(word).isCorrect()
        || (word.indexOf('\'') > 0 && morphology.analyze(Turkish.capitalize(word)).isCorrect());
  }

  private boolean matchesCliticHarmony(String head, String clitic) {
    for (SingleAnalysis s : morphology.analyze(head)) {
      if (!s.isUnknown()
          && !s.isRuntime()
          && s.getMorphemes().size() <= 2
          && s.getDictionaryItem().hasAttribute(RootAttribute.InverseHarmony)) {
        return "de".equals(clitic);
      }
    }
    return matchesFrontBackHarmony(head, "de".equals(clitic));
  }

  private static boolean matchesVoicelessHarmony(String first, String second) {
    return TurkishAlphabet.INSTANCE.isVoicelessConsonant(first.charAt(first.length() - 1))
        && matchesFrontBackHarmony(first, "te".equals(second));
  }

  private static boolean matchesFrontBackHarmony(String word, boolean expectFront) {
    TurkicLetter v = TurkishAlphabet.INSTANCE.getLastVowel(word);
    if (v == TurkicLetter.UNDEFINED) {
      return false;
    }
    return (v.charValue != 'û' && v.isFrontal()) == expectFront;
  }

  private float splitMinusMergedNats(
      String prev, String head, String clitic, String merged, String next) {
    int pIdx = lmIndex(prev);
    int hIdx = lmIndex(head);
    int cIdx = lmIndex(clitic);
    int mIdx = lmIndex(merged);
    int nIdx = lmIndex(next);
    return (lm.getProbability(pIdx, hIdx)
            + lm.getProbability(hIdx, cIdx)
            + lm.getProbability(cIdx, nIdx)
            - lm.getProbability(pIdx, mIdx)
            - lm.getProbability(mIdx, nIdx))
        * toNats;
  }

  private int lmIndex(String s) {
    return lm.getVocabulary().indexOf(normalizeForLm(s));
  }

  private static String normalizeForLm(String s) {
    return s.indexOf('\'') > 0 ? Turkish.capitalize(s) : s;
  }

  private static boolean isLocativeCandidate(String s) {
    return "de".equals(s) || "da".equals(s) || "te".equals(s) || "ta".equals(s);
  }

  private static boolean hasDeDaTokenOrSuffix(String s) {
    int len = s.length();
    for (int i = 0; i < len - 1; i++) {
      char c0 = s.charAt(i);
      if (c0 == 'd' || c0 == 't') {
        char c1 = s.charAt(i + 1);
        if ((c1 == 'a' || c1 == 'e')
            && (i + 2 == len || !Character.isLetter(s.charAt(i + 2)))) {
          return true;
        }
      }
    }
    return false;
  }
}
