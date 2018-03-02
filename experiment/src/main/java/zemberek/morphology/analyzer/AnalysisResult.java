package zemberek.morphology.analyzer;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.StemTransition;

public class AnalysisResult {

  // TODO: these two may be part of the [morphemes] list.
  public final DictionaryItem dictionaryItem;
  public final String root;

  List<MorphemeSurfaceForm> morphemes;

  public AnalysisResult(SearchPath searchPath) {
    StemTransition st = searchPath.getStemTransition();
    this.dictionaryItem = st.item;
    this.root = st.surface;
    this.morphemes = searchPath.getMorphemes();
  }

  public DictionaryItem getDictionaryItem() {
    return dictionaryItem;
  }

  public String getRoot() {
    return root;
  }

  public List<MorphemeSurfaceForm> getMorphemes() {
    return morphemes;
  }

  @Override
  public String toString() {
    return "[" +
        String.join(" + ", morphemes.stream()
            .map(MorphemeSurfaceForm::toString)
            .collect(Collectors.toList())) + "]";
  }

  public String shortForm() {
    return
        String.join(" + ", morphemes.stream()
            .map(MorphemeSurfaceForm::toMorphemeString)
            .collect(Collectors.toList()));
  }

  public String lexicalForm() {
    return
        String.join(" + ", morphemes.stream()
            .map(s -> s.morphemeState.morpheme.id)
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
