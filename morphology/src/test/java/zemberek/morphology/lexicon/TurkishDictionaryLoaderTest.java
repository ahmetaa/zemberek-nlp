package zemberek.morphology.lexicon;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.io.Strings;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicSeq;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;


import java.io.File;
import java.io.IOException;
import java.util.*;

import static zemberek.core.turkish.PrimaryPos.*;
import static zemberek.core.turkish.RootAttribute.*;

public class TurkishDictionaryLoaderTest {

    TurkishSuffixes suffixProvider = new TurkishSuffixes();

    @Test
    public void loadNounsFromFileTest() throws IOException {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        RootLexicon items = loader.load(new File(Resources.getResource("test-lexicon-nouns.txt").getFile()));

        Assert.assertFalse(items.isEmpty());
        for (DictionaryItem item : items) {
            Assert.assertTrue(item.primaryPos == Noun);
        }
    }

    @Test
    public void nounInferenceTest() {
        DictionaryItem item = getItem("elma");
        Assert.assertEquals("elma", item.lemma);
        Assert.assertEquals("elma", item.root);
        Assert.assertEquals(Noun, item.primaryPos);

        item = getItem("elma [P:Noun]");
        Assert.assertEquals("elma", item.lemma);
        Assert.assertEquals("elma", item.root);
        Assert.assertEquals(Noun, item.primaryPos);
    }


    public DictionaryItem getItem(String itemStr) {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        return loader.loadFromString(itemStr);
    }


    public DictionaryItem getLastItem(String... itemStr) {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        String last = Strings.subStringUntilFirst(itemStr[itemStr.length - 1], " ");
        return loader.load(itemStr).getMatchingItems(last).get(0);
    }

    @Test
    public void verbInferenceTest() {
        DictionaryItem item = getItem("gelmek");
        Assert.assertEquals("gel", item.root);
        Assert.assertEquals("gelmek", item.lemma);
        Assert.assertEquals(Verb, item.primaryPos);

        String[] verbs = {"germek", "yarmak", "salmak", "yermek [P:Verb]", "etmek [P:Verb; A:Voicing]", "etmek [A:Voicing]",
                "yıkanmak [A:Reflexive]", "küfretmek [A:Voicing, Aorist_A]"};
        for (String verb : verbs) {
            item = getItem(verb);
            Assert.assertEquals(Verb, item.primaryPos);
        }
    }

    @Test
    public void compoundTest() {
        DictionaryItem item = getLastItem("kuyruk", "atkuyruğu [A:CompoundP3sg; Roots:at-kuyruk]");
        Assert.assertEquals("atkuyruğu", item.lemma);
        Assert.assertEquals(Noun, item.primaryPos);
    }

    @Test
    public void voicingInferenceTest() {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        DictionaryItem item = loader.loadFromString("aort [A:NoVoicing]");
        Assert.assertEquals("aort", item.root);
        Assert.assertEquals(Noun, item.primaryPos);
        Assert.assertTrue(item.hasAttribute(RootAttribute.NoVoicing));
        Assert.assertFalse(item.hasAttribute(RootAttribute.Voicing));

        item = loader.loadFromString("at");
        Assert.assertEquals("at", item.root);
        Assert.assertEquals(Noun, item.primaryPos);
        Assert.assertTrue(item.hasAttribute(RootAttribute.NoVoicing));
        Assert.assertFalse(item.hasAttribute(RootAttribute.Voicing));
    }

    @Test
    public void specialSuffixRoot() {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        DictionaryItem item = loader.loadFromString("su [P:Noun ; RootSuffix:Noun_Su_Root ]");
        Assert.assertEquals("su", item.root);
        Assert.assertEquals(Noun, item.primaryPos);
        Assert.assertNotNull(item.specialRootSuffix);
        Assert.assertEquals(suffixProvider.getSuffixFormById("Noun_Su_Root"), item.specialRootSuffix);
    }

    @Test
    public void suffixDataTest() {
        DictionaryItem item = getItem("ben [P:Pron; S: +A1sg_TEMPLATE]");
        Assert.assertEquals(Pronoun, item.primaryPos);
        Assert.assertNotNull(item.suffixData);
        Assert.assertTrue(!item.suffixData.accepts.isEmpty());
        Assert.assertTrue(item.suffixData.rejects.isEmpty());
        Assert.assertTrue(item.suffixData.onlyAccepts.isEmpty());

        item = getItem("ben [P:Pron; S: -A1sg_m, -A1sg_+yIm, +A1sg_TEMPLATE, +Dim_>cI~k]");
        Assert.assertTrue(item.suffixData.rejects.contains(suffixProvider.A1sg_m));
        Assert.assertTrue(item.suffixData.rejects.contains(suffixProvider.A1sg_yIm));
        Assert.assertTrue(item.suffixData.accepts.contains(suffixProvider.A1sg_TEMPLATE));
        Assert.assertTrue(item.suffixData.accepts.contains(suffixProvider.Dim_cIk));
    }


    @Test
    public void nounVoicingTest() {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);
        String[] voicing = {"kabak", "kabak [A:Voicing]", "psikolog", "havuç", "turp [A:Voicing]", "galip", "nohut", "cenk", "kükürt"};
        for (String s : voicing) {
            DictionaryItem item = loader.loadFromString(s);
            Assert.assertEquals(Noun, item.primaryPos);
            Assert.assertEquals("error in:" + s, EnumSet.of(RootAttribute.Voicing), item.attrs);
        }

