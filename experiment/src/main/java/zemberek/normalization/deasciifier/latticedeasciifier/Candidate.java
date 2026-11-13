package zemberek.normalization.deasciifier.latticedeasciifier;

import java.util.List;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;

/**
 * Immutable candidate restoration for a token in the sentence lattice, carrying its morphological
 * attributes and precomputed 64-bit feature hashes.
 */
public final class Candidate {

  public static final Candidate BOUNDARY =
      new Candidate(
          "<s>",
          "<s>",
          PrimaryPos.Unknown,
          SecondaryPos.None,
          PrimaryPos.Unknown,
          "Nom",
          "Pnon",
          false,
          false,
          false,
          false,
          false,
          true,
          0,
          0);

  public final String surface;
  public final String lemma;
  public final PrimaryPos pos;
  public final SecondaryPos secondaryPos;
  public final PrimaryPos lastGroupPos;
  public final String caseMorpheme;
  public final String possessiveMorpheme;
  public final boolean plural;
  public final boolean informal;
  public final boolean negativeOrImperative;
  public final boolean question;
  public final boolean circumflexVariant;
  public final boolean oovFallback;
  public final int foreignStemLength;
  public final int morphemeGroupCount;

  // Precomputed hashes and vowel-harmony states for zero-allocation lattice scoring:
  public final long surfaceHash;
  public final long lemmaHash;
  public final long baseLemmaHash;
  public final long morphTagHash;
  public final long caseHash;
  public final int lastVowelHarmony;
  public final boolean contentWord;

  public Candidate(
      String surface,
      String lemma,
      PrimaryPos pos,
      SecondaryPos secondaryPos,
      PrimaryPos lastGroupPos,
      String caseMorpheme,
      String possessiveMorpheme,
      boolean plural,
      boolean informal,
      boolean negativeOrImperative,
      boolean question,
      boolean circumflexVariant,
      boolean oovFallback,
      int foreignStemLength,
      int morphemeGroupCount) {
    this.surface = surface;
    this.lemma = lemma;
    this.pos = pos;
    this.secondaryPos = secondaryPos;
    this.lastGroupPos = lastGroupPos;
    this.caseMorpheme = caseMorpheme;
    this.possessiveMorpheme = possessiveMorpheme;
    this.plural = plural;
    this.informal = informal;
    this.negativeOrImperative = negativeOrImperative;
    this.question = question;
    this.circumflexVariant = circumflexVariant;
    this.oovFallback = oovFallback;
    this.foreignStemLength = foreignStemLength;
    this.morphemeGroupCount = morphemeGroupCount;

    this.surfaceHash = FeatureHash.hashString(surface);
    this.lemmaHash = FeatureHash.hashString(lemma);
    String strippedLemma = TurkishTextCodec.stripCircumflex(lemma);
    this.baseLemmaHash =
        strippedLemma.equals(lemma) ? lemmaHash : FeatureHash.hashString(strippedLemma);
    this.caseHash = FeatureHash.hashString(caseMorpheme);
    this.morphTagHash =
        oovFallback
            ? FeatureHash.hashString("OOV")
            : FeatureHash.hashString(
                pos.shortForm
                    + ":"
                    + secondaryPos.shortForm
                    + ":"
                    + lastGroupPos.shortForm
                    + ":"
                    + caseMorpheme
                    + ":"
                    + possessiveMorpheme
                    + (plural ? ":PL" : "")
                    + (negativeOrImperative ? ":NEG" : "")
                    + (question ? ":Q" : ""));
    this.lastVowelHarmony = computeLastVowelHarmony(surface);
    this.contentWord = computeIsContentWord(pos, surface, oovFallback);
  }

  public static Candidate oov(String lowerSurface, int foreignStemLength) {
    return new Candidate(
        lowerSurface,
        lowerSurface,
        PrimaryPos.Unknown,
        SecondaryPos.None,
        PrimaryPos.Unknown,
        "Nom",
        "Pnon",
        false,
        false,
        false,
        false,
        false,
        true,
        foreignStemLength,
        1);
  }

  /**
   * Encodes the last vowel's 4-way Turkish harmony class ({@code 1=a/ı}, {@code 2=e/i},
   * {@code 3=o/u}, {@code 4=ö/ü}, {@code 0=none}) for clitic agreement ({@code koyayım mı?}).
   */
  public static int computeLastVowelHarmony(String word) {
    for (int i = word.length() - 1; i >= 0; i--) {
      switch (word.charAt(i)) {
        case 'a', 'ı' -> {
          return 1;
        }
        case 'e', 'i', 'â', 'î' -> {
          return 2;
        }
        case 'o', 'u' -> {
          return 3;
        }
        case 'ö', 'ü', 'û' -> {
          return 4;
        }
        default -> {}
      }
    }
    return 0;
  }

  public static boolean containsSurface(List<Candidate> candidates, String surface) {
    for (Candidate c : candidates) {
      if (c.surface.equals(surface)) {
        return true;
      }
    }
    return false;
  }

  public static boolean addIfSurfaceAbsent(List<Candidate> candidates, Candidate candidate) {
    if (containsSurface(candidates, candidate.surface)) {
      return false;
    }
    candidates.add(candidate);
    return true;
  }

  private static boolean computeIsContentWord(PrimaryPos pos, String surface, boolean oov) {
    if (surface.length() < 3) {
      return false;
    }
    if (oov) {
      return surface.length() >= 4;
    }
    return pos == PrimaryPos.Noun
        || pos == PrimaryPos.Verb
        || pos == PrimaryPos.Adjective
        || pos == PrimaryPos.Adverb;
  }
}
