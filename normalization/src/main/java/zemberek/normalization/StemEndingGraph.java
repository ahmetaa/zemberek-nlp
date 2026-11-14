package zemberek.normalization;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import zemberek.core.collections.Histogram;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.StemTransitions;
import zemberek.morphology.morphotactics.StemTransition;

/**
 * This is a data structure that can be used for spell checking purposes. This is a graph consist of
 * two trie data structures. One for stems, other for endings. Stem leaf nodes are connected to the
 * ending graph root.
 */
public class StemEndingGraph {

  final CharacterGraph stemGraph;
  private final CharacterGraph endingGraph;
  private final TurkishMorphology morphology;

  public StemEndingGraph(TurkishMorphology morphology) throws IOException {
    this(morphology, TextIO.loadLinesFromResource("endings"));
  }

  StemEndingGraph(TurkishMorphology morphology, List<String> endings) {
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
      for (SingleAnalysis analysis : morphology.analyze(word)) {
        if (analysis.isUnknown()) {
          continue;
        }
        String ending = analysis.getEnding();
        if (!ending.isEmpty()) {
          endings.add(ending);
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
    CharacterGraph graph = new CharacterGraph();
    StemTransitions stemTransitions = morphology.getMorphotactics().getStemTransitions();
    for (StemTransition transition : stemTransitions.getTransitions()) {
      if (transition.surface.isEmpty()
          || transition.item.primaryPos == PrimaryPos.Punctuation
          || transition.item.isAbbreviation()) {
        continue;
      }
      graph.addWord(transition.surface, Node.TYPE_WORD);
    }
    return graph;
  }
}
