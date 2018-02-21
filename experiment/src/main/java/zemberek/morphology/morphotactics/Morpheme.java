package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PrimaryPos;

public class Morpheme {

  public final String name;
  public final String id;

  public final PrimaryPos pos;

  public Morpheme(String name, String id) {
    this.name = name;
    this.id = id;
    this.pos = PrimaryPos.Unknown;
  }

  public Morpheme(String name, String id, PrimaryPos pos) {
    this.name = name;
    this.pos = pos;
    this.id = id;
  }

}
