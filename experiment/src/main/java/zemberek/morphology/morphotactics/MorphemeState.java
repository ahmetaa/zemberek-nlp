package zemberek.morphology.morphotactics;

import zemberek.core.logging.Log;

import java.util.HashSet;
import java.util.Set;

public class MorphemeState {

    final Morpheme morpheme;
    final String id;
    Set<Transition> outgoing = new HashSet<>();
    Set<Transition> incoming = new HashSet<>();
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

    public MorphemeState addOutgoing(Transition... transitions) {
        for (Transition transition : transitions) {
            if (outgoing.contains(transition)) {
                Log.warn("Outgoing transition %s already exist in %s", transition, this);
            }
            outgoing.add(transition);
        }
        return this;
    }

    public MorphemeState addIncoming(Transition... transitions) {
        for (Transition transition : transitions) {
            if (incoming.contains(transition)) {
                Log.warn("Incoming transition %s already exist in %s", transition, this);
            }
            incoming.add(transition);
        }
        return this;
    }

    public Transition.Builder newTransition(MorphemeState to) {
        return new Transition.Builder().from(this).to(to);
    }

    @Override
    public String toString() {
        return "[" + id + ":" + morpheme.id + "]";
    }
}
