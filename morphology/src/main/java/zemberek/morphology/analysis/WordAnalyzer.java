package zemberek.morphology.analysis;

import com.google.common.collect.Lists;
import zemberek.core.collections.IntValueMap;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;
import zemberek.morphology.lexicon.graph.TerminationType;

import java.util.ArrayList;
import java.util.List;

public class WordAnalyzer {

    public final DynamicLexiconGraph graph;

    public WordAnalyzer(DynamicLexiconGraph graph) {
        this.graph = graph;
    }

    public List<WordAnalysis> analyze(String input) {
        // get stem candidates.
        List<StemNode> candidates = Lists.newArrayListWithCapacity(3);
        for (int i = 1; i <= input.length(); i++) {
            String stem = input.substring(0, i);
            candidates.addAll(graph.getMatchingStemNodes(stem));
        }

        // generate starting tokens with suffix root nodes.
        List<Token> initialTokens = Lists.newArrayListWithCapacity(5);
        for (StemNode candidate : candidates) {
            String rest = input.substring(candidate.surfaceForm.length());
            initialTokens.add(new Token(candidate, Lists.newArrayList(candidate.getSuffixRootSurfaceNode()), rest));
        }

        // traverse suffix graph.
        List<WordAnalysis> result = Lists.newArrayListWithCapacity(3);

        traverseSuffixes(initialTokens, result);
        return result;
    }

    private void traverseSuffixes(List<Token> current, List<WordAnalysis> completed) {

        if (current.size() > 50) {
            current = pruneCyclicPaths(current);
        }

        List<Token> newTokens = Lists.newArrayList();
        for (Token token : current) {
            boolean matchFound = false;
            for (SuffixSurfaceNode successor : token.currentSurfaceNode.getSuccessors()) {
                if (token.rest.startsWith(successor.surfaceForm)) {
                    if (token.rest.length() > 0) {
                        newTokens.add(token.getCopy(successor));
                        matchFound = true;
                    } else {
                        if (successor.termination != TerminationType.NON_TERMINAL) {
                            newTokens.add(token.getCopy(successor));
                            matchFound = true;
                        }
                    }
                } else {
//                    System.out.println("No match:" +  successor.getSuffixForm().getId() );
                }
            }
            if (!matchFound) {
                if (token.rest.length() == 0 && token.terminal) {
                    completed.add(token.getResult());
                }
            }
        }
        if (!newTokens.isEmpty()) {
            traverseSuffixes(newTokens, completed);
        }
    }

    public void dump(String input) {
        // get stem candidates.
        System.out.println("  Input:" + input);
        List<StemNode> candidates = Lists.newArrayList();
        for (int i = 1; i <= input.length(); i++) {
            String stem = input.substring(0, i);
            candidates.addAll(graph.getMatchingStemNodes(stem));
        }
        System.out.println("  Stem Nodes:");
        for (StemNode candidate : candidates) {
            System.out.println(candidate);
        }

        System.out.println("  Traverse Paths:");
        // generate starting tokens with suffix root nodes.
        List<Token> initialTokens = Lists.newArrayList();
        for (StemNode candidate : candidates) {
            String rest = input.substring(candidate.surfaceForm.length());
            initialTokens.add(new Token(candidate, Lists.newArrayList(candidate.getSuffixRootSurfaceNode()), rest));
        }

        // traverse suffix graph.
        List<WordAnalysis> result = Lists.newArrayList();
        dumpTraverse(initialTokens, result);

    }

    private void dumpTraverse(List<Token> current, List<WordAnalysis> completed) {
        if (current.size() > 50) {
            current = pruneCyclicPaths(current);
        }
        List<Token> newtokens = Lists.newArrayList();
        for (Token token : current) {
            boolean matchFound = false;
            for (SuffixSurfaceNode successor : token.currentSurfaceNode.getSuccessors()) {
                if (token.rest.startsWith(successor.surfaceForm)) {
                    System.out.println(successor.getSuffixForm().getId());
                    final Token copy = token.getCopy(successor);
                    if (token.rest.length() > 0) {
                        newtokens.add(copy);
                        matchFound = true;
                    } else {
                        if (successor.termination != TerminationType.NON_TERMINAL) {
                            newtokens.add(copy);
                            matchFound = true;
                        }
                    }
                }
            }
            if (!matchFound) {
                if (token.rest.length() == 0 && token.terminal)
                    completed.add(token.getResult());
                else {
                    System.out.println("Failed:" + token.getResult());
                }
            }
        }
        if (!newtokens.isEmpty())
            dumpTraverse(newtokens, completed);
    }

    private static final int MAX_REPEATING_SUFFIX_TYPE_COUNT = 3;

    // for preventing excessive branching during search, we remove paths that has more than 3 repeating suffix forms.
    private List<Token> pruneCyclicPaths(List<Token> tokens) {
        List<Token> result = new ArrayList<>();
        for (Token token : tokens) {
            boolean remove = false;
            IntValueMap<String> typeCounts = new IntValueMap<>(10);
            for (SuffixSurfaceNode node : token.surfaceNodeHistory) {
                if (typeCounts.addOrIncrement(node.getSuffixForm().id) > MAX_REPEATING_SUFFIX_TYPE_COUNT) {
                    remove = true;
                    break;
                }
            }
            if (!remove) {
                result.add(token);
            }
        }
        return result;
    }

    static class Token {
        StemNode stemNode;
        SuffixSurfaceNode currentSurfaceNode;
        List<SuffixSurfaceNode> surfaceNodeHistory;
        String rest;
        boolean terminal = false;

        public Token(StemNode stemNode, List<SuffixSurfaceNode> surfaceNodeHistory, String rest) {
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

        Token(StemNode stemNode,
              SuffixSurfaceNode suffixSurfaceNode,
              List<SuffixSurfaceNode> surfaceNodeHistory,
              String rest,
              boolean terminal) {
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

        public WordAnalysis getResult() {
            return new WordAnalysis(stemNode, surfaceNodeHistory);
        }

        Token getCopy(SuffixSurfaceNode surfaceNode) {
            boolean t = terminal;
            switch (surfaceNode.termination) {
                case TERMINAL:
                    t = true;
                    break;
                case NON_TERMINAL:
                    t = false;
                    break;
            }
            ArrayList<SuffixSurfaceNode> hist = new ArrayList<>(surfaceNodeHistory);
            hist.add(surfaceNode);
            return new Token(stemNode, surfaceNode, hist, rest.substring(surfaceNode.surfaceForm.length()), t);
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
}
