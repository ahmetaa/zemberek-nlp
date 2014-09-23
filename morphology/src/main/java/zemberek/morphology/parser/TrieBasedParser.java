package zemberek.morphology.parser;

import com.google.common.collect.Lists;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.StemTrie;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;

import java.util.List;

public class TrieBasedParser implements MorphParser {
    DynamicLexiconGraph graph;
    StemTrie lexicon = new StemTrie();

    public TrieBasedParser(DynamicLexiconGraph graph) {
        this.graph = graph;
        for (StemNode stemNode : graph.getStemNodes()) {
            lexicon.add(stemNode);
        }
    }

    public List<MorphParse> parse(String input) {
        // get stem candidates.
        List<StemNode> candidates = lexicon.getMatchingStems(input);
        // generate starting tokens with suffix root nodes.
        List<ParseToken> initialTokens = Lists.newArrayList();
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
                    matchFound = true;
                    newtokens.add(token.getCopy(successor));
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
}
