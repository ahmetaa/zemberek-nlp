package zemberek.morphology.lexicon;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.TurkicSeq;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.graph.SuffixData;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;


import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static zemberek.core.turkish.PhoneticAttribute.*;

public class SuffixSurfaceNodeGeneratorTest {

    static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

    @Test
    public void suffixFormAHarmonyTest() {
        SuffixSurfaceNodeGenerator sfg = new SuffixSurfaceNodeGenerator();
        SuffixSurfaceNode surfaceNode = getFirstNodeNoExpectatios(sfg, set(LastVowelBack), "lAr");
        Assert.assertEquals("lar", surfaceNode.surfaceForm);
        Assert.assertTrue(surfaceNode.getAttributes().containsAll(Arrays.asList(LastLetterConsonant, LastVowelBack, LastVowelUnrounded)));

        surfaceNode = getFirstNodeNoExpectatios(sfg, set(LastVowelBack, LastVowelRounded), "lAr");
        Assert.assertEquals("lar", surfaceNode.surfaceForm);

        surfaceNode = getFirstNodeNoExpectatios(sfg, set(LastVowelFrontal, LastVowelRounded), "lAr");
        Assert.assertEquals("ler", surfaceNode.surfaceForm);
    }

    @Test
    public void suffixFormIHarmonyTest() {
        SuffixSurfaceNodeGenerator sfg = new SuffixSurfaceNodeGenerator();
        SuffixSurfaceNode form = getFirstNodeNoExpectatios(sfg, set(LastVowelBack, LastVowelUnrounded), "sIn");
        Assert.assertEquals("sın", form.surfaceForm);
        Assert.assertTrue(form.getAttributes().containsAll(Arrays.asList(LastLetterConsonant, LastVowelBack, LastVowelUnrounded)));

        form = getFirstNodeNoExpectatios(sfg, set(LastVowelBack, LastVowelRounded), "sInIz");
        Assert.assertEquals("sunuz", form.surfaceForm);
    }

    @Test
    public void emptyFormTest() {
        SuffixSurfaceNodeGenerator sfg = new SuffixSurfaceNodeGenerator();
        SuffixSurfaceNode surfaceNode = getFirstNodeNoExpectatios(sfg, set(LastVowelBack, LastVowelRounded, LastLetterConsonant), "");
        Assert.assertEquals("", surfaceNode.surfaceForm);
        Assert.assertTrue(surfaceNode.getAttributes().containsAll(Arrays.asList(LastVowelBack, LastVowelRounded, LastLetterConsonant)));
    }

    @Test
    public void novowelFormTest() {
        SuffixSurfaceNodeGenerator sfg = new SuffixSurfaceNodeGenerator();
        SuffixSurfaceNode surfaceNode = getFirstNodeNoExpectatios(sfg, set(LastVowelBack, LastVowelRounded, LastLetterVowel), "m");
        Assert.assertEquals("m", surfaceNode.surfaceForm);
        Assert.assertTrue(surfaceNode.getAttributes().containsAll(Arrays.asList(LastVowelBack, LastVowelRounded, LastLetterConsonant)));
    }

    @Test
    public void lastDevoicingTest() {
        SuffixSurfaceNodeGenerator sfg = new SuffixSurfaceNodeGenerator();
        List<SuffixSurfaceNode> surfaceNodes = getNodes(sfg, set(LastVowelBack, LastVowelUnrounded, LastLetterConsonant), ">cI~k");
        Assert.assertEquals(2, surfaceNodes.size());
        Assert.assertEquals("cık", surfaceNodes.get(0).surfaceForm);
        Assert.assertEquals("cığ", surfaceNodes.get(1).surfaceForm);
    }

