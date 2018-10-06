package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PrimaryPos;

public class Morpheme {

  public static final Morpheme UNKNOWN = instance("Unknown", "Unknown");

  public final String name;
  public final String id;
  public final PrimaryPos pos;
  public final boolean derivational;
  public final boolean informal;
  public final Morpheme mappedMorpheme;

  public Morpheme(Builder builder) {
    this.name = builder.name;
    this.id = builder.id;
    this.informal = builder.informal;
    this.mappedMorpheme = builder.mappedMorpheme;
    this.derivational = builder.derivational;
    this.pos = builder.pos;
  }

  public static Morpheme instance(String name, String id) {
    return builder(name, id).build();
  }

  public static Morpheme instance(String name, String id, PrimaryPos pos) {
    return builder(name, id).pos(pos).build();
  }

  public static Morpheme derivational(String name, String id) {
    return builder(name, id).derivational().build();
  }

  public static Builder builder(String name, String id) {
    return new Builder(name, id);
  }

  public static class Builder {

    private String name;
    private String id;
    private PrimaryPos pos;
    private boolean derivational = false;
    private boolean informal = false;
    private Morpheme mappedMorpheme;

    public Builder(String name, String id) {
      this.name = name;
      this.id = id;
    }

    public Builder pos(PrimaryPos pos) {
      this.pos = pos;
      return this;
    }

    public Builder derivational() {
      this.derivational = true;
      return this;
    }

    public Builder informal() {
      this.informal = true;
      return this;
    }

    public Builder mappedMorpheme(Morpheme morpheme) {
      this.mappedMorpheme = morpheme;
      return this;
    }

    public Morpheme build() {
      return new Morpheme(this);
    }

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
    return id.equals(morpheme.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return name + ':' + id;
  }
}
