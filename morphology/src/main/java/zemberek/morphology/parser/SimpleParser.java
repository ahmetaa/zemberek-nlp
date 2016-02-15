package zemberek.morphology.parser;

import com.google.common.collect.Lists;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;
import zemberek.morphology.lexicon.graph.TerminationType;

import java.util.List;

public class SimpleParser implements MorphParser {

    DynamicLexiconGraph graph;

    public SimpleParser(DynamicLexiconGraph graph) {
        this.graph = graph;
    }

    public List<MorphParse> parse(String input) {
        // get stem candidates.
        List<StemNode> candidates = Lists.newArrayListWithCapacity(3);
        for (int i = 1; i <= input.length(); i++) {
            String stem = input.substring(0, i);
            candidates.addAll(graph.getMatchingStemNodes(stem));
        }

        // generate starting tokens with suffix root nodes.
        List<ParseToken> initialTokens = Lists.newArrayListWithCapacity(3);
        for (StemNode candidate : candidates) {
            String rest = input.substring(candidate.surfaceForm.length());
            initialTokens.add(new ParseToken(candidate, Lists.newArrayList(candidate.getSuffixRootSurfaceNode()), rest));
        }

        // traverse suffix graph.
        List<MorphParse> result = Lists.newArrayList();
        traverseSuffixes(initialTokens, result);
        return result;
    }

    private void traverseSuffixes(List<ParseToken> current, List<MorphParse> completed) {
        List<ParseToken> newtokens = Lists.newArrayList();
        for (ParseToken token : current) {
            boolean matchFound = false;
            for (SuffixSurfaceNode successor : token.currentSurfaceNode.getSuccessors()) {
                if (token.rest.startsWith(successor.surfaceForm)) {
                    if (token.rest.length() > 0) {
                        newtokens.add(token.getCopy(successor));
                        matchFound = true;
                    } else {
                        if (successor.termination != TerminationType.NON_TERMINAL) {
                            newtokens.add(token.getCopy(successor));
                            matchFound = true;
                        }
                    }
                } else {
//                    System.out.println("No match:" +  successor.getSuffixForm().getId() );
                }
            }
            if (!matchFound) {
                if (token.rest.length() == 0 && token.terminal)
                    completed.add(token.getResult());
            }
        }
        if (!newtokens.isEmpty())
            traverseSuffixes(newtokens, completed);
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
        List<ParseToken> initialTokens = Lists.newArrayList();
        for (StemNode candidate : candidates) {
            String rest = input.substring(candidate.surfaceForm.length());
            initialTokens.add(new ParseToken(candidate, Lists.newArrayList(candidate.getSuffixRootSurfaceNode()), rest));
        }

        // traverse suffix graph.
        List<MorphParse> result = Lists.newArrayList();
        dumpTraverse(initialTokens, result);

    }

    private void dumpTraverse(List<ParseToken> current, List<MorphParse> completed) {
        List<ParseToken> newtokens = Lists.newArrayList();
        for (ParseToken token : current) {
            boolean matchFound = false;
            for (SuffixSurfaceNode successor : token.currentSurfaceNode.getSuccessors()) {
                if (token.rest.startsWith(successor.surfaceForm)) {
                    System.out.println(successor.getSuffixForm().getId());
                    final ParseToken copy = token.getCopy(successor);
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
}
