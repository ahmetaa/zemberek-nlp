package zemberek.morphology._analyzer;

import java.util.List;
import zemberek.morphology._morphotactics.Morpheme;
import zemberek.morphology.lexicon.DictionaryItem;

// This class represents a single morphological analysis result
// TODO: Not Yet In Use
public class _SingleAnalysis {

  // Dictionary Item of the analysis.
  DictionaryItem item;

  // Morphemes contain Morphemes and their surface form (actual appearance in the normalized input
  // List also contain the root (unchanged or modified) of the Dictionary item.
  // For example, for normalized input "kedilere"
  // This list may contain "kedi:Noun, ler:A3pl , e:Dat" information.
  List<MorphemeSurface> morphemes;

  // container for Morphemes and their surface forms.
  static class MorphemeSurface {

    final Morpheme morpheme;
    final String surface;

    public MorphemeSurface(Morpheme morpheme, String surface) {
      this.morpheme = morpheme;
      this.surface = surface;
    }
  }

}
