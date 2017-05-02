package zemberek.morphology.morphotactics;

import zemberek.core.logging.Log;

import java.util.HashSet;
import java.util.Set;

public class MorphemeState {

    final Morpheme morpheme;
    final String id;
    Set<MorphemeTransition> outgoing = new HashSet<>(2);
    Set<MorphemeTransition> incoming = new HashSet<>(2);
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

    public MorphemeState addOutgoing(SuffixTransition... suffixTransitions) {
        for (SuffixTransition suffixTransition : suffixTransitions) {
            if (outgoing.contains(suffixTransition)) {
                Log.warn("Outgoing transition %s already exist in %s", suffixTransition, this);
            }
            outgoing.add(suffixTransition);
        }
        return this;
    }

    public MorphemeState addIncoming(SuffixTransition... suffixTransitions) {
        for (SuffixTransition suffixTransition : suffixTransitions) {
            if (incoming.contains(suffixTransition)) {
                Log.warn("Incoming transition %s already exist in %s", suffixTransition, this);
            }
            incoming.add(suffixTransition);
        }
        return this;
    }

    public SuffixTransition.Builder transition(MorphemeState to) {
        return new SuffixTransition.Builder().from(this).to(to);
    }


    public SuffixTransition.Builder transition(MorphemeState to, String template) {
        return new SuffixTransition.Builder().surfaceTemplate(template).from(this).to(to);
    }

    @Override
    public String toString() {
        return "[" + id + ":" + morpheme.id + "]";
    }
}
