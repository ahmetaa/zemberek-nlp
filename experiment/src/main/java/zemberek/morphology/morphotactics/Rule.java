package zemberek.morphology.morphotactics;

public interface Rule {
    // rule is accepted.
    boolean canPass(GraphVisitor visitor);
}