    @Test
    public void surfaceFormFunctionalTest() {
        Triple[] triples = {
                new Triple("kalem", "lAr", "ler"),
                new Triple("kalem", "lArA", "lere"),
                new Triple("kan", "lAr", "lar"),
                new Triple("kan", "lArAt", "larat"),
                new Triple("kan", "Ar", "ar"),
                new Triple("kaba", "lAr", "lar"),
                new Triple("kaba", "Ar", "r"),
                new Triple("kedi", "lAr", "ler"),
                new Triple("kedi", "lArA", "lere"),
                new Triple("kart", "lAr", "lar"),
                new Triple("a", "lAr", "lar"),
                new Triple("ee", "lAr", "ler"),

                new Triple("kalem", "lIk", "lik"),
                new Triple("kedi", "lIk", "lik"),
                new Triple("kabak", "lIk", "lık"),
                new Triple("kuzu", "lIk", "luk"),
                new Triple("göz", "lIk", "lük"),
                new Triple("gö", "lIk", "lük"),
                new Triple("ö", "lIk", "lük"),

                new Triple("kalem", "lArI", "leri"),
                new Triple("arı", "lArI", "ları"),
                new Triple("odun", "lArI", "ları"),
                new Triple("odun", "lIrA", "lura"),

                new Triple("kale", "+yA", "ye"),
                new Triple("kale", "+nA", "ne"),
                new Triple("kalem", "+yA", "e"),
                new Triple("kale", "+yI", "yi"),
                new Triple("kalem", "+yI", "i"),
                new Triple("kale", "+yIr", "yir"),
                new Triple("kale", "+yAr", "yer"),

                new Triple("kale", "+In", "n"),
                new Triple("kale", "+An", "n"),
                new Triple("kalem", "InA", "ine"),
                new Triple("kale", "InI", "ni"),

                new Triple("kitap", ">cA", "ça"),
                new Triple("sarraf", ">cA", "ça"),
                new Triple("makas", ">cA", "ça"),
                new Triple("tokat", ">cA", "ça"),
                new Triple("kaş", ">cA", "ça"),
                new Triple("fok", ">cA", "ça"),
                new Triple("gitar", ">cA", "ca"),
                new Triple("kalem", ">cA", "ce"),
                new Triple("kale", ">cA", "ce"),
                new Triple("kitap", ">dAn", "tan"),
                new Triple("gitar", ">dIn", "dın"),
                new Triple("kalem", ">dA", "de"),
                new Triple("kale", ">dArI", "deri"),

                new Triple("kale", "+y>cI", "yci"),
                new Triple("kitap", "+y>cI", "çı")

        };
        SuffixSurfaceNodeGenerator sfg = new SuffixSurfaceNodeGenerator();
        for (Triple triple : triples) {
            SuffixSurfaceNode form = getFirstNodeNoExpectatios(sfg,
                    sfg.defineMorphemicAttributes(new TurkicSeq(triple.predecessor, alphabet)),
                    triple.generationWord);
            Assert.assertEquals("Error in:" + triple, triple.expectedSurface, form.surfaceForm);
        }
    }


    private EnumSet<PhoneticAttribute> set(PhoneticAttribute... attributes) {
        return EnumSet.copyOf(Arrays.asList(attributes));
    }

    private SuffixSurfaceNode getFirstNodeNoExpectatios(SuffixSurfaceNodeGenerator sfg, EnumSet<PhoneticAttribute> attributes, String generation) {
        SuffixForm dummySet = new SuffixForm(1, "dummy-form", new Suffix("dummy"), generation);
        return sfg.generate(attributes, EnumSet.noneOf(PhoneticExpectation.class), new SuffixData(), dummySet).get(0);
    }

    private List<SuffixSurfaceNode> getNodes(SuffixSurfaceNodeGenerator sfg, EnumSet<PhoneticAttribute> attributes, String generation) {
        SuffixForm dummySet = new SuffixForm(2, "dummy-form", new Suffix("dummy"), generation);
        return sfg.generate(attributes, EnumSet.noneOf(PhoneticExpectation.class), new SuffixData(), dummySet);
    }

    private class Triple {
        String predecessor;
        String generationWord;
        String expectedSurface;

        private Triple(String predecessor, String generationWord, String expectedSurface) {
            this.predecessor = predecessor;
            this.generationWord = generationWord;
            this.expectedSurface = expectedSurface;
        }

        @Override
        public String toString() {
            return "Triple{" +
                    "predecessor='" + predecessor + '\'' +
                    ", generationWord='" + generationWord + '\'' +
                    ", expectedSurface=" + (expectedSurface == null ? null : Arrays.asList(expectedSurface)) +
                    '}';
        }
    }

}