        String[] novoicing = {"kek", "top", "kulp", "takat [A:NoVoicing]"};
        for (String s : novoicing) {
            DictionaryItem item = loader.loadFromString(s);
            Assert.assertEquals(Noun, item.primaryPos);
            Assert.assertEquals("error in:" + s, EnumSet.of(NoVoicing), item.attrs);
        }
    }

    @Test
    public void nounAttributesTest() {
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(suffixProvider);

        List<ItemAttrPair> testList = Lists.newArrayList(
                testPair("takat [A:NoVoicing, InverseHarmony]", NoVoicing, InverseHarmony),
                testPair("nakit [A: LastVowelDrop]", Voicing, LastVowelDrop),
                testPair("ret [A:Voicing, Doubling]", Voicing, Doubling)
        );
        for (ItemAttrPair pair : testList) {
            DictionaryItem item = loader.loadFromString(pair.str);
            Assert.assertEquals(Noun, item.primaryPos);
            Assert.assertEquals("error in:" + pair.str, pair.attrs, item.attrs);
        }
    }

    @Test
    @Ignore("Not a unit Test. Only loads the master dictionary.")
    public void masterDictionaryLoadTest() throws IOException {
        SuffixProvider sp = new TurkishSuffixes();
        TurkishDictionaryLoader loader = new TurkishDictionaryLoader(sp);

        RootLexicon items = loader.load(new File(Resources.getResource("tr/master-dictionary.dict").getFile()));
        TurkishAlphabet alphabet = new TurkishAlphabet();
        Set<String> masterVoicing = new HashSet<String>();
        for (DictionaryItem item : items) {
            if (item.attrs.contains(NoVoicing))
                masterVoicing.add(item.lemma);
        }

        Locale tr = new Locale("tr");
        List<String> allZ2 = SimpleTextReader.trimmingUTF8Reader(
                new File(Resources.getResource("tr/master-dictionary.dict").getFile())).asStringList();
        for (String s : allZ2) {
            if (s.startsWith("#"))
                continue;
            String clean = Strings.subStringUntilFirst(s.trim(), " ").toLowerCase(tr).replaceAll("[\\-']", "");
            if (s.contains("Adj") && !s.contains("Compound") && !s.contains("PropNoun")) {
                TurkicSeq seq = new TurkicSeq(clean, alphabet);
                if (seq.vowelCount() > 1 && seq.lastLetter().isStopConsonant() && !s.contains("Vo") && !s.contains("VowDrop")) {
                    if (!masterVoicing.contains(clean)) {
                        File f = new File("/home/afsina/data/tdk/html", clean + ".html");
                        if (!f.exists())
                            f = new File("/home/afsina/data/tdk/html", clean.replaceAll("â", "a").replaceAll("\\u00ee", "i") + ".html");
                        if (!f.exists()) {
                            System.out.println("Cannot find:" + s);
                            continue;
                        }
                        char c = clean.charAt(clean.length() - 1);
                        char vv = c;
                        switch (c) {
                            case 'k':
                                vv = 'ğ';
                                break;
                            case 'p':
                                vv = 'b';
                                break;
                            case 'ç':
                                vv = 'c';
                                break;
                            case 't':
                                vv = 'd';
                                break;
                            default:
                                System.out.println("crap:" + s);
                        }
                        String content = SimpleTextReader.trimmingUTF8Reader(f).asString();
                        if (!content.contains("color=DarkBlue>-" + String.valueOf(vv)))
                            System.out.println(s);
                    }
                }
            }
        }


        for (DictionaryItem item : items) {
            if ((item.primaryPos == Noun || item.primaryPos == PrimaryPos.Adjective) && item.secondaryPos != SecondaryPos.ProperNoun &&
                    item.hasAttribute(RootAttribute.Voicing)) {

            }
        }

        System.out.println(items.size());

    }

    @Test
    @Ignore("Not a unit Test. Converts word histogram to word list")
    public void prepareWordListFromHistogram() throws IOException {
        List<String> hist = SimpleTextReader.trimmingUTF8Reader(new File("test/data/all-turkish-noproper.txt.tr")).asStringList();
        List<String> all = Lists.newArrayList();
        for (String s : hist) {
            all.add(Strings.subStringUntilFirst(s, " ").trim());
        }
        SimpleTextWriter.oneShotUTF8Writer(new File(Resources.getResource("z2-vocab.tr").getFile())).writeLines(all);
    }

    @Test
    @Ignore("Not a unit test")
    public void shouldPrintItemsInDevlDictionary() throws IOException {
        RootLexicon items = new TurkishDictionaryLoader(new TurkishSuffixes()).load(new File(Resources.getResource("dev-dictionary.txt").getFile()));
        for (DictionaryItem item : items) {
            System.out.println(item);
        }
    }

    private static ItemAttrPair testPair(String s, RootAttribute... attrs) {
        return new ItemAttrPair(s, EnumSet.copyOf(Arrays.asList(attrs)));
    }

    private static class ItemAttrPair {
        String str;
        EnumSet<RootAttribute> attrs;

        private ItemAttrPair(String str, EnumSet<RootAttribute> attrs) {
            this.str = str;
            this.attrs = attrs;
        }
    }


}
