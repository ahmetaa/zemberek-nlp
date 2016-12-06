package zemberek.morphology.lexicon;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalyzer;


import java.util.ArrayList;
import java.util.List;

public class TurkishSuffixesTest {

    @Test
    public void testCase() {
        Tester tester = new Tester("elma","armut");
        tester.assertHasParses("elma", "armut", "elmada", "armutta");
    }

    @Test
    public void testVoicing() {
        Tester tester = new Tester("armut");
        tester.assertHasParses("armut", "armuda", "armutlar", "armutlara");
        tester.assertUnParseable("armud", "armuta", "armudlar", "armutlarlar", "armutalar");
    }

    @Test
    public void quantPron() {
        Tester tester = new Tester("biri [P:Pron, Quant]");
        tester.assertHasParses("birisi", "birileri");
    }

    @Test
    public void testVoicingOptional() {
        Tester tester = new Tester("yok [A:Voicing]","yok");
        tester.assertHasParses("yok", "yoka", "yoğa" ,"yoktan");
        tester.assertUnParseable("yoğta", "yokku");
    }

    @Test
    public void testNosuffix() {
        Tester tester = new Tester("göbek");
        tester.assertHasParses("göbek");
        tester.assertUnParseable("göbeğ");
    }

    // TODO:  "zeytinyağlılarım" gives two identical parse wordParseList.
    @Test
    public void testCompounds() {
        Tester tester = new Tester("zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]");
        tester.assertHasParses("zeytinyağcık", "zeytinyağım", "zeytinyağına", "zeytinyağlılar", "zeytinyağlılarım","zeytinyağı", "zeytinyağcığa", "zeytinyağlarım");
        tester.assertUnParseable("zeytinyağılar", "zeytinyağıcık");
    }

    @Test
    public void testCompoundsVoicing() {
        Tester tester = new Tester("atkuyruğu [A:CompoundP3sg; Roots:at-kuyruk]");
        tester.assertHasParses("atkuyrukçuk", "atkuyruğu", "atkuyruklarım");
        tester.assertUnParseable("atkuyruğlarım", "atkuyruk");
    }

    //TODO elmalidir should have single parse
    @Test
    public void testWithAndWithout() {
        Tester tester = new Tester("elma", "kitap","Ankara");
        tester.assertHasParses("elmalı", "elmasız", "kitaplı", "kitapsız", "elmalıydı", "elmalıdır","ankarasız");
        tester.assertUnParseable("elmayalı", "elmalarlı", "elmadasız", "elmalarsız");
    }

    @Test
    public void testBecome() {
        // noun
        Tester tester = new Tester("elma", "mavi [P:Adj]");
        tester.assertHasParses("elmalaş");
        // adj
        tester.assertHasParses("mavileş");
    }

    @Test
    public void questMi() {
        // noun
        Tester tester = new Tester("mi [P:Ques]","mu [P:Ques]");
        tester.assertHasParses("mi","misin","musun","miydik","miymişiz");
    }

    @Test
    public void testAcquire() {
        // noun
        Tester tester = new Tester("elma", "mavi [P:Adj]");
        tester.assertHasParses("elmalan", "elmalanıyor", "elmalandırmak");
        // adj
        tester.assertHasParses("mavilendirilebileceğinden");
    }

    @Test
    public void testNounToVerbCopular() {
        Tester tester = new Tester("elma");
        tester.assertHasParses("elmaydı", "elmaymış", "elmaydık", "elmayım", "elmadır");
    }


    @Test
    public void fitFor() {
        Tester tester = new Tester("elma");
        tester.assertHasParses("elmalık", "elmalığa");
    }

    @Test
    public void testAdj2Verb() {
        Tester tester = new Tester("mavi [P:Adj]");
        tester.assertHasParses("mavileşti", "mavileşmiş", "maviydi", "maviyim");
        tester.assertHasParses("mavilendi", "mavilendir");
    }


    @Test
    public void testAdj2Noun() {
        // noun
        Tester tester = new Tester("mavi [P:Adj]");
        tester.assertHasParses("maviye", "mavilerde");
    }

    @Test
    public void testQuiteAndLy() {
        // Adj-Adj and Adj-Adv
        Tester tester = new Tester("hızlı [P:Adj]");
        tester.assertHasParses("hızlıca");
    }

    @Test
    public void testRelated() {
        // Noun-Adj
        Tester tester = new Tester("elma");
        tester.assertHasParses("elmadaki", "elmadakini");
        tester.assertHasParses("elmanınki", "elmanınkinde", "elmanınkinden");
        tester.assertUnParseable("elmaki", "elmayaki", "elmadakiki");
        //TODO: add akşamki etc. uses Rel_kI instead of Rel_ki
    }

    @Test
    public void testA3pl() {
        Tester tester = new Tester("elma", "mavi [P:Adj]");
        tester.assertHasParses("elmalar", "elmalara", "elmalarda", "elmalardaki", "elmalardakilerdeki");
        tester.assertHasParses("elmalardı", "elmalaraydı", "elmalardır", "elmalardandır", "elmalarsa");
        tester.assertHasParses("maviler", "mavilere", "mavilerim", "mavilerdendir", "mavilerdeymiş");
    }

    @Test
    public void testDim() {
        // Noun-Noun
        Tester tester = new Tester("armut");
        tester.assertHasParses("armutçuk", "armutçuğu", "armutcağız", "armutcağızı");
        tester.assertUnParseable("armutçuğ", "armutçukcuk", "armutçukcağız", "armutcağızcık");
        // TODO: check oflazer parse "babacım = baba+Noun+A3sg+Pnon+Nom^DB+Noun+Dim+A3sg+P1sg+Nom
    }

    @Test
    public void testResembl() {
        Tester tester = new Tester("armut", "yeşil [P:Adj]");
        tester.assertHasParses("armutsu", "armudumsu", "yeşilsi", "yeşilimsi");
        // TODO: oflazer uses JustLike for this. It parses words like "tuhafsı","arabası" as JustLike
    }

    @Test
    public void testCausative() {
        Tester tester = new Tester("yapmak", "aramak");
        tester.assertHasParses("yaptırttır", "yaptır", "yaptırt", "arat", "arattır", "arattırt");
        tester.assertHasParses("yaptırarak", "yaptırtarak", "yaptırtmayacak");
        tester.assertUnParseable("yapt", "aratt");
    }



    @Test
    public void testPassive() {
        // Noun-Noun
        Tester tester = new Tester("yapmak", "aramak", "gelmek");
        tester.assertHasParses("aranıl", "yapıl", "aran", "gelin", "gelinil");
        tester.assertUnParseable("aral", "gelil", "kavurul", "kavurunul");
        // causative and passive
        tester.assertHasParses("yaptırıl", "yaptırtıl", "yaptırttırıl", "aratıl", "arattırıl", "arattırtıl");
    }

    @Test
    public void testNegative() {
        // Noun-Noun
        Tester tester = new Tester("aramak", "gelmek");
        tester.assertHasParses("arama", "gelme");
    }

    @Test
    public void testProgressive() {
        // Noun-Noun
        Tester tester = new Tester("aramak", "gelmek");
        tester.assertHasParses("arıyor", "aramıyor", "geliyor", "gelmiyor", "arıyordu", "arıyorlardı", "gelmiyorlarsa");
        tester.assertHasParses("arıyormuş", "aramıyorsak", "geliyormuşuz");
        tester.assertHasParses("aramakta", "aramamakta", "gelmekte", "gelmemekte");
    }

