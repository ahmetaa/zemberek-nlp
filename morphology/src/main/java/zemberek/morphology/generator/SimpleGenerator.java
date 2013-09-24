package zemberek.morphology.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.NullSuffixForm;
import zemberek.morphology.lexicon.Suffix;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleGenerator {

    DynamicLexiconGraph graph;
    ArrayListMultimap<DictionaryItem, StemNode> multiStems = ArrayListMultimap.create(1000, 2);
    Map<DictionaryItem, StemNode> singeStems = Maps.newHashMap();

    public SimpleGenerator(DynamicLexiconGraph graph) {
        this.graph = graph;
        for (StemNode stemNode : graph.getStemNodes()) {
            final DictionaryItem item = stemNode.getDictionaryItem();
            if (multiStems.containsKey(item)) {
                multiStems.put(item, stemNode);
            } else if (singeStems.containsKey(item)) {
                multiStems.put(item, singeStems.get(item));
                singeStems.remove(item);
                multiStems.put(item, stemNode);
            } else
                singeStems.put(item, stemNode);
        }
    }

    public String[] generateMorphemes(DictionaryItem item, List<Suffix> suffixes) {
        List<GenerationToken> tokens = getTokens(item, suffixes);
        if (tokens.size() == 0)
            return new String[0];
        else return tokens.get(0).getAsMorphemes();
    }

    /**
     * generates a word from a dictionary item and a list of Suffixes. There can be multiple results for this operation.
     * Because a suffix can have multiple forms.
     *
     * @param item     dictionary item
     * @param suffixes suffixes.
     * @return zero or more words generated from input.
     */
    public String[] generate(DictionaryItem item, List<Suffix> suffixes) {
        List<GenerationToken> tokens = getTokens(item, suffixes);
        if (tokens.size() == 0)
            return new String[0];
        else {
            String[] res = new String[tokens.size()];
            int i = 0;
            for (GenerationToken token : tokens) {
                res[i++] = token.getAsString();
            }
            return res;
        }
    }

    private List<GenerationToken> getTokens(DictionaryItem item, List<Suffix> suffixes) {
        // find nodes for the dictionary item.
        List<StemNode> nodeList = new ArrayList<>();
        if (singeStems.containsKey(item))
            nodeList.add(singeStems.get(item));
        else if (multiStems.containsKey(item))
            nodeList.addAll(multiStems.get(item));

        // generate starting tokens with suffix root nodes.
        List<GenerationToken> initialTokens = new ArrayList<>(2);
        for (StemNode candidate : nodeList) {
            initialTokens.add(new GenerationToken(candidate, suffixes));
        }

        // traverse suffix graph.
        List<GenerationToken> result = new ArrayList<>(2);
        traverseSuffixes(initialTokens, result);
        return result;
    }

    private void traverseSuffixes(List<GenerationToken> current, List<GenerationToken> completed) {
        List<GenerationToken> newtokens = Lists.newArrayList();
        for (GenerationToken token : current) {
            if (token.nodesLeft.size() == 0) {
                if (token.terminal)
                    completed.add(token);
                continue;
            }
            for (SuffixSurfaceNode successor : token.currentSurfaceNode.getSuccessors()) {
                if (successor.getSuffixForm().getSuffix() == token.getSuffix()) {
                    newtokens.add(token.getCopy(successor));
                } else if(successor.getSuffixForm() instanceof NullSuffixForm ) {
                    newtokens.add(token.getForNull(successor));
                }
            }
        }
        if (newtokens.size() > 0)
            traverseSuffixes(newtokens, completed);
    }
}
