package zemberek.normalization;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;

public class CharacterGraphNodeTest {

  @Test
  public void testLeafSortedChildrenAndWordPreservation() {
    CharacterGraph graph = new CharacterGraph();
    String[] words = {"ev", "evim", "elma", "erik", "et", "ek", "eş", "eğri", "ezgi", "edep"};
    for (String w : words) {
      graph.addWord(w, Node.TYPE_WORD);
    }

    Node eNode = graph.getRoot().getImmediateChild('e');
    Assert.assertNotNull(eNode);
    Assert.assertTrue(eNode.hasImmediateChild('v'));
    Assert.assertFalse(eNode.hasImmediateChild('a'));
    Assert.assertEquals(9, eNode.getImmediateChildNodes().size());
    Assert.assertEquals("[e children=[d, k, l, r, t, v, z, ğ, ş]]", eNode.toString());

    Node vNode = eNode.getImmediateChild('v');
    Assert.assertEquals("ev", vNode.word);
    Assert.assertEquals(Node.TYPE_WORD, vNode.getType());
    Assert.assertEquals(1, vNode.getImmediateChildNodes().size());

    Node leaf = vNode.getImmediateChild('i').getImmediateChild('m');
    Assert.assertEquals("evim", leaf.word);
    Assert.assertTrue(leaf.getImmediateChildNodes().isEmpty());
    Assert.assertNull(leaf.getImmediateChild('a'));
  }

  @Test
  public void testEpsilonConnectionsAndSpellCheckerParity() throws Exception {
    TurkishMorphology morphology =
        TurkishMorphology.builder().setLexicon("gelmek", "kitap", "ev").build();
    StemEndingGraph graph = new StemEndingGraph(morphology);
    TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology, graph.stemGraph);
    List<String> suggestions = spellChecker.suggestForWord("kitp");
    Assert.assertTrue(suggestions.contains("kitap"));
    Assert.assertTrue(spellChecker.suggestForWord("glmek").contains("gelmek"));
  }
}
