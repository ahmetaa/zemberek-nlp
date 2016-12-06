package zemberek.morphology.analysis;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;

public class WordAnalysisTest {

    TurkishSuffixes suffixProvider = new TurkishSuffixes();

    @Test
    public void parseTest1() {
        String[] dictionary = {"simit","ev", "kitap", "mavi [P:Adj]", "yirmi [P:Num,Card]"};
        String[] testSet = {"evde", "kitabıma", "kitaplaşırız", "mavi", "yirmiye"};
        String[] stemSet = {"ev", "kitab", "kitap", "mavi", "yirmi"};
        String[] expected = {
                "[(ev:ev) (Noun;A3sg+Pnon+Loc:de)]",
                "[(kitap:kitab) (Noun;A3sg+P1sg:ım+Dat:a)]",
                "[(kitap:kitap) (Noun;A3sg+Pnon+Nom)(Verb;Become:laş+Pos+Aor:ır+A1pl:ız)]",
                "[(mavi:mavi) (Adj)]",
                "[(yirmi:yirmi) (Num,Card)(Noun;A3sg+Pnon+Dat:ye)]"
        };
        String[] expectedNoSurface = {
                "[(ev) (Noun;A3sg+Pnon+Loc)]",
                "[(kitap) (Noun;A3sg+P1sg+Dat)]",
                "[(kitap) (Noun;A3sg+Pnon+Nom)(Verb;Become+Pos+Aor+A1pl)]",
                "[(mavi) (Adj)]",
                "[(yirmi) (Num,Card)(Noun;A3sg+Pnon+Dat)]"

        };
        String[] expectedEmptyNoSurface = {
                "[(ev) (Noun;Loc)]",
                "[(kitap) (Noun;P1sg+Dat)]",
                "[(kitap) (Noun)(Verb;Become+Aor+A1pl)]",
                "[(mavi) (Adj)]",
                "[(yirmi) (Num,Card)(Noun;Dat)]"

        };
        String[] expectedOflazer = {
                "ev+Noun+A3sg+Pnon+Loc",
                "kitap+Noun+A3sg+P1sg+Dat",
                "kitap+Noun+A3sg+Pnon+Nom^DB+Verb+Become+Pos+Aor+A1pl",
                "mavi+Adj",
                "yirmi+Num+Card^DB+Noun+A3sg+Pnon+Dat"
        };
        WordAnalyzer parser = getParser(dictionary);
        int i = 0;
        for (String s : testSet) {
            List<WordAnalysis> results = parser.analyze(s);
            WordAnalysis res = results.get(0);
            Assert.assertEquals(stemSet[i], res.root);
            Assert.assertEquals(expected[i], res.formatLong());
            Assert.assertEquals(expectedNoSurface[i], res.formatNoSurface());
            Assert.assertEquals(expectedOflazer[i], res.formatOflazer());
            Assert.assertEquals(expectedEmptyNoSurface[i], res.formatNoEmpty());
            i++;
        }
    }

    @Test
    public void igSurfaceTest() {
        WordAnalyzer parser = getParser("kitap");
        String[] testSet = {"kitabıma", "kitaplaşırız"};
        String[] expected = {"ıma", "laşırız"};
        int i = 0;
        for (String s : testSet) {
            List<WordAnalysis> results = parser.analyze(s);
            WordAnalysis res = results.get(0);
            List<String> surfaces = Lists.newArrayList();
            for (WordAnalysis.InflectionalGroup ig : res.inflectionalGroups) {
                surfaces.add(ig.surfaceForm());
            }
            Assert.assertEquals(expected[i], Joiner.on("").join(surfaces));
            i++;
        }
    }

    @Test
    public void getLemmasTest() {
        WordAnalyzer parser = getParser("kitap", "aramak", "mavi [P:Adj]", "leh", "dekorasyon", "yapmak");

        String[] testSet = {"kitaplaşırız", "kitabımızsa", "kitaba", "aradım", "aratagörün", "arattırın", "mavide",
                "lehimeydi", "dekorasyonundaki", "yapacağı", "yapacağınaysa"};

        String[][] expected = {
                {"kitap", "kitaplaş"},
                {"kitap", "kitabımızsa"},
                {"kitap"},
                {"ara"},
                {"ara", "arat", "aratagör"},
                {"ara", "arat", "arattır"},
                {"mavi"},
                {"leh", "lehimeydi"},
                {"dekorasyon", "dekorasyonundaki"},
                {"yap", "yapacak"},
                {"yap", "yapacak","yapacağınaysa"}
        };

        int i = 0;
        for (String s : testSet) {
            List<WordAnalysis> results = parser.analyze(s);
            WordAnalysis res = results.get(0);
            List<String> expStems = Lists.newArrayList(expected[i]);
            MatcherAssert.assertThat(res.getLemmas(), equalTo(expStems));
            i++;
        }
    }

    @Test
    public void getStemsTest() {
        WordAnalyzer parser = getParser("kitap", "yapmak");

        String[] testSet = {"kitaplaşırız", "kitaba", "yapacağı"};

        String[][] expected = {
                {"kitap", "kitaplaş"},
                {"kitab"},
                {"yap", "yapacağ"}
        };

        int i = 0;
        for (String s : testSet) {
            List<WordAnalysis> results = parser.analyze(s);
            WordAnalysis res = results.get(0);
            List<String> expStems = Lists.newArrayList(expected[i]);
            MatcherAssert.assertThat(res.getStems(), equalTo(expStems));
            i++;
        }
    }


    private WordAnalyzer getParser(String... lines) {
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(new TurkishDictionaryLoader(suffixProvider).load(lines));
        return new WordAnalyzer(graph);
    }
}
