package zemberek.normalization;

import java.util.List;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;

/**
 * Shared morphological predicates, one-way canonical/ASCII matching helpers, and surface
 * realization from formal, informal, and diacritic-tolerant {@link SingleAnalysis} results.
 */
final class FormalSurfaceRealizer {

  private final TurkishMorphology morphology;
  private final TurkishMorphology informalAsciiTolerantMorphology;
  private final InformalAnalysisConverter analysisConverter;

  FormalSurfaceRealizer(
      TurkishMorphology morphology,
      TurkishMorphology informalAsciiTolerantMorphology,
      InformalAnalysisConverter analysisConverter) {
    this.morphology = morphology;
    this.informalAsciiTolerantMorphology = informalAsciiTolerantMorphology;
    this.analysisConverter = analysisConverter;
  }

  TurkishMorphology morphology() {
    return morphology;
  }

  TurkishMorphology informalAsciiTolerantMorphology() {
    return informalAsciiTolerantMorphology;
  }

  InformalAnalysisConverter analysisConverter() {
    return analysisConverter;
  }

  static boolean isKnownLexical(SingleAnalysis analysis) {
    return !analysis.isUnknown() && !analysis.isRuntime();
  }

  static boolean isRegularAnalysis(SingleAnalysis analysis) {
    return isKnownLexical(analysis)
        && analysis.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun
        && analysis.getDictionaryItem().secondaryPos != SecondaryPos.Abbreviation;
  }

  static boolean isExtendedInterjection(SingleAnalysis analysis) {
    DictionaryItem item = analysis.getDictionaryItem();
    return item.primaryPos == PrimaryPos.Interjection && item.hasAttribute(RootAttribute.Ext);
  }

  static boolean matchesCanonicalOrAscii(String input, String canonical, boolean prefixMatch) {
    boolean validLength = prefixMatch
        ? input.length() >= canonical.length()
        : input.length() == canonical.length();
    if (!validLength) {
      return false;
    }
    for (int i = 0; i < canonical.length(); i++) {
      char in = input.charAt(i);
      char target = canonical.charAt(i);
      if (in != target && TurkishAlphabet.INSTANCE.getAsciiEqual(target) != in) {
        return false;
      }
    }
    return true;
  }

  static boolean stripsExplicitTurkishDiacritic(String src, String cand) {
    if (src.length() != cand.length()) {
      return false;
    }
    for (int i = 0; i < src.length(); i++) {
      char s = src.charAt(i);
      char c = cand.charAt(i);
      if ((s == 'ç' && c == 'c')
          || (s == 'ğ' && c == 'g')
          || (s == 'ö' && c == 'o')
          || (s == 'ş' && c == 's')
          || (s == 'ü' && c == 'u')) {
        return true;
      }
    }
    return false;
  }

  boolean hasRegularAnalysis(String word) {
    return morphology.analyze(word).stream().anyMatch(FormalSurfaceRealizer::isRegularAnalysis);
  }

  boolean hasRegularOrAsciiAnalysis(String word) {
    return hasRegularAnalysis(word)
        || informalAsciiTolerantMorphology.analyze(word).stream()
            .anyMatch(FormalSurfaceRealizer::isRegularAnalysis);
  }

  boolean hasAsciiRootWordAnalysis(String word) {
    return word.length() >= 3
        && informalAsciiTolerantMorphology.analyze(word).stream()
            .anyMatch(a -> isRegularAnalysis(a) && a.getEnding().isEmpty());
  }

  boolean isValidLexicalWord(String word) {
    return morphology.analyze(word).stream()
        .anyMatch(a -> isRegularAnalysis(a) && !isExtendedInterjection(a));
  }

  String realizeRegularSurface(String candidate) {
    if (hasRegularAnalysis(candidate)) {
      return candidate;
    }
    for (SingleAnalysis analysis : informalAsciiTolerantMorphology.analyze(candidate)) {
      if (!isRegularAnalysis(analysis)) {
        continue;
      }
      String surface = toFormalSurface(candidate, analysis);
      if (surface != null) {
        return surface;
      }
    }
    return null;
  }

  String realizeRegularSurfaceOrIdentity(String candidate) {
    String realized = realizeRegularSurface(candidate);
    return realized != null ? realized : candidate;
  }

  String toFormalSurface(String candidate, SingleAnalysis analysis) {
    if (analysis.containsInformalMorpheme()) {
      Result converted = analysisConverter.convert(candidate, analysis);
      return converted != null ? converted.surface : null;
    }
    for (Result r : generateSurfaces(analysis)) {
      if (TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(candidate, r.surface)) {
        return r.surface;
      }
    }
    return null;
  }

  String realizeForLemma(String candidate, String expectedLemma) {
    for (SingleAnalysis analysis : morphology.analyze(candidate)) {
      if (isKnownLexical(analysis)
          && expectedLemma.equals(analysis.getDictionaryItem().lemma)) {
        return candidate;
      }
    }

    String bestSurface = null;
    int bestDiffs = Integer.MAX_VALUE;
    for (SingleAnalysis analysis : informalAsciiTolerantMorphology.analyze(candidate)) {
      if (!isKnownLexical(analysis)
          || !expectedLemma.equals(analysis.getDictionaryItem().lemma)) {
        continue;
      }
      for (String surface : matchingSurfaces(candidate, analysis)) {
        int diffs = diacriticMismatchCount(candidate, surface);
        if (diffs < bestDiffs) {
          bestDiffs = diffs;
          bestSurface = surface;
        }
      }
    }
    return bestSurface;
  }

  private List<String> matchingSurfaces(String candidate, SingleAnalysis analysis) {
    if (analysis.containsInformalMorpheme()) {
      Result converted = analysisConverter.convert(candidate, analysis);
      return converted != null ? List.of(converted.surface) : List.of();
    }
    return generateSurfaces(analysis).stream()
        .map(r -> r.surface)
        .filter(surface -> TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(candidate, surface))
        .toList();
  }

  List<Result> generateSurfaces(SingleAnalysis analysis) {
    return morphology.getWordGenerator().generate(
        analysis.getDictionaryItem(), analysis.getMorphemes());
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
