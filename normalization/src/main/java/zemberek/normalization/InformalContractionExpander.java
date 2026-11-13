package zemberek.normalization;

import java.util.List;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator.Result;

/**
 * Expands productive Turkish colloquial vowel-drop (haplology) and merged question contractions
 * that are either absent from the lexicon or masked by {@link RootAttribute#Ext} dictionary
 * entries (such as {@code orda}, {@code şurda}, and {@code orjinal}).
 */
final class InformalContractionExpander {

  private record PrefixRule(String sourcePrefix, String targetPrefix, String expectedLemma) {}

  // Ordered longest-prefix-first so -dan/-da prefixes do not shadow one another.
  private static final PrefixRule[] STEM_CONTRACTION_RULES = {
      new PrefixRule("burdan", "buradan", "bura"),
      new PrefixRule("burda", "burada", "bura"),
      new PrefixRule("şurdan", "şuradan", "şura"),
      new PrefixRule("surdan", "şuradan", "şura"),
      new PrefixRule("şurda", "şurada", "şura"),
      new PrefixRule("surda", "şurada", "şura"),
      new PrefixRule("ordan", "oradan", "ora"),
      new PrefixRule("orda", "orada", "ora"),
      new PrefixRule("nerden", "nereden", "nere"),
      new PrefixRule("nerde", "nerede", "nere"),
      new PrefixRule("orjinal", "orijinal", "orijinal")
  };

  private final TurkishMorphology morphology;
  private final TurkishMorphology informalAsciiTolerantMorphology;
  private final InformalAnalysisConverter analysisConverter;

  InformalContractionExpander(
      TurkishMorphology morphology,
      TurkishMorphology informalAsciiTolerantMorphology,
      InformalAnalysisConverter analysisConverter) {
    this.morphology = morphology;
    this.informalAsciiTolerantMorphology = informalAsciiTolerantMorphology;
    this.analysisConverter = analysisConverter;
  }

  /**
   * Returns the expanded single- or multi-word replacement for {@code word}, or {@code word}
   * unchanged if no rule applies.
   */
  String expandWord(String word) {
    if (word == null || word.length() < 4) {
      return word;
    }

    for (PrefixRule rule : STEM_CONTRACTION_RULES) {
      if (word.startsWith(rule.sourcePrefix)
          && !hasNonExtRegularAnalysisExcept(word, rule.expectedLemma)) {
        String candidate = rule.targetPrefix + word.substring(rule.sourcePrefix.length());
        String realized = realizeSingleWord(candidate, rule.expectedLemma);
        if (realized != null) {
          return realized;
        }
      }
    }

    String verbExpansion = expandNeVerbContraction(word);
    return verbExpansion != null ? verbExpansion : word;
  }

  private String expandNeVerbContraction(String word) {
    if (word.length() < 5 || "nolu".equals(word)) {
      return null;
    }
    String candidate;
    String lemma;
    if (word.startsWith("nap")) {
      candidate = "yap" + word.substring(3);
      lemma = "yapmak";
    } else if (word.startsWith("n'ap") && word.length() >= 6) {
      candidate = "yap" + word.substring(4);
      lemma = "yapmak";
    } else if (word.startsWith("nol")) {
      candidate = "ol" + word.substring(3);
      lemma = "olmak";
    } else if (word.startsWith("n'ol") && word.length() >= 6) {
      candidate = "ol" + word.substring(4);
      lemma = "olmak";
    } else {
      return null;
    }
    if (hasNonExtRegularAnalysisExcept(word, null)) {
      return null;
    }
    String realized = realizeSingleWord(candidate, lemma);
    return realized != null ? "ne " + realized : null;
  }

  private boolean hasNonExtRegularAnalysisExcept(String word, String allowedColloquialLemma) {
    WordAnalysis analysis = morphology.analyze(word);
    for (SingleAnalysis single : analysis) {
      if (single.isUnknown() || single.isRuntime()) {
        continue;
      }
      SecondaryPos secPos = single.getDictionaryItem().secondaryPos;
      if (secPos == SecondaryPos.ProperNoun || secPos == SecondaryPos.Abbreviation) {
        continue;
      }
      if (single.getDictionaryItem().hasAttribute(RootAttribute.Ext)) {
        continue;
      }
      if (allowedColloquialLemma != null
          && allowedColloquialLemma.equals(single.getDictionaryItem().lemma)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private String realizeSingleWord(String candidate, String expectedLemma) {
    WordAnalysis formal = morphology.analyze(candidate);
    for (SingleAnalysis analysis : formal) {
      if (!analysis.isUnknown()
          && !analysis.isRuntime()
          && expectedLemma.equals(analysis.getDictionaryItem().lemma)) {
        return candidate;
      }
    }

    WordAnalysis relaxed = informalAsciiTolerantMorphology.analyze(candidate);
    String bestSurface = null;
    int bestDiffs = Integer.MAX_VALUE;
    for (SingleAnalysis analysis : relaxed) {
      if (analysis.isUnknown()
          || analysis.isRuntime()
          || !expectedLemma.equals(analysis.getDictionaryItem().lemma)) {
        continue;
      }
      if (analysis.containsInformalMorpheme()) {
        Result converted = analysisConverter.convert(candidate, analysis);
        if (converted != null) {
          int diffs = diacriticMismatchCount(candidate, converted.surface);
          if (diffs < bestDiffs) {
            bestDiffs = diffs;
            bestSurface = converted.surface;
          }
        }
      } else {
        List<Result> generated =
            morphology
                .getWordGenerator()
                .generate(analysis.getDictionaryItem(), analysis.getMorphemes());
        for (Result result : generated) {
          if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(candidate, result.surface)) {
            int diffs = diacriticMismatchCount(candidate, result.surface);
            if (diffs < bestDiffs) {
              bestDiffs = diffs;
              bestSurface = result.surface;
            }
          }
        }
      }
    }
    return bestSurface;
  }

  private static int diacriticMismatchCount(String a, String b) {
    int limit = Math.min(a.length(), b.length());
    int diffs = 0;
    for (int i = 0; i < limit; i++) {
      char ca = a.charAt(i);
      char cb = b.charAt(i);
      if (ca != cb && TurkishAlphabet.INSTANCE.isAsciiEqual(ca, cb)) {
        diffs++;
      }
    }
    return diffs;
  }
}
