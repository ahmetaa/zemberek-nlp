package zemberek.morphology._analyzer;

import java.util.List;
import java.util.stream.Collectors;
import zemberek.morphology._morphotactics.StemTransition;
import zemberek.morphology.lexicon.DictionaryItem;

public class AnalysisResult {

  // TODO: these two may be part of the [morphemes] list.
  public final DictionaryItem dictionaryItem;
  public final String root;

  List<SurfaceTransition> morphemes;

  public AnalysisResult(SearchPath searchPath) {
    StemTransition st = searchPath.getStemTransition();
    this.dictionaryItem = st.item;
    this.root = st.surface;
    this.morphemes = searchPath.getTransitions();
  }

  public DictionaryItem getDictionaryItem() {
    return dictionaryItem;
  }

  public String getRoot() {
    return root;
  }

  public List<SurfaceTransition> getMorphemes() {
    return morphemes;
  }

  @Override
  public String toString() {
    return "[" +
        String.join(" + ", morphemes.stream()
            .map(SurfaceTransition::toString)
            .collect(Collectors.toList())) + "]";
  }

  public String shortForm() {
    return
        String.join(" + ", morphemes.stream()
            .map(SurfaceTransition::toMorphemeString)
            .collect(Collectors.toList()));
  }

  public String lexicalForm() {
    return
        String.join(" + ", morphemes.stream()
            .map(s -> s.getMorpheme().id)
            .collect(Collectors.toList()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalysisResult that = (AnalysisResult) o;
    return o.toString().equals(that.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
