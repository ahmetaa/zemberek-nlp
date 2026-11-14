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

  private static final class PrefixRule {
    final String sourcePrefix;
    final String targetPrefix;
    final String expectedLemma;

    PrefixRule(String sourcePrefix, String targetPrefix, String expectedLemma) {
      this.sourcePrefix = sourcePrefix;
      this.targetPrefix = targetPrefix;
      this.expectedLemma = expectedLemma;
    }
  }

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

  private static final class OpenCompoundRule {
    final String sourcePrefix;
    final String firstWord;
    final String secondStem;
    final String expectedLemma;
    final int minSuffixLen;

    OpenCompoundRule(
        String sourcePrefix,
        String firstWord,
        String secondStem,
        String expectedLemma,
        int minSuffixLen) {
      this.sourcePrefix = sourcePrefix;
      this.firstWord = firstWord;
      this.secondStem = secondStem;
      this.expectedLemma = expectedLemma;
      this.minSuffixLen = minSuffixLen;
    }
  }

  private static final OpenCompoundRule[] OPEN_COMPOUND_RULES = {
      new OpenCompoundRule("hiçbirşey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("hiçbirsey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("hicbirsey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("hiçbişey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("hiçbisey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("hicbisey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("birşey", "bir", "şey", "şey", 0),
      new OpenCompoundRule("birsey", "bir", "şey", "şey", 0),
      new OpenCompoundRule("bişey", "bir", "şey", "şey", 0),
      new OpenCompoundRule("bisey", "bir", "şey", "şey", 0),
      new OpenCompoundRule("herşey", "her", "şey", "şey", 0),
      new OpenCompoundRule("hersey", "her", "şey", "şey", 0),
      new OpenCompoundRule("heryer", "her", "yer", "yer", 0),
      new OpenCompoundRule("hoşgel", "hoş", "gel", "gelmek", 2),
      new OpenCompoundRule("hosgel", "hoş", "gel", "gelmek", 2),
      new OpenCompoundRule("sağol", "sağ", "ol", "olmak", 0),
      new OpenCompoundRule("sagol", "sağ", "ol", "olmak", 0),
      new OpenCompoundRule("saol", "sağ", "ol", "olmak", 0)
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

    if (!hasNonExtRegularAnalysisExcept(word, null)) {
      String openCompound = expandOpenCompound(word);
      if (openCompound != null) {
        return openCompound;
      }
      String verbExpansion = expandNeVerbContraction(word);
      if (verbExpansion != null) {
        return verbExpansion;
      }
    }

    return word;
  }

  private String expandOpenCompound(String word) {
    for (OpenCompoundRule rule : OPEN_COMPOUND_RULES) {
      if (word.startsWith(rule.sourcePrefix)
          && word.length() - rule.sourcePrefix.length() >= rule.minSuffixLen) {
        String suffix = word.substring(rule.sourcePrefix.length());
        String realized = realizeSingleWord(rule.secondStem + suffix, rule.expectedLemma);
        if (realized != null) {
          return rule.firstWord + " " + realized;
        }
      }
    }
    return null;
  }

  private String expandNeVerbContraction(String word) {
    if (word.length() < 5 || "nolu".equals(word)) {
      return null;
    }
    if (word.startsWith("nap")) {
      String realized = realizeSingleWord("yap" + word.substring(3), "yapmak");
      return realized != null ? "ne " + realized : null;
    }
    if (word.startsWith("n'ap") && word.length() >= 6) {
      String realized = realizeSingleWord("yap" + word.substring(4), "yapmak");
      return realized != null ? "ne " + realized : null;
    }
    if (word.startsWith("nol")) {
      String realized = realizeSingleWord("ol" + word.substring(3), "olmak");
      return realized != null ? "ne " + realized : null;
    }
    if (word.startsWith("n'ol") && word.length() >= 6) {
      String realized = realizeSingleWord("ol" + word.substring(4), "olmak");
      return realized != null ? "ne " + realized : null;
    }
    return null;
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
    if (a.length() != b.length()) {
      return Math.abs(a.length() - b.length()) + 10;
    }
    int diffs = 0;
    for (int i = 0; i < a.length(); i++) {
      if (a.charAt(i) != b.charAt(i)) {
        diffs++;
      }
    }
    return diffs;
  }
}
