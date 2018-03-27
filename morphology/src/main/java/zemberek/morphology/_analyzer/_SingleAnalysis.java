package zemberek.morphology._analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.morphology._morphotactics.Morpheme;
import zemberek.morphology._morphotactics.StemTransition;
import zemberek.morphology.lexicon.DictionaryItem;

// This class represents a single morphological analysis result
// TODO: Not Yet In Use
public class _SingleAnalysis {

  // Dictionary Item of the analysis.
  private DictionaryItem item;

  // Contains Morphemes and their surface form (actual appearance in the normalized input)
  // List also contain the root (unchanged or modified) of the Dictionary item.
  // For example, for normalized input "kedilere"
  // This list may contain "kedi:Noun, ler:A3pl , e:Dat" information.
  private List<MorphemeSurface> morphemes;

  // groupBoundaries holds the index values of morphemes.
  private int[] groupBoundaries;

  private _SingleAnalysis(
      DictionaryItem item,
      List<MorphemeSurface> morphemes,
      int[] groupBoundaries) {
    this.item = item;
    this.morphemes = morphemes;
    this.groupBoundaries = groupBoundaries;
  }

  public List<MorphemeSurface> getMorphemes() {
    return morphemes;
  }

  public List<MorphemeSurface> getGroup(int groupIndex) {
    if (groupIndex < 0 || groupIndex >= groupBoundaries.length) {
      throw new IllegalArgumentException("There are only " + groupBoundaries.length +
          " morpheme groups. But input is " + groupIndex);
    }
    int endIndex = groupIndex == groupBoundaries.length - 1 ?
        morphemes.size() : groupBoundaries[groupIndex + 1];

    return new ArrayList<>(morphemes.subList(groupIndex, endIndex));
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

    StemTransition st = searchPath.getStemTransition();
    List<MorphemeSurface> morphemes = new ArrayList<>(searchPath.transitions.size());

    MorphemeSurface stemSurface = new MorphemeSurface(st.to.morpheme, st.surface);
    morphemes.add(stemSurface);

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

}
