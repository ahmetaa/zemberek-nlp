package zemberek.normalization;

import java.util.List;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;

/**
 * Expands productive Turkish colloquial vowel-drop (haplology) and merged open-compound / question
 * contractions that are either absent from the lexicon or masked by {@link RootAttribute#Ext}
 * dictionary entries (such as {@code orda}, {@code şurda}, {@code herşey}, and {@code napıyon}).
 */
final class InformalContractionExpander {

  private record PrefixRule(String sourcePrefix, String targetPrefix, String expectedLemma) {}

  private record OpenCompoundRule(
      String sourcePrefix,
      String firstWord,
      String secondStem,
      String expectedLemma,
      int minSuffixLen) {}

  // Ordered longest-prefix-first so -dan/-da prefixes do not shadow one another.
  private static final List<PrefixRule> STEM_CONTRACTION_RULES = List.of(
      new PrefixRule("burdan", "buradan", "bura"),
      new PrefixRule("burda", "burada", "bura"),
      new PrefixRule("şurdan", "şuradan", "şura"),
      new PrefixRule("şurda", "şurada", "şura"),
      new PrefixRule("ordan", "oradan", "ora"),
      new PrefixRule("orda", "orada", "ora"),
      new PrefixRule("nerden", "nereden", "nere"),
      new PrefixRule("nerde", "nerede", "nere"),
      new PrefixRule("orjinal", "orijinal", "orijinal")
  );

  private static final List<OpenCompoundRule> OPEN_COMPOUND_RULES = List.of(
      new OpenCompoundRule("hiçbirşey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("hiçbişey", "hiçbir", "şey", "şey", 0),
      new OpenCompoundRule("birşey", "bir", "şey", "şey", 0),
      new OpenCompoundRule("bişey", "bir", "şey", "şey", 0),
      new OpenCompoundRule("herşey", "her", "şey", "şey", 0),
      new OpenCompoundRule("heryer", "her", "yer", "yer", 0),
      new OpenCompoundRule("hoşgel", "hoş", "gel", "gelmek", 2),
      new OpenCompoundRule("sağol", "sağ", "ol", "olmak", 0),
      new OpenCompoundRule("saol", "sağ", "ol", "olmak", 0),
      new OpenCompoundRule("nap", "ne", "yap", "yapmak", 2),
      new OpenCompoundRule("n'ap", "ne", "yap", "yapmak", 2),
      new OpenCompoundRule("nol", "ne", "ol", "olmak", 2),
      new OpenCompoundRule("n'ol", "ne", "ol", "olmak", 2)
  );

  private final TurkishMorphology morphology;
  private final FormalSurfaceRealizer surfaceRealizer;

  InformalContractionExpander(
      TurkishMorphology morphology,
      FormalSurfaceRealizer surfaceRealizer) {
    this.morphology = morphology;
    this.surfaceRealizer = surfaceRealizer;
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
      if (FormalSurfaceRealizer.matchesCanonicalOrAscii(word, rule.sourcePrefix, true)
          && !hasNonExtRegularAnalysisExcept(word, rule.expectedLemma)) {
        String candidate = rule.targetPrefix + word.substring(rule.sourcePrefix.length());
        String realized = surfaceRealizer.realizeForLemma(candidate, rule.expectedLemma);
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
    }

    return word;
  }

  private String expandOpenCompound(String word) {
    if ("nolu".equals(word)) {
      return null;
    }
    for (OpenCompoundRule rule : OPEN_COMPOUND_RULES) {
      int prefixLen = rule.sourcePrefix.length();
      if (word.length() - prefixLen >= rule.minSuffixLen
          && FormalSurfaceRealizer.matchesCanonicalOrAscii(word, rule.sourcePrefix, true)) {
        String suffix = word.substring(prefixLen);
        String realized = surfaceRealizer.realizeForLemma(rule.secondStem + suffix, rule.expectedLemma);
        if (realized != null) {
          return rule.firstWord + " " + realized;
        }
      }
    }
    return null;
  }

  private boolean hasNonExtRegularAnalysisExcept(String word, String allowedColloquialLemma) {
    for (SingleAnalysis single : morphology.analyze(word)) {
      if (!FormalSurfaceRealizer.isRegularAnalysis(single)
          || single.getDictionaryItem().hasAttribute(RootAttribute.Ext)) {
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
}
