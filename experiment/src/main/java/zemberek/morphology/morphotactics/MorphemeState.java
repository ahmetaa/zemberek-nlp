package zemberek.morphology.morphotactics;

import zemberek.core.logging.Log;

import java.util.HashSet;
import java.util.Set;

public class MorphemeState {

    final Morpheme morpheme;
    final String id;
    Set<MorphemeTransition> outgoing = new HashSet<>();
    Set<MorphemeTransition> incoming = new HashSet<>();
    boolean terminal = false;

    public MorphemeState(String id, Morpheme morpheme, boolean terminal) {
        this.morpheme = morpheme;
        this.id = id;
        this.terminal = terminal;
    }

    public static MorphemeState terminal(String id, Morpheme morpheme) {
        return new MorphemeState(id, morpheme, true);
    }

    public static MorphemeState nonTerminal(String id, Morpheme morpheme) {
        return new MorphemeState(id, morpheme, false);
    }

    public MorphemeState addOutgoing(MorphemeTransition... transitions) {
        for (MorphemeTransition transition : transitions) {
            if (outgoing.contains(transition)) {
                Log.warn("Outgoing transition %s already exist in %s", transition, this);
            }
            outgoing.add(transition);
        }
        return this;
    }

    public MorphemeState addIncoming(MorphemeTransition... transitions) {
        for (MorphemeTransition transition : transitions) {
            if (incoming.contains(transition)) {
                Log.warn("Incoming transition %s already exist in %s", transition, this);
            }
            incoming.add(transition);
        }
        return this;
    }

    @Override
    public String toString() {
        return "[" + id + ":" + morpheme.id + "]";
    }
}
