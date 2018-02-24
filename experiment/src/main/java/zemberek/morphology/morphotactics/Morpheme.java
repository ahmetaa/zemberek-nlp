package zemberek.morphology.morphotactics;

import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Morpheme morpheme = (Morpheme) o;
    return Objects.equals(id, morpheme.id);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id);
  }
}
