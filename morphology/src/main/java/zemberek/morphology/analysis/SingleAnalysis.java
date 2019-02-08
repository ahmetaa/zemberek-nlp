package zemberek.morphology.analysis;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.StemAndEnding;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * This class represents a single morphological analysis result.
 */
public class SingleAnalysis {

  // Dictionary Item of the analysis.
  private DictionaryItem item;

  // Contains Morphemes and their surface form (actual appearance in the normalized input)
  // List also contain the root (unchanged or modified) of the Dictionary item.
  // For example, for normalized input "kedilere"
  // This list may contain "kedi:Noun, ler:A3pl , e:Dat" information.
  private List<MorphemeData> morphemeDataList;

  // groupBoundaries holds the index values of morphemes.
  private int[] groupBoundaries;

  // cached hash value.
  private int hash;

  public SingleAnalysis(
      DictionaryItem item,
      List<MorphemeData> morphemeDataList,
      int[] groupBoundaries) {
    this.item = item;
    this.morphemeDataList = morphemeDataList;
    this.groupBoundaries = groupBoundaries;
    this.hash = hashCode();
  }

  public static SingleAnalysis unknown(String input) {
    DictionaryItem item = DictionaryItem.UNKNOWN;
    MorphemeData s = new MorphemeData(Morpheme.UNKNOWN, input);
    int[] boundaries = {0};
    return new SingleAnalysis(item, Collections.singletonList(s), boundaries);
  }

  public static SingleAnalysis dummy(String input, DictionaryItem item) {
    MorphemeData s = new MorphemeData(Morpheme.UNKNOWN, input);
    int[] boundaries = {0};
    return new SingleAnalysis(item, Collections.singletonList(s), boundaries);
  }

  public String surfaceForm() {
    return getStem() + getEnding();
  }

  public static class MorphemeGroup {

    List<MorphemeData> morphemes;

    public MorphemeGroup(List<MorphemeData> morphemes) {
      this.morphemes = morphemes;
    }

    public List<MorphemeData> getMorphemes() {
      return morphemes;
    }

    public PrimaryPos getPos() {
      for (MorphemeData mSurface : morphemes) {
        if (mSurface.morpheme.pos != null && mSurface.morpheme.pos != PrimaryPos.Unknown) {
          return mSurface.morpheme.pos;
        }
      }
      return PrimaryPos.Unknown;
    }

    public String surfaceForm() {
      StringBuilder sb = new StringBuilder();
      for (MorphemeData mSurface : morphemes) {
        sb.append(mSurface.surface);
      }
      return sb.toString();
    }

    public String surfaceFormSkipPosRoot() {
      StringBuilder sb = new StringBuilder();
      for (MorphemeData mSurface : morphemes) {
        if (mSurface.morpheme.pos != null) {
          continue;
        }
        sb.append(mSurface.surface);
      }
      return sb.toString();
    }

    public String lexicalForm() {
      StringBuilder sb = new StringBuilder();
      for (MorphemeData mSurface : morphemes) {
        sb.append(mSurface.morpheme.id);
      }
      return sb.toString();
    }

  }

