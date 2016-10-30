package zemberek.morphology.generator;

import com.google.common.base.Joiner;
import zemberek.morphology.lexicon.Suffix;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;

import java.util.ArrayList;
import java.util.List;

public class GenerationToken {
    StemNode stemNode;
    SuffixSurfaceNode currentSurfaceNode;
    List<Suffix> nodesLeft;
    List<String> formList = new ArrayList<>();
    boolean terminal;

    public GenerationToken(StemNode stemNode, List<Suffix> nodesLeft) {
        this.stemNode = stemNode;
        this.currentSurfaceNode = stemNode.getSuffixRootSurfaceNode();
        this.nodesLeft = nodesLeft;
        this.formList.add(stemNode.surfaceForm);
        terminal = stemNode.isTerminal();
    }

    public Suffix getSuffix() {
        return nodesLeft.get(0);
    }

    public GenerationToken(StemNode stemNode,
                           SuffixSurfaceNode currentSurfaceNode,
                           List<Suffix> nodesLeft,
                           List<String> formList,
                           boolean terminal) {
        this.stemNode = stemNode;
        this.currentSurfaceNode = currentSurfaceNode;
        this.nodesLeft = nodesLeft;
        this.formList = formList;
        this.terminal = terminal;
    }

    public String getAsString() {
        return Joiner.on("").join(formList);
    }

    public String[] getAsMorphemes() {
        return formList.toArray(new String[formList.size()]);
    }

    GenerationToken getCopy(SuffixSurfaceNode surfaceNode) {
        boolean t = terminal;
        switch (surfaceNode.termination) {
            case TERMINAL:
                t = true;
                break;
            case NON_TERMINAL:
                t = false;
                break;
        }
        List<Suffix> hist = nodesLeft.subList(1, nodesLeft.size());
        List<String> formList = new ArrayList<>(this.formList);
        formList.add(surfaceNode.surfaceForm);
        return new GenerationToken(stemNode, surfaceNode, hist, formList, t);
    }

    GenerationToken getForNull(SuffixSurfaceNode surfaceNode) {
        boolean t = terminal;
        switch (surfaceNode.termination) {
            case TERMINAL:
                t = true;
                break;
            case NON_TERMINAL:
                t = false;
                break;
        }
        return new GenerationToken(stemNode, surfaceNode, new ArrayList<>(nodesLeft), formList, t);
    }

}
