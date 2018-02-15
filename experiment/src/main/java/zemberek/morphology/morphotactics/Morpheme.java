package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PrimaryPos;

public class Morpheme {

  public final String id;

  public final PrimaryPos pos;

  public Morpheme(String id) {
    this.id = id;
    this.pos = PrimaryPos.Unknown;
  }

  public Morpheme(String id, PrimaryPos pos) {
    this.id = id;
    this.pos = pos;
  }

}
