package zemberek.morphology.lexicon.graph;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StemTrieTest {

    private static Random r = new Random(0xCAFEDEADBEEFL);
    private static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    private StemTrie lt;

    @Before
    public void setUp() {
        lt = new StemTrie();
    }

	private StemNode createStemNode(String surfaceForm) {
		DictionaryItem di = new DictionaryItem(surfaceForm, surfaceForm, PrimaryPos.Noun, null, null, null,null);
		return new StemNode(surfaceForm, di, TerminationType.TERMINAL);
	}

    private void addStemNodes(List<StemNode> nodes){
        for (StemNode node : nodes) {
            lt.add(node);
        }
    }

    private List<StemNode> createNodes(String... stems) {
        List<StemNode> nodes = Lists.newArrayList();
        for (String s : stems) {
            StemNode node = createStemNode(s);
            nodes.add(node);
        }
        return nodes;
    }

    private void checkNodesExist(List<StemNode> nodes) {
        for (StemNode node : nodes) {
            List<StemNode> stems = lt.getMatchingStems(node.surfaceForm);
            assertTrue(" Should have contained: " + node, stems.contains(node));
        }
    }

    private void checkNodesMatches(String prefix, List<StemNode> nodes) {
        List<StemNode> stems = lt.getMatchingStems(prefix);
        for (StemNode node : nodes) {
            assertTrue("Should have contained: " + node, stems.contains(node));
        }
    }
	
	@Test
	public void empty(){
		List<StemNode> stems = lt.getMatchingStems("foo");
		assertEquals(stems.size(), 0);
	}
	
	@Test
	public void singleItem() {
        List<StemNode> nodes = createNodes("elma");
        addStemNodes(nodes);
        checkNodesExist(nodes);
	}
	
	@Test
	public void distinctStems() {
        List<StemNode> nodes = createNodes("elma", "armut");
        addStemNodes(nodes);
        checkNodesExist(nodes);
	}
	
	@Test
	public void stemsSharingSamePrefixOrder1() {
        List<StemNode> nodes = createNodes("elmas", "elma");
        addStemNodes(nodes);
        checkNodesExist(nodes);
        checkNodesMatches("elma", createNodes("elma"));
        checkNodesMatches("elmas", createNodes("elma", "elmas"));
	}	
	
	@Test
	public void stemsSharingSamePrefixOrder2() {
        List<StemNode> nodes = createNodes("elma", "elmas");
        addStemNodes(nodes);
        checkNodesExist(nodes);
        checkNodesMatches("elma", createNodes("elma"));
        checkNodesMatches("elmas", createNodes("elma", "elmas"));
	}

	@Test
	public void stemsSharingSamePrefix3Stems() {
        List<StemNode> nodes = createNodes("el", "elmas", "elma");
        addStemNodes(nodes);
        checkNodesExist(nodes);
        checkNodesMatches("elma", createNodes("el", "elma"));
        checkNodesMatches("el", createNodes("el"));
        checkNodesMatches("elmas", createNodes("el", "elma", "elmas"));
        checkNodesMatches("elmaslar", createNodes("el", "elma", "elmas"));
    }

    @Test
    public void stemsForLongerInputs() {
        List<StemNode> nodes = createNodes("el", "elmas", "elma", "ela");
        addStemNodes(nodes);
        checkNodesExist(nodes);
        checkNodesMatches("e", createNodes());
        checkNodesMatches("el", createNodes("el"));
        checkNodesMatches("elif", createNodes("el"));
        checkNodesMatches("ela", createNodes("el", "ela"));
        checkNodesMatches("elastik", createNodes("el", "ela"));
        checkNodesMatches("elmas", createNodes("el", "elma", "elmas"));
        checkNodesMatches("elmaslar", createNodes("el", "elma", "elmas"));
    }

    @Test
    public void removeStems() {
        List<StemNode> nodes = createNodes("el", "elmas", "elma", "ela");
        addStemNodes(nodes);
        checkNodesExist(nodes);
        checkNodesMatches("el", createNodes("el"));
        // Remove el
        checkNodesMatches("el", createNodes());
        // Remove elmas
        lt.remove(nodes.get(1));
        checkNodesMatches("elmas", createNodes());
        checkNodesMatches("e", createNodes());
        checkNodesMatches("ela", createNodes( "ela"));
        checkNodesMatches("elastik", createNodes( "ela"));
        checkNodesMatches("elmas", createNodes("el", "elma"));
        checkNodesMatches("elmaslar", createNodes("el", "elma"));
    }

    @Test
    public void stemsSharingPartialPrefix1() {
        List<StemNode> nodes = createNodes("fix", "foobar", "foxes");
        addStemNodes(nodes);
        checkNodesExist(nodes);
    }

    private List<String> generateRandomWords(int number){
        List<String> randomWords = Lists.newArrayList();
        for (int i=0; i<number; i++) {
            int len = r.nextInt(20) + 1;
            char[] chars = new char[len];
            for (int j = 0; j < len ; j++) {
                chars[j] = alphabet.getLetter(r.nextInt(29) + 1).charValue();
            }
            randomWords.add(new String(chars));
        }
        return randomWords;
    }

    @Test
    public void testBigNumberOfBigWords() {
        List<String> words = generateRandomWords(1000);
        List<StemNode> nodes = Lists.newArrayList();
        for (String s : words) {
            StemNode n =  createStemNode(s);
            lt.add(n);
            nodes.add(n);
        }
        for (StemNode node : nodes) {
            List<StemNode> res = lt.getMatchingStems(node.surfaceForm);
            assertTrue(res.contains(node));
            assertTrue(res.get(res.size() - 1).surfaceForm.equals(node.surfaceForm));
            for (StemNode n : res) {
                // Check if all stems are a prefix of last one on the tree. 
                assertTrue(res.get(res.size() - 1).surfaceForm.startsWith(n.surfaceForm));
            }
        }
    }
	
}