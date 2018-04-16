package zemberek.morphology.old_lexicon.graph;

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.morphology.old_lexicon.Suffix;
import zemberek.morphology.old_lexicon.SuffixForm;
import zemberek.morphology.old_lexicon.graph.SuffixData;
import zemberek.morphology.old_lexicon.graph.SuffixSurfaceNode;
import zemberek.morphology.old_lexicon.graph.TerminationType;

public class SuffixSurfaceNodeTest {

  @Test
  public void equalityTest() {
    SuffixForm dummySet = new SuffixForm(1, "dummy-set", new Suffix("dummy-suffix"), "lar");
    SuffixSurfaceNode surfaceNode1 = new SuffixSurfaceNode(
        dummySet,
        "ler",
        EnumSet.of(PhoneticAttribute.LastVowelBack),
        TerminationType.TERMINAL
    );

    SuffixSurfaceNode surfaceNode2 = new SuffixSurfaceNode(
        dummySet,
        "ler",
        EnumSet.of(PhoneticAttribute.LastVowelBack),
        TerminationType.TERMINAL
    );

    Assert.assertEquals(surfaceNode1, surfaceNode2);

    SuffixSurfaceNode surfaceNode3 = new SuffixSurfaceNode(
        dummySet,
        "ler",
        EnumSet.of(PhoneticAttribute.LastVowelBack),
        EnumSet.of(PhoneticExpectation.ConsonantStart),
        new SuffixData(),
        TerminationType.TERMINAL
    );

    Assert.assertNotSame(surfaceNode1, surfaceNode3);
    Assert.assertNotSame(surfaceNode2, surfaceNode3);

    Set<SuffixSurfaceNode> surfaceNodeSet = Sets
        .newHashSet(surfaceNode1, surfaceNode2, surfaceNode3);
    Assert.assertEquals(2, surfaceNodeSet.size());

  }
}
