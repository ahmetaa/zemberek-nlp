package zemberek.morphology.lexicon.graph;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.lexicon.Suffix;
import zemberek.morphology.lexicon.SuffixForm;


public class SuffixFormSetTest {
    @Test
    public void equalityTest() {
        Suffix suffix = new Suffix("SUFFIX");
        SuffixForm sf1 = new SuffixForm(1,"sf1", suffix, "lAr", TerminationType.TERMINAL);
        SuffixForm sf2 = new SuffixForm(2,"sf2", suffix, "lAr", TerminationType.TERMINAL);
        SuffixForm sf3 = new SuffixForm(3,"sf3", suffix, "k", TerminationType.TERMINAL);
        sf1.index = 0;
        sf2.index = 1;
        sf3.index = 100;
        
        SuffixData data1 = new SuffixData(sf1, sf2);
        Assert.assertTrue(data1.contains(sf1));
        Assert.assertTrue(data1.contains(sf2));


    }

}
