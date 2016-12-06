package zemberek.morphology.analysis;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.*;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.graph.DynamicSuffixProvider;
import zemberek.morphology.lexicon.graph.SuffixData;
import zemberek.morphology.lexicon.graph.TerminationType;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordAnalyzerTest {

    @Test
    public void testVoicing() {
        DynamicLexiconGraph graph = getLexiconGraph("armut");
        assertHasParses(graph, "armut", "armuda", "armutlar", "armutlara");
        assertUnParseable(graph, "armud", "armuta", "armudlar");
    }

    @Test
    public void testSuffixNonDeterminism() {
        DynamicLexiconGraph graph = getLexiconGraph("elma");
        assertHasParses(graph, "elmacığa", "elmacık");
        assertUnParseable(graph, "elmacığ", "elmacıka", "elmamcık", "elmayacık", "elmalarcık");
    }

    @Test
    public void testInverseHarmony() {
        DynamicLexiconGraph graph = getLexiconGraph("saat [A: NoVoicing, InverseHarmony]");
        assertHasParses(graph, "saate", "saat", "saatler", "saatlere");
        assertUnParseable(graph, "saata", "saatlar", "saada", "saade");
    }

    @Test
    public void testVowelDrop() {
        DynamicLexiconGraph graph = getLexiconGraph("ağız [A: LastVowelDrop]", "nakit [A:LastVowelDrop]", "vakit [A:LastVowelDrop, NoVoicing]");
        assertHasParses(graph, "vakitlere", "ağza", "ağız", "ağızlar", "nakit", "nakitlere", "nakde", "vakit", "vakte");
        assertUnParseable(graph, "ağz", "ağıza", "ağzlar", "nakd", "nakt", "nakite", "nakda", "vakide", "vakda", "vakite", "vakt");
    }

    @Test
    public void testDoubling() {
        DynamicLexiconGraph graph = getLexiconGraph("ret [A:Voicing, Doubling]");
        assertHasParses(graph, "ret", "retler", "redde");
        assertUnParseable(graph, "rede", "rete", "redler", "red");
    }

    @Test
    public void testCompounds() {
        DynamicLexiconGraph graph = getLexiconGraph("zeytinyağı [A:CompoundP3sg ;Roots:zeytin-yağ]");
        assertHasParses(graph, "zeytinyağım", "zeytinyağına", "zeytinyağı", "zeytinyağcık", "zeytinyağcığa", "zeytinyağlarım");
        assertUnParseable(graph, "zeytinyağıcık", "zeytinyağılar");
    }

    @Test
    public void testCompoundsVoicing() {
        DynamicLexiconGraph graph = getLexiconGraph("at [A:NoVoicing]","kuyruk","atkuyruğu [A:CompoundP3sg; Roots:at-kuyruk]");
        assertHasParses(graph, "atkuyruğu", "atkuyruklarım", "atkuyrukçuk");
        assertUnParseable(graph, "atkuyruğlarım", "atkuyruk");
    }

    @Test
    public void testMultiWordDictionary() {
        DynamicLexiconGraph graph = getLexiconGraph("armut", "elma", "kabak", "kek");
        assertHasParses(graph, "armuda", "armutlara", "elmacığa", "keke", "kekçiklere");
    }

    private DynamicLexiconGraph getLexiconGraph(String... words) {
        SuffixProvider suffixProvider = new NounSuffixes();
        List<DictionaryItem> items = getItems(words, suffixProvider);
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(items);
        return graph;
    }

    private void assertHasParses(DynamicLexiconGraph graph, String... words) {
        WordAnalyzer parser = new WordAnalyzer(graph);
        for (String word : words) {
            List<WordAnalysis> results = parser.analyze(word);
            if (results.size() == 0)
                parser.dump(word);
            Assert.assertTrue("No parse for:" + word, results.size() > 0);
            for (WordAnalysis result : results) {
                System.out.println(word + "= " + result.formatLong());
            }
        }
    }

    private void assertLongParses(DynamicLexiconGraph graph, String word, String... parses) {
        WordAnalyzer parser = new WordAnalyzer(graph);

        List<WordAnalysis> results = parser.analyze(word);
        Assert.assertTrue("Cannot parse:" + word, results.size() > 0);

        Set<String> parseStrins = new HashSet<String>();
        for (WordAnalysis result : results) {
            parseStrins.add(result.formatLong());
        }
        for (String parse : parses) {
            Assert.assertTrue("Cannot parse: parse for:" + word, parseStrins.contains(parse));
        }
    }

    private void assertUnParseable(DynamicLexiconGraph graph, String... words) {
        WordAnalyzer parser = new WordAnalyzer(graph);
        for (String word : words) {
            List<WordAnalysis> results = parser.analyze(word);
            Assert.assertTrue("Unexpected parse for:" + word + " parse:" + results, results.size() == 0);
        }
    }

    private List<DictionaryItem> getItems(String[] lines, SuffixProvider suffixProvider) {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        List<DictionaryItem> items = new ArrayList<DictionaryItem>();
        for (String line : lines) {
            items.add(loader.loadFromString(line));
        }
        return items;
    }

    static class NounSuffixes extends DynamicSuffixProvider {

        SuffixForm Dim_CIK = getForm(new Suffix("Dim"), ">cI~k");
        SuffixForm P1sg_Im = getForm(new Suffix("P1sg"), "Im");
        SuffixForm Dat_yA = getForm(new Suffix("Dat"), "+yA");
        SuffixForm Dat_nA = getForm(new Suffix("Dat"), "nA");
        SuffixFormTemplate Pnon_TEMPLATE = getTemplate("Pnon_TEMPLATE", new Suffix("Pnon"));
        SuffixFormTemplate Nom_TEMPLATE = getTemplate("Nom_TEMPLATE", new Suffix("Nom"));
        SuffixFormTemplate A3sg_TEMPLATE = getTemplate("A3sg_TEMPLATE", new Suffix("A3sg"));
        SuffixForm A3pl_lAr = getForm(new Suffix("A3pl"), "lAr");
        SuffixForm A3pl_Comp_lAr = getForm("A3pl_Comp_lAr", new Suffix("A3pl"), "lAr", TerminationType.NON_TERMINAL); //zeytinyağlarımız
        RootSuffix Noun_Root = new RootSuffix("Noun", PrimaryPos.Noun);
        SuffixFormTemplate Noun_TEMPLATE = getTemplate("Noun_TEMPLATE", Noun_Root);
        SuffixForm Noun_Default = getNull("Noun_Default", Noun_TEMPLATE);
        SuffixFormTemplate Noun_Deriv = getTemplate("Noun2Noun", Noun_Root, TerminationType.NON_TERMINAL);

        SuffixFormTemplate Noun_Comp_P3sg = getTemplate("Noun_Comp_P3sg", Noun_Root);
        SuffixFormTemplate Noun_Comp_P3sg_Root = getTemplate("Noun_Comp_P3sg_Root", Noun_Root);

        NounSuffixes() {

            Noun_TEMPLATE.connections.add(A3pl_lAr, A3pl_Comp_lAr, A3sg_TEMPLATE);
            Noun_TEMPLATE.indirectConnections.add(P1sg_Im, Pnon_TEMPLATE, Nom_TEMPLATE, Dat_yA, Dat_nA, Dim_CIK, Noun_Deriv);

            Noun_Default.connections.add(Noun_TEMPLATE.connections);
            Noun_Default.indirectConnections.add(Noun_TEMPLATE.indirectConnections).remove(Dat_nA);

            Noun_Deriv.connections.add(Dim_CIK);
            //Noun2Noun.indirectConnections.add(Noun_TEMPLATE.allConnections());

            A3sg_TEMPLATE.connections.add(Pnon_TEMPLATE, P1sg_Im);
            A3sg_TEMPLATE.indirectConnections.add(Nom_TEMPLATE, Dat_yA, Dat_nA, Noun_Deriv).add(Noun_Deriv.allConnections());

            A3pl_lAr.connections.add(P1sg_Im, Pnon_TEMPLATE);
            A3pl_lAr.indirectConnections.add(Nom_TEMPLATE, Dat_yA);

            P1sg_Im.connections.add(Nom_TEMPLATE, Dat_yA);

            Pnon_TEMPLATE.connections.add(Nom_TEMPLATE, Dat_nA, Dat_yA);
            Pnon_TEMPLATE.indirectConnections.add(Noun_Deriv).add(Noun_Deriv.allConnections());

            Nom_TEMPLATE.connections.add(Noun_Deriv);
            Nom_TEMPLATE.indirectConnections.add(Noun_Deriv.allConnections());

            Dim_CIK.connections.add(Noun_Default.connections);
            Dim_CIK.indirectConnections.add(Noun_Default.allConnections().remove(Dim_CIK));

            // P3sg compound suffixes. (full form. such as zeytinyağı-na)
            Noun_Comp_P3sg.connections.add(A3sg_TEMPLATE);
            Noun_Comp_P3sg.indirectConnections.add(P1sg_Im, Pnon_TEMPLATE, Nom_TEMPLATE, Dat_nA);

            A3pl_Comp_lAr.connections.add(A3pl_lAr.connections);
            A3pl_Comp_lAr.indirectConnections.add(A3pl_lAr.indirectConnections);

            // P3sg compound suffixes. (root form. such as zeytinyağ-lar-ı)
            Noun_Comp_P3sg_Root.connections.add(A3pl_Comp_lAr, A3sg_TEMPLATE); // A3pl_Comp_lAr is used, because zeytinyağ-lar is not allowed.
            Noun_Comp_P3sg_Root.indirectConnections.add(Pnon_TEMPLATE, Nom_TEMPLATE);

            registerForms(Noun_TEMPLATE, Noun_Deriv, A3sg_TEMPLATE, Pnon_TEMPLATE, Nom_TEMPLATE);
            registerForms(Noun_Default, A3pl_lAr, P1sg_Im, Dat_yA, Dat_nA, Dim_CIK,Noun_Comp_P3sg_Root,Noun_Comp_P3sg,A3pl_Comp_lAr);

        }


        public SuffixForm getRootSet(DictionaryItem item, SuffixData successorConstraint) {

            if (successorConstraint.isEmpty()) {
                switch (item.primaryPos) {
                    case Noun:
                        return Noun_Default;
                    default:
                        throw new UnsupportedOperationException("In this class only some noun morphemes exist.");
                }
            } else {
                switch (item.primaryPos) {
                    case Noun:
                        NullSuffixForm nullForm = generateNullFormFromTemplate(Noun_TEMPLATE, successorConstraint);
                        registerForm(nullForm.copy());
                        return nullForm;
                    default:
                        throw new UnsupportedOperationException("In this class only some noun morphemes exist.");

                }

            }
        }

        public SuffixData[] defineSuccessorSuffixes(DictionaryItem item) {

            SuffixData original = new SuffixData();
            SuffixData modified = new SuffixData();

            PrimaryPos primaryPos = item.primaryPos;

            switch (primaryPos) {
                case Noun:
                    getForNoun(item, original, modified);
                    break;
                default:
                    throw new UnsupportedOperationException("In this class only some noun morphemes exist.");
            }
            return new SuffixData[]{original, modified};
        }

        void getForNoun(DictionaryItem item, SuffixData original, SuffixData modified) {

            for (RootAttribute attribute : item.attributes) {
                switch (attribute) {
                    case CompoundP3sg:
                        original.add(Noun_Comp_P3sg.allConnections());
                        modified.add(Noun_Comp_P3sg_Root.allConnections());
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
