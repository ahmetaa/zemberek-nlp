package zemberek.morphology.morphotactics;

import zemberek.core.logging.Log;

import java.util.HashSet;
import java.util.Set;

public class LexicalState {

    final Morpheme morpheme;
    final String id;
    Set<LexicalTransition> outgoing = new HashSet<>();
    Set<LexicalTransition> incoming = new HashSet<>();
    boolean terminal = false;

    public LexicalState(String id, Morpheme morpheme, boolean terminal) {
        this.morpheme = morpheme;
        this.id = id;
        this.terminal = terminal;
    }

    public static LexicalState terminal(String id, Morpheme morpheme) {
        return new LexicalState(id, morpheme, true);
    }

    public static LexicalState nonTerminal(String id, Morpheme morpheme) {
        return new LexicalState(id, morpheme, false);
    }

    public LexicalState addOutgoing(LexicalTransition... lexicalTransitions) {
        for (LexicalTransition lexicalTransition : lexicalTransitions) {
            if (outgoing.contains(lexicalTransition)) {
                Log.warn("Outgoing transition %s already exist in %s", lexicalTransition, this);
            }
            outgoing.add(lexicalTransition);
        }
        return this;
    }

    public LexicalState addIncoming(LexicalTransition... lexicalTransitions) {
        for (LexicalTransition lexicalTransition : lexicalTransitions) {
            if (incoming.contains(lexicalTransition)) {
                Log.warn("Incoming transition %s already exist in %s", lexicalTransition, this);
            }
            incoming.add(lexicalTransition);
        }
        return this;
    }

    public LexicalTransition.Builder newTransition(LexicalState to) {
        return new LexicalTransition.Builder().from(this).to(to);
    }

    @Override
    public String toString() {
        return "[" + id + ":" + morpheme.id + "]";
    }
}
