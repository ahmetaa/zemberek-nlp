package zemberek.morphology.lexicon;

import zemberek.morphology.lexicon.graph.TerminationType;

public class DerivationalSuffixTemplate extends SuffixFormTemplate {

    public DerivationalSuffixTemplate(int index, String id, Suffix suffix, TerminationType type) {
        super(index, id, suffix, type);
    }

    public DerivationalSuffixTemplate(int index, String id, Suffix suffix) {
        super(index, id, suffix);
    }
}