    @Test
    public void testAorist() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]", "gelmek [A:NonTransitive, Aorist_I]");
        tester.assertHasParses("arar", "ararsa", "gider", "gelir", "aramaz");
        tester.assertHasParses("ararlar", "ararlarsa", "ararsalar");
        tester.assertUnParseable("geler", "gidir");
    }

    @Test
    public void testPast() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("aradı", "gitti", "gittik", "gittiyse", "gittim", "gittiniz", "gittiydim");
        tester.assertUnParseable("gittiz", "gittimiş");
    }

    @Test
    public void testConditional() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("ararsa", "ararsam", "aradıysa", "aradıysam", "aradımsa", "aradıksa");
        tester.assertUnParseable("arardıysa");
    }

    @Test
    public void testImp() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]", "hapsetmek [A:Voicing]","buyurmak [A:LastVowelDrop]");
        tester.assertHasParses("ara", "arayın", "git", "gitme", "gidiniz", "gitsene", "gitsenize", "aramayacak", "aramasın", "gitmesin", "arasınlar", "gitmesinler","buyrun");
        tester.assertHasParses("aramasanıza", "aramayın", "gitmeyin");
        tester.assertUnParseable(suffixProvider.Pass, "hapsedin");
    }

    @Test
    public void testEvid() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("aramış", "gitmiş", "gitmişiz", "aramıştı");
    }

    @Test
    public void testVerb2Verb2() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("arayıver", "aramayıver", "gidiver", "gitmeyiver");
    }

    @Test
    public void testInf() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("aramak", "aramada", "arayışı", "gitmek", "gitmekten", "gitmektendi");
        tester.assertHasParses("aramakla", "aramaktı");
    }

    @Test
    public void testInf2() {
        Tester tester = new Tester("aramak", "çalışmak");
        tester.assertHasParses("çalışmalarını","arayış");
    }

    @Test
    public void testFuture() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("arayacak", "arayacağız", "aramayacak", "aratacak", "arayacaklar", "arayacaktık");
        tester.assertHasParses("gidecek", "gideceğiz", "gitmeyecek", "gidecekler", "gidecektik");
    }


    @Test
    public void testNess() {
        Tester tester = new Tester("elma", "mavi [P:Adj]");
        tester.assertHasParses("elmacıktı", "elmalık", "elmalığı", "elmalıktı", "elmalığa", "mavilik", "maviliği", "mavilikti");
        tester.assertUnParseable("elmalıka", "maviliki");
    }

    @Test
    public void verbVowelDrop() {
        Tester tester = new Tester("kavurmak [A:LastVowelDrop]");
        tester.assertHasParses("kavurdu", "kavuracağız", "kavurur", "kavuruyor", "kavur", "kavrul", "kavrulma", "kavurma", "kavrulacağız", "kavrulmayıver", "kavurtacağız");
        tester.assertUnParseable("kavurul", "kavracağız", "kavruyor", "kavrar", "kavrar");
    }

    @Test
    public void nounVowelDrop() {
        Tester tester = new Tester("ağız [A:LastVowelDrop]");
        tester.assertHasParses("ağız", "ağzı", "ağızlar", "ağza");
        tester.assertUnParseable("ağızı", "ağzlar");
    }

    @Test
    public void optative() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("araya", "arayayım", "gide", "gidesin");
    }

    @Test
    public void WhileTest() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("ararken", "aramazken", "gidiyorken", "gidecekken");
        tester.assertHasParses("ararkenki");
    }

    @Test
    public void whenTest() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("arayınca", "arayıncaya");
    }

    @Test
    public void feelLike() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("arayası", "arayasım", "gidesi", "gitmeyesi");
    }

    @Test
    public void necessity() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("gitmeli", "gitmeliyim", "gitmemeli", "gitmemelisiniz", "gitmeliyiz");
        tester.assertHasParses("aramalı", "aramamalı", "aramalıyım", "aramamalısın", "aramamalısınız");
    }

    @Test
    public void PronP2pl() {
        Tester tester = new Tester("siz [P:Pron, Pers ; RootSuffix:PersPron_Siz]");
        tester.assertHasParses("siz", "sizler", "sizlere");
    }

    @Test
    public void PronP1pl() {
        Tester tester = new Tester("biz [P:Pron, Pers ; RootSuffix:PersPron_Biz]");
        tester.assertHasParses("biz", "bizler", "bizlere");
    }

    @Test
    public void possession() {
        Tester tester = new Tester("elma", "mavi [P:Adj]");
        tester.assertHasParses("elmam", "elman", "elması", "elmamız", "elmaları");
        tester.assertHasParses("elmamı", "elmanda", "elmasında", "elmamızdan", "elmalarıyla");
        tester.assertHasParses("mavim", "mavin", "mavisi", "mavimiz", "mavileri");
        tester.assertHasParses("mavimi", "mavinde", "mavisinden", "mavimizde", "mavileriyle");
        tester.assertHasParses("elmamdı", "elmammış", "elmamızsa", "elmamızdır");
        tester.assertHasParses("mavimdi", "mavinmiş", "mavisiyse", "mavimizdir");
    }


    @Test
    public void ability() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        // abil
        tester.assertHasParses("arayabil", "arayabilir", "arayabilecek", "gidebil", "gidebilecek", "gidebiliyor");
        // a
        tester.assertHasParses("arayamıyorum", "arayamaya", "arayamasın", "arayamamaktan", "gidemese");
        tester.assertHasParses("arayamam", "arayamazsın", "arayamayız", "arayama", "arayamayabilir", "gideme", "gidemeyebilir");
    }

    @Test
    public void asif() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("ararcasına", "aramazcasına", "arıyormuşçasına", "aranılmışçasına", "gidebilirmişsinizcesine");
    }

    @Test
    public void pastPart() {
        Tester tester = new Tester("yaşamak");
        tester.assertHasParses("yaşadıklarını");
    }


    @Test
    public void Verb2Adverb() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        // WithoutDoing_mAdAn
        tester.assertHasParses("aramadan", "arayamadan", "gitmeden", "gidemeden");
        // When_yIncA, , UnableToDo_yAmAdAn, ByDoing_yArAk, WithoutDoing_mAdAn, WithoutDoing2_mAksIzIn)
        //                .add(InsteadOfDoing_mAktAnsA, AsLongAs_dIkcA, AfterDoing_yIp
        //SinceDoing_yAlI
        tester.assertHasParses("arayalı", "aramayalı", "gideli", "gitmeyeli");
        //ByDoing_yArAk
        tester.assertHasParses("arayarak", "aramayarak", "giderek", "gitmeyerek");
        //WithoutDoing_mAdAn.
        tester.assertHasParses("aramadan", "gitmeden", "aranmadan", "aratmadan");
        //WithoutDoing2_mAksIzIn
        tester.assertHasParses("aramaksızın", "gitmeksizin", "aranmaksızın");
        //InsteadOfDoing_mAktAnsA
        tester.assertHasParses("aramaktansa", "aramamaktansa", "aranmaktansa", "aratmaktansa", "aranmamaktansa");
        //AsLongAs_dIkcA
        tester.assertHasParses("aradıkça", "aramadıkça", "aranmadıkça", "aratmadıkça", "arayamadıkça");
        //AfterDoing_yIp
        tester.assertHasParses("arayıp", "aramayıp");
    }

    @Test
    public void PresPart_yAn() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        //
        tester.assertHasParses("gidene", "arayandan", "gitmeyenden", "aramayanı", "aratmayanı", "aranmayanı");
    }

    @Test
    public void FutPart_yAcAK() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        // WithoutDoing_mAdAn
        tester.assertHasParses("gidecekleri", "gitmeyeceklere", "arayacaklardan", "aramayacaklardan", "arayamayacaklardan");
    }

    @Test
    public void Des() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("arasa", "arasak", "arasalar", "aramasalar", "aransalar", "aratsalar", "arattırsak", "arattırmasanız");
    }

    @Test
    public void Rel_Sal() {
        Tester tester = new Tester("elma");
        tester.assertHasParses("elmasal");
    }

    @Test
    public void Agt() {
        Tester tester = new Tester("aramak", "gitmek [A:Voicing, Aorist_A]");
        tester.assertHasParses("arayıcı", "gidici");
    }

    @Test
    public void AgtNoun() {
        Tester tester = new Tester("elma", "armut");
        tester.assertHasParses("elmacı", "armutçu", "elmacılar", "elmacısın", "elmacısınız", "elmacıyız", "elmacılık");
    }


    @Test
    public void stemChange() {
        Tester tester = new Tester("demek [A:Special]", "yemek [A:Special]");
        tester.assertHasParses("ye", "de", "yeme", "deme", "yiyor", "diyor", "yemiyor", "yiyebilecek", "demiyor", "yedi", "dedi");
        tester.assertUnParseable("yi", "di", "yime", "dime", "yeyor", "deyor", "yemeyor");
    }


    static TurkishSuffixes suffixProvider = new TurkishSuffixes();

    class Tester {

        DynamicLexiconGraph graph;

        Tester(String... words) {
            synchronized (this) {
                List<DictionaryItem> items = getItems(words, suffixProvider);
                graph = new DynamicLexiconGraph(suffixProvider);
                graph.addDictionaryItems(items);
            }
        }

        void assertHasParses(String... words) {
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

        void assertUnParseable(String... words) {
            WordAnalyzer parser = new WordAnalyzer(graph);
            for (String word : words) {
                List<WordAnalysis> results = parser.analyze(word);
                Assert.assertTrue("Unexpected parse for:" + word + " parse:" + results, results.size() == 0);
            }
        }

        void assertUnParseable(Suffix suffix, String... words) {
            WordAnalyzer parser = new WordAnalyzer(graph);
            for (String word : words) {
                List<WordAnalysis> results = parser.analyze(word);
                for (WordAnalysis result : results) {
                    Assert.assertFalse(word + " parse should not contain suffix:" + suffix.id + " parse:" + results,
                            result.containsSuffix(suffix));
                }
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
    }

    @Test
    public void testFormCount() {
        int t = suffixProvider.getFormCount();
        new Tester("elma", "ekma", "armut");
        Assert.assertEquals(t, suffixProvider.getFormCount());
    }

    @Test
    public void testFormCount2() {
        System.out.println("======== adding roots =========");
        int t = suffixProvider.getFormCount();
        new Tester("gitmek");
        System.out.println("======== gitmek added =========");
        int k = suffixProvider.getFormCount();
        Assert.assertFalse(t == k);
        new Tester("yitmek");
        System.out.println("======== yitmek added =========");
        int j = suffixProvider.getFormCount();
        Assert.assertEquals(k, j);
    }

    @Test
    public void testForms() {
        for (SuffixForm form : suffixProvider.getAllForms()) {
            Assert.assertTrue(!(form instanceof SuffixFormTemplate));
        }
    }

}