  public boolean containsInformalMorpheme() {
    return getMorphemes().stream().anyMatch(m -> m.informal);
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
    for (int i = 1; i < morphemeDataList.size(); i++) {
      MorphemeData mSurface = morphemeDataList.get(i);
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
    return morphemeDataList.get(0).surface;
  }

  public boolean containsMorpheme(Morpheme morpheme) {
    for (MorphemeData morphemeData : morphemeDataList) {
      if (morphemeData.morpheme == morpheme) {
        return true;
      }
    }
    return false;
  }

  /**
   * Splits the parse into stem and ending. Such as:
   * <pre>
   * "kitaplar" -> "kitap-lar"
   * "kitabımdaki" -> "kitab-ımdaki"
   * "kitap" -> "kitap-"
   * </pre>
   *
   * @return a StemAndEnding instance carrying stem and ending. If ending has no surface content
   * empty string is used.
   */
  public StemAndEnding getStemAndEnding() {
    return new StemAndEnding(getStem(), getEnding());
  }


  public DictionaryItem getDictionaryItem() {
    return item;
  }

  public boolean isUnknown() {
    return item.isUnknown();
  }

  public boolean isRuntime() {
    return item.hasAttribute(RootAttribute.Runtime);
  }


  public List<MorphemeData> getMorphemeDataList() {
    return morphemeDataList;
  }

  public List<Morpheme> getMorphemes() {
    return morphemeDataList.stream().map(s -> s.morpheme).collect(Collectors.toList());
  }

  public MorphemeGroup getGroup(int groupIndex) {
    if (groupIndex < 0 || groupIndex >= groupBoundaries.length) {
      throw new IllegalArgumentException("There are only " + groupBoundaries.length +
          " morpheme groups. But input is " + groupIndex);
    }
    int endIndex = groupIndex == groupBoundaries.length - 1 ?
        morphemeDataList.size() : groupBoundaries[groupIndex + 1];

    return new MorphemeGroup(morphemeDataList.subList(groupBoundaries[groupIndex], endIndex));
  }

  // container for Morphemes and their surface forms.
  public static class MorphemeData {

    public final Morpheme morpheme;
    public final String surface;

    public MorphemeData(Morpheme morpheme, String surface) {
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

      MorphemeData that = (MorphemeData) o;

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

  public MorphemeGroup getLastGroup() {
    return getGroup(groupBoundaries.length - 1);
  }

  public MorphemeGroup[] getGroups() {
    MorphemeGroup[] groups = new MorphemeGroup[groupBoundaries.length];
    for (int i = 0; i < groups.length; i++) {
      groups[i] = getGroup(i);
    }
    return groups;
  }


  private static final ConcurrentHashMap<Morpheme, MorphemeData> emptyMorphemeCache =
      new ConcurrentHashMap<>();

  // Here we generate a SingleAnalysis from a search path.
  public static SingleAnalysis fromSearchPath(SearchPath searchPath) {

    List<MorphemeData> morphemes = new ArrayList<>(searchPath.transitions.size());

    int derivationCount = 0;

    for (SurfaceTransition transition : searchPath.getTransitions()) {

      if (transition.isDerivative()) {
        derivationCount++;
      }

      Morpheme morpheme = transition.getMorpheme();

      // we skip these two morphemes as they create visual noise and does not carry much information.
      if (morpheme == TurkishMorphotactics.nom || morpheme == TurkishMorphotactics.pnon) {
        continue;
      }

      // if empty, use the cache.
      if (transition.surface.isEmpty()) {
        MorphemeData morphemeData = emptyMorphemeCache.get(morpheme);
        if (morphemeData == null) {
          morphemeData = new MorphemeData(morpheme, "");
          emptyMorphemeCache.put(morpheme, morphemeData);
        }
        morphemes.add(morphemeData);
        continue;
      }

      MorphemeData suffixSurface = new MorphemeData(morpheme, transition.surface);
      morphemes.add(suffixSurface);
    }

    int[] groupBoundaries = new int[derivationCount + 1];
    groupBoundaries[0] = 0; // we assume there is always an IG

    int morphemeCounter = 0, derivationCounter = 1;
    for (MorphemeData morphemeData : morphemes) {
      if (morphemeData.morpheme.derivational) {
        groupBoundaries[derivationCounter] = morphemeCounter;
        derivationCounter++;
      }
      morphemeCounter++;
    }

    // if dictionary item is `Dummy`, use the referenced item.
    // `Dummy` items are usually generated for some compound words. For example for `zeytinyağı`
    // a DictionaryItem is generated with root "zeytinyağ". But here we switch to the original.
    DictionaryItem item = searchPath.getDictionaryItem();
    if (item.hasAttribute(RootAttribute.Dummy)) {
      item = item.getReferenceItem();
    }
    return new SingleAnalysis(item, morphemes, groupBoundaries);
  }

  public boolean containsAnyMorpheme(Morpheme... morphemes) {
    for (Morpheme morpheme : morphemes) {
      if (containsMorpheme(morpheme)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method is used for modifying the dictionary item and stem of an analysis without changing
   * the suffix morphemes. This is used for generating result for inputs like "5'e"
   *
   * @param item new DictionaryItem
   * @param stem new stem
   * @return new SingleAnalysis object with given DictionaryItem and stem.
   */
  SingleAnalysis copyFor(DictionaryItem item, String stem) {
    // copy morpheme-surface list.
    List<MorphemeData> data = new ArrayList<>(morphemeDataList);
    // replace the stem surface. it is in the first morpheme.
    data.set(0, new MorphemeData(data.get(0).morpheme, stem));
    return new SingleAnalysis(item, data, groupBoundaries.clone());
  }

  /**
   * Returns surface forms list of all root and derivational roots of a parse. Examples:
   * <pre>
   * "kitaplar"  ->["kitap"]
   * "kitabım"   ->["kitab"]
   * "kitaplaşır"->["kitap", "kitaplaş"]
   * "kavrulduk" ->["kavr","kavrul"]
   * </pre>
   */
  public List<String> getStems() {
    List<String> stems = Lists.newArrayListWithCapacity(2);
    stems.add(getStem());
    String previousStem = getGroup(0).surfaceForm();
    if (groupBoundaries.length > 1) {
      for (int i = 1; i < groupBoundaries.length; i++) {
        MorphemeGroup ig = getGroup(i);
        MorphemeData suffixData = ig.morphemes.get(0);

        String surface = suffixData.surface;
        String stem = previousStem + surface;
        if (!stems.contains(stem)) {
          stems.add(stem);
        }
        previousStem = previousStem + ig.surfaceForm();
      }
    }
    return stems;
  }


  /**
   * Returns list of all lemmas of a parse. Examples:
   * <pre>
   * "kitaplar"  ->["kitap"]
   * "kitabım"   ->["kitap"]
   * "kitaplaşır"->["kitap", "kitaplaş"]
   * "kitaplaş"  ->["kitap", "kitaplaş"]
   * "arattıragörür" -> ["ara","arat","arattır","arattıragör"]
   * </pre>
   */
  public List<String> getLemmas() {
    List<String> lemmas = Lists.newArrayListWithCapacity(2);
    lemmas.add(item.root);

    String previousStem = getGroup(0).surfaceForm();
    if (!previousStem.equals(item.root)) {
      if (previousStem.endsWith("ğ")) {
        previousStem = previousStem.substring(0, previousStem.length() - 1) + "k";
      }
    }

    if (groupBoundaries.length > 1) {
      for (int i = 1; i < groupBoundaries.length; i++) {
        MorphemeGroup ig = getGroup(i);
        MorphemeData suffixData = ig.morphemes.get(0);

        String surface = suffixData.surface;
        String stem = previousStem + surface;
        if (stem.endsWith("ğ")) {
          stem = stem.substring(0, stem.length() - 1) + "k";
        }
        if (!lemmas.contains(stem)) {
          lemmas.add(stem);
        }
        previousStem = previousStem + ig.surfaceForm();
      }
    }
    return lemmas;
  }

  @Override
  public String toString() {
    return formatLong();
  }

  public String formatLexical() {
    return AnalysisFormatters.DEFAULT_LEXICAL.format(this);
  }

  /**
   * Formats only the morphemes. Dictionary item information is not included.
   *
   * @return formatted
   */
  public String formatMorphemesLexical() {
    return AnalysisFormatters.DEFAULT_LEXICAL_ONLY_MORPHEMES.format(this);
  }

  public PrimaryPos getPos() {
    return getGroup(groupCount() - 1).getPos();
  }

  public String formatLong() {
    return AnalysisFormatters.DEFAULT.format(this);
  }

  public int groupCount() {
    return groupBoundaries.length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingleAnalysis that = (SingleAnalysis) o;

    if (hash != that.hash) {
      return false;
    }
    if (!item.equals(that.item)) {
      return false;
    }
    return morphemeDataList.equals(that.morphemeDataList);
  }

  @Override
  public int hashCode() {
    if (hash != 0) {
      return hash;
    }
    int result = item.hashCode();
    result = 31 * result + morphemeDataList.hashCode();
    result = 31 * result + hash;
    return result;
  }
}
