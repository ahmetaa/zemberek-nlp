package zemberek.morphology.morphotactics;

public class Morpheme {

    public final String id;
    public final MorphemeGroup group;

    public Morpheme(String id, MorphemeGroup group) {
        this.id = id;
        this.group = group;
    }
}
