package zemberek.normalization;

import com.google.common.io.Resources;
import zemberek.core.collections.Histogram;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.structure.StemAndEnding;
import zemberek.morphology.structure.Turkish;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * This builds a graph that can be used for spell checking purposes.
 * Graph actually consist of two trie data structures. One for stems, other for endings.
 * Stem leaf nodes are connected to both stem root and the ending graph root.
 * Ending roots are only connected to stem roots. Transitions to stem roots allows separating connected words.
 */
public class StemEndingGraphBuilder {

    CharacterGraph stemGraph;
    CharacterGraph endingGraph;

    private TurkishMorphology morphology;

    public StemEndingGraphBuilder(TurkishMorphology morphology) {
        this.morphology = morphology;
    }

    List<String> getEndingsFromVocabulary(List<String> words) {
        Histogram<String> endings = new Histogram<>(words.size() / 10);
        for (String word : words) {
            List<WordAnalysis> analyses = morphology.analyze(word);
            for (WordAnalysis analysis : analyses) {
                if (analysis.isUnknown()) {
                    continue;
                }
                StemAndEnding se = analysis.getStemAndEnding();
                if (se.ending.length() > 0) {
                    endings.add(se.ending);
                }
            }
        }
        return endings.getSortedList(Turkish.STRING_COMPARATOR_ASC);
    }

    private CharacterGraph generateEndingGraph(List<String> endings) {
        CharacterGraph graph = new CharacterGraph();
        for (String ending : endings) {
            graph.addWord(ending);
        }
        return graph;
    }

    private CharacterGraph generateStemGraph() {
        CharacterGraph stemGraph = new CharacterGraph();
        DynamicLexiconGraph lexiconGraph = morphology.getGraph();
        for (StemNode stemNode : lexiconGraph.getStemNodes()) {
            if (stemNode.surfaceForm.length() == 0) {
                continue;
            }
            stemGraph.addWord(stemNode.surfaceForm);
        }
        return stemGraph;
    }


    void build() throws IOException {
        List<String> endings = Files.readAllLines(
                new File(Resources.getResource("endings").getFile()).toPath());
        CharacterGraph endingGraph = generateEndingGraph(endings);
        CharacterGraph stems = generateStemGraph();
        Set<Node> leafNodes = stems.getAllNodes(n -> n.word != null);
        for (Node leafNode : leafNodes) {
            leafNode.connect(endingGraph.getRoot());
        }
    }


    public static void main(String[] args) throws IOException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        StemEndingGraphBuilder graphBuilder = new StemEndingGraphBuilder(morphology);
        graphBuilder.build();
    }


}
