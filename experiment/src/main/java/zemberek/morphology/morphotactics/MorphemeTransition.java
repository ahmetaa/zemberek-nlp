package zemberek.morphology.morphotactics;

public class MorphemeTransition {

    final String id;
    final MorphemeState from;
    final MorphemeState to;
    final String format;

    public MorphemeTransition(MorphemeState from, MorphemeState to, String format) {
        this.id = from.id + "_" + to.id + "_" + format;
        this.format = format;
        this.from = from;
        this.to = to;
        from.addOutgoing(this);
        to.addIncoming(this);
    }

    @Override
    public String toString() {
        return id;
    }

}
