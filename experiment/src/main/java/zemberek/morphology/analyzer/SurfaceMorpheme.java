package zemberek.morphology.analyzer;

import zemberek.morphology.morphotactics.Morpheme;

public class SurfaceMorpheme {

  public final Morpheme morpheme;
  public final String surfaceForm;

  public SurfaceMorpheme(Morpheme morpheme, String surfaceForm) {
    this.morpheme = morpheme;
    this.surfaceForm = surfaceForm;
  }
}
