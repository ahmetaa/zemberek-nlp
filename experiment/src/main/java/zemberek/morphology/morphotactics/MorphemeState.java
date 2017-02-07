package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MorphemeState {

    final Morpheme morpheme;
    final String id;
    List<MorphemeTransition> outgoing = new ArrayList<>();
    List<MorphemeTransition> incoming = new ArrayList<>();

    public MorphemeState(String id, Morpheme morpheme) {
        this.morpheme = morpheme;
        this.id = id;
    }

    public MorphemeState addOutgoing(MorphemeTransition... transitions) {
        Collections.addAll(outgoing, transitions);
        return this;
    }

    public MorphemeState addIncoming(MorphemeTransition... transitions) {
        Collections.addAll(incoming, transitions);
        return this;
    }
}
