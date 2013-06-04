package zemberek.morphology.parser;

import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;
import zemberek.morphology.lexicon.graph.TerminationType;

import java.util.ArrayList;
import java.util.List;

class ParseToken {
    StemNode stemNode;
    SuffixSurfaceNode currentSurfaceNode;
    List<SuffixSurfaceNode> surfaceNodeHistory;
    String rest;
    boolean terminal = false;

    public ParseToken(StemNode stemNode, List<SuffixSurfaceNode> surfaceNodeHistory, String rest) {
        this.stemNode = stemNode;
        this.currentSurfaceNode = stemNode.getSuffixRootSurfaceNode();
        this.surfaceNodeHistory = surfaceNodeHistory;
        this.rest = rest;
        this.terminal = stemNode.termination == TerminationType.TERMINAL;
    }

    public DictionaryItem getDictionaryItem() {
        return stemNode.getDictionaryItem();
    }

    public String getLemma() {
        return getDictionaryItem().lemma;
    }

    ParseToken(StemNode stemNode, SuffixSurfaceNode suffixSurfaceNode, List<SuffixSurfaceNode> surfaceNodeHistory, String rest, boolean terminal) {
        this.stemNode = stemNode;
        this.currentSurfaceNode = stemNode.getSuffixRootSurfaceNode();
        this.surfaceNodeHistory = surfaceNodeHistory;
        this.rest = rest;
        this.terminal = terminal;
        this.currentSurfaceNode = suffixSurfaceNode;
    }

    public String getRest() {
        return rest;
    }

    public SuffixSurfaceNode getCurrentSurfaceNode() {
        return currentSurfaceNode;
    }

    public MorphParse getResult() {
        return new MorphParse(stemNode, surfaceNodeHistory);
    }

    ParseToken getCopy(SuffixSurfaceNode surfaceNode) {
        boolean t = terminal;
        switch (surfaceNode.termination) {
            case TERMINAL:
                t = true;
                break;
            case NON_TERMINAL:
                t = false;
                break;
        }
        ArrayList<SuffixSurfaceNode> hist = new ArrayList<SuffixSurfaceNode>(surfaceNodeHistory);
        hist.add(surfaceNode);
        return new ParseToken(stemNode, surfaceNode, hist, rest.substring(surfaceNode.surfaceForm.length()), t);
    }

    @Override
    public String toString() {
        return stemNode.surfaceForm + ":" + surfaceNodeHistory;
    }

    public String asParseString() {
        StringBuilder sb = new StringBuilder("[" + stemNode.surfaceForm + ":" + getLemma() + "-" + getDictionaryItem().primaryPos + "]");
        sb.append("[");
        int i = 0;
        for (SuffixSurfaceNode suffixSurfaceNode : surfaceNodeHistory) {
            sb.append(suffixSurfaceNode.getSuffixForm().getSuffix()).append(":").append(suffixSurfaceNode.surfaceForm);
            if (i++ < surfaceNodeHistory.size() - 1)
                sb.append(" + ");
        }
        sb.append("]");
        return sb.toString();
    }
}
