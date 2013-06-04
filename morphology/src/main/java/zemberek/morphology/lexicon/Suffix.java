package zemberek.morphology.lexicon;

import zemberek.morphology.structure.IdItem;

/**
 * This is the representation of a Suffix. It only contains an id.
 */
public class Suffix extends IdItem {

    public static final Suffix UNKNOWN = new Suffix("Unkown");

    public Suffix(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Suffix suffix = (Suffix) o;

        return id.equals(suffix.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
