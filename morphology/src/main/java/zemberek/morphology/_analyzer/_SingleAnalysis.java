package zemberek.morphology._analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology._morphotactics.Morpheme;
import zemberek.morphology.lexicon.DictionaryItem;

// This class represents a single morphological analysis result
public class _SingleAnalysis {

  // Dictionary Item of the analysis.
  private DictionaryItem item;

  // Contains Morphemes and their surface form (actual appearance in the normalized input)
  // List also contain the root (unchanged or modified) of the Dictionary item.
  // For example, for normalized input "kedilere"
  // This list may contain "kedi:Noun, ler:A3pl , e:Dat" information.
  private List<MorphemeSurface> morphemesSurfaces;

  // groupBoundaries holds the index values of morphemes.
  private int[] groupBoundaries;

  private _SingleAnalysis(
      DictionaryItem item,
      List<MorphemeSurface> morphemesSurfaces,
      int[] groupBoundaries) {
    this.item = item;
    this.morphemesSurfaces = morphemesSurfaces;
    this.groupBoundaries = groupBoundaries;
  }

  public static class MorphemeGroup {

    List<MorphemeSurface> morphemes;

    public MorphemeGroup(
        List<MorphemeSurface> morphemes) {
      this.morphemes = morphemes;
    }

    public PrimaryPos getPos() {
      for (MorphemeSurface mSurface : morphemes) {
        if (mSurface.morpheme.pos != null) {
          return mSurface.morpheme.pos;
        }
      }
      throw new IllegalStateException(
          "A morhpheme group must have a morpheme with a POS information. " + morphemes);
    }

    public String surface() {
      StringBuilder sb = new StringBuilder();
      for (MorphemeSurface mSurface : morphemes) {
        sb.append(mSurface.surface);
      }
      return sb.toString();
    }
  }

  int getMorphemeGroupCount() {
    return groupBoundaries.length;
  }

  /**
   * Returns the concatenated suffix surfaces.
   * <pre>
   *   "elmalar"      -> "lar"
   *   "elmalara"     -> "lara"
   *   "okutturdular" -> "tturdular"
   *   "arıyor"       -> "ıyor"
   * </pre>
   *
   * @return concatenated suffix surfaces.
   */
  public String getEnding() {
    StringBuilder sb = new StringBuilder();
    // skip the root.
    for (int i = 1; i < morphemesSurfaces.size(); i++) {
      MorphemeSurface mSurface = morphemesSurfaces.get(i);
      sb.append(mSurface.surface);
    }
    return sb.toString();
  }

  /**
   * Returns the stem of the word. Stem may be different than the lemma of the word.
   * <pre>
   *   "elmalar"      -> "elma"
   *   "kitabımız"    -> "kitab"
   *   "okutturdular" -> "oku"
   *   "arıyor"       -> "ar"
   * </pre>
   * TODO: decide for inputs like "12'ye and "Ankara'da"
   *
   * @return concatenated suffix surfaces.
   */
  public String getStem() {
    return morphemesSurfaces.get(0).surface;
  }

  public DictionaryItem getItem() {
    return item;
  }

  public List<MorphemeSurface> getMorphemesSurfaces() {
    return morphemesSurfaces;
  }

  public MorphemeGroup getGroup(int groupIndex) {
    if (groupIndex < 0 || groupIndex >= groupBoundaries.length) {
      throw new IllegalArgumentException("There are only " + groupBoundaries.length +
          " morpheme groups. But input is " + groupIndex);
    }
    int endIndex = groupIndex == groupBoundaries.length - 1 ?
        morphemesSurfaces.size() : groupBoundaries[groupIndex + 1];

    return new MorphemeGroup(morphemesSurfaces.subList(groupIndex, endIndex));
  }

  // container for Morphemes and their surface forms.
  public static class MorphemeSurface {

    public final Morpheme morpheme;
    public final String surface;

    public MorphemeSurface(Morpheme morpheme, String surface) {
      this.morpheme = morpheme;
      this.surface = surface;
    }

    public String toString() {
      return toMorphemeString();
    }

    public String toMorphemeString() {
      return surfaceString() + morpheme.id;
    }

    private String surfaceString() {
      return surface.isEmpty() ? "" : surface + ":";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MorphemeSurface that = (MorphemeSurface) o;

      if (!morpheme.equals(that.morpheme)) {
        return false;
      }
      return surface.equals(that.surface);
    }

    @Override
    public int hashCode() {
      int result = morpheme.hashCode();
      result = 31 * result + surface.hashCode();
      return result;
    }
  }

  private static final ConcurrentHashMap<Morpheme, MorphemeSurface> emptyMorphemeCache =
      new ConcurrentHashMap<>();

  // Here we generate a _SingleAnalysis from a search path.
  public static _SingleAnalysis fromSearchPath(SearchPath searchPath) {

    List<MorphemeSurface> morphemes = new ArrayList<>(searchPath.transitions.size());

    int derivationCount = 0;

    for (SurfaceTransition transition : searchPath.getTransitions()) {

      if (transition.isDerivative()) {
        derivationCount++;
      }

      Morpheme morpheme = transition.getMorpheme();

      // if empty, use the cache.
      if (transition.surface.isEmpty()) {
        MorphemeSurface suffixSurface = emptyMorphemeCache.get(morpheme);
        if (suffixSurface == null) {
          suffixSurface = new MorphemeSurface(morpheme, "");
          emptyMorphemeCache.put(morpheme, suffixSurface);
        }
        morphemes.add(suffixSurface);
        continue;
      }

      MorphemeSurface suffixSurface = new MorphemeSurface(morpheme, transition.surface);
      morphemes.add(suffixSurface);
    }

    int[] groupBoundaries = new int[derivationCount + 1];
    groupBoundaries[0] = 0; // we assume there is always an IG

    int morphemeCounter = 0, derivationCounter = 1;
    for (SurfaceTransition transition : searchPath.getTransitions()) {
      if (transition.isDerivative()) {
        groupBoundaries[derivationCounter] = morphemeCounter;
        derivationCounter++;
      }
      morphemeCounter++;
    }

    return new _SingleAnalysis(searchPath.getDictionaryItem(), morphemes, groupBoundaries);
  }

  @Override
  public String toString() {
    return AnalysisFormatters.lexicalForm().format(this);
  }

  public String shortForm() {
    return AnalysisFormatters.shortForm().format(this);
  }

  public String lexicalForm() {
    return AnalysisFormatters.lexicalForm().format(this);
  }


}
