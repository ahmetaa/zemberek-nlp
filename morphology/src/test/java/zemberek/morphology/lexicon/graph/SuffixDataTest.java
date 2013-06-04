package zemberek.morphology.lexicon.graph;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.lexicon.Suffix;
import zemberek.morphology.lexicon.SuffixForm;

public class SuffixDataTest {

    @Test
    public void equalityTest() {
        Suffix suffix = new Suffix("SUFFIX");
        SuffixForm sf1 = new SuffixForm(1, "sf1", suffix, "lAr", TerminationType.TERMINAL);
        SuffixForm sf2 = new SuffixForm(1, "sf1", suffix, "lAr", TerminationType.TERMINAL);
        Assert.assertTrue(sf1.equals(sf2));
        Assert.assertTrue(sf2.equals(sf1));

        SuffixForm sf3 = new SuffixForm(3, "sf3", suffix, "k", TerminationType.TERMINAL);
        Assert.assertFalse(sf1.equals(sf3));

        sf1.connections.add(sf3);
        Assert.assertFalse(sf1.equals(sf2));

        sf2.connections.add(sf3);
        Assert.assertTrue(sf1.equals(sf2));

        sf1.connections.remove(sf3);
        SuffixForm sf4 = new SuffixForm(4, "sf4", suffix, "lAr", TerminationType.NON_TERMINAL);
        Assert.assertFalse(sf4.equals(sf2));
    }


    @Test
    public void removeTest() {
        Suffix suffix = new Suffix("SUFFIX");
        SuffixForm sf1 = new SuffixForm(1, "sf1", suffix, "lAr");
        SuffixForm sf2 = new SuffixForm(2, "sf2", suffix, "lerr");
        sf1.connections = new SuffixData(sf1, sf2);
        sf1.connections.remove(sf1);
        Assert.assertEquals(1, sf1.connections.size());
    }
}
