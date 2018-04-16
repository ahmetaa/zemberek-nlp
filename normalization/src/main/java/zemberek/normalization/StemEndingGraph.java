package zemberek.normalization;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import zemberek.core.collections.Histogram;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.StemTransitions;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.core.turkish.StemAndEnding;
import zemberek.core.turkish.Turkish;

/**
 * This is a data structure that can be used for spell checking purposes. This is a graph consist of
 * two trie data structures. One for stems, other for endings. Stem leaf nodes are connected to the
 * ending graph root.
 */
public class StemEndingGraph {

  CharacterGraph stemGraph;
  private CharacterGraph endingGraph;

  private TurkishMorphology morphology;

  public StemEndingGraph(TurkishMorphology morphology) throws IOException {
    this.morphology = morphology;
    List<String> endings =
        Resources.readLines(
            Resources.getResource("endings"), StandardCharsets.UTF_8);
    this.endingGraph = generateEndingGraph(endings);
    this.stemGraph = generateStemGraph();
    Set<Node> stemWordNodes = stemGraph.getAllNodes(n -> n.word != null);
    for (Node node : stemWordNodes) {
      node.connectEpsilon(endingGraph.getRoot());
    }
  }

  StemEndingGraph(TurkishMorphology morphology, List<String> endings) throws IOException {
    this.morphology = morphology;
    this.endingGraph = generateEndingGraph(endings);
    this.stemGraph = generateStemGraph();
    Set<Node> leafNodes = stemGraph.getAllNodes(n -> n.word != null);
    for (Node leafNode : leafNodes) {
      leafNode.connectEpsilon(endingGraph.getRoot());
    }
  }

  List<String> getEndingsFromVocabulary(List<String> words) {
    Histogram<String> endings = new Histogram<>(words.size() / 10);
    for (String word : words) {
      WordAnalysis analyses = morphology.analyze(word);
      for (SingleAnalysis analysis : analyses) {
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
      graph.addWord(ending, Node.TYPE_ENDING);
    }
    return graph;
  }

  private CharacterGraph generateStemGraph() {
    CharacterGraph stemGraph = new CharacterGraph();
    StemTransitions stemTransitions = morphology.getMorphotactics().getStemTransitions();
    for (StemTransition transition : stemTransitions.getTransitions()) {
      if (transition.surface.length() == 0 ||
          transition.item.primaryPos == PrimaryPos.Punctuation) {
        continue;
      }
      stemGraph.addWord(transition.surface, Node.TYPE_WORD);
    }
    return stemGraph;
  }
}
