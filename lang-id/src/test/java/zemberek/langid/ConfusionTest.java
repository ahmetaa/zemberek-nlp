package zemberek.langid;


import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import zemberek.core.collections.Histogram;
import zemberek.core.io.Files;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.Strings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ConfusionTest {

    LanguageIdentifier identifier;

    public ConfusionTest(LanguageIdentifier identifier) {
        this.identifier = identifier;
    }

    public void testAll() throws IOException {

        int sliceLength = 1000;
        int maxSliceCount = 1000;
        List<TestSet> sets = allSets(maxSliceCount, sliceLength);
        Set<String> languages = identifier.getLanguages();
        for (String language : languages) {
            System.out.println(language);
            Stopwatch sw = Stopwatch.createStarted();
            int falsePositives = 0;
            int totalCount = 0;
            int correctlyFound = 0;
            int correctAmount = 0;
            for (TestSet set : sets) {
/*                if(!set.modelId.equals("tr"))
                    continue;*/
                totalCount += set.size();
                Histogram<String> result = new Histogram<>();
                for (String s : set.testPieces) {
/*
                    LanguageIdentifier.IdResult idResult = identifier.identifyFullConf(s);
                    result.add(idResult.id);
*/
                    String t = identifier.identify(s);
                    if (set.modelId.equals(language) && !t.equals(language)) {
                       /* if (identifier.containsLanguage(s, "tr", 100, -1))
                            System.out.println("Has tr slice!");
                        System.out.println(t + " " + s);*/
                    }
                    result.add(t);
                    //result.add(identifier.identifyWithSampling(s,sliceLength));
                    //result.add(identifier.identifyWithSampling(s, 4));
                }
                if (set.modelId.equals(language)) {
                    System.out.println("Lang test size:" + set.size());
                    correctlyFound = result.getCount(language);
                    correctAmount = set.size();
                    List<String> sorted = result.getSortedList();
                    for (String s : sorted) {
                        System.out.println(s + " : " + result.getCount(s));
                    }
                    continue;
                } else {
                    int fpcount = result.getCount(language);
                    if (fpcount > 0)
                        System.out.println(set.modelId + " " + fpcount);
                }
                falsePositives += result.getCount(language);
            }
            double elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
            System.out.println(String.format("Id per second: %.2f", (1000d * totalCount / elapsed)));
            System.out.println("False positive count: " + falsePositives);
            System.out.println("All: " + totalCount);
            System.out.println(String.format("Precision:%.2f ", (100d * correctlyFound / correctAmount)));
            System.out.println(String.format("Recall: %.2f", (100d * (totalCount - falsePositives) / totalCount)));
        }
    }

    public void testContains() throws IOException {

        int sliceLength = 1000;
        int maxSliceCount = 1000;
        List<TestSet> sets = allSets(maxSliceCount, sliceLength);
        Set<String> languages = identifier.getLanguages();
        for (String language : languages) {
            System.out.println(language);
            Stopwatch sw = Stopwatch.createStarted();
            int falsePositives = 0;
            int totalCount = 0;
            int correctlyFound = 0;
            int correctAmount = 0;
            for (TestSet set : sets) {
/*                if(!set.modelId.equals("tr"))
                    continue;*/
                totalCount += set.size();
                Histogram<String> result = new Histogram<>();
                for (String s : set.testPieces) {
/*
                    LanguageIdentifier.IdResult idResult = identifier.identifyFullConf(s);
                    result.add(idResult.id);
*/
                    //String t = identifier.identify(s, 100);
                    //String t = identifier.identify(s);
                    String t = "tr";

                    identifier.containsLanguage(s, "tr", 100, -1);

                    if (set.modelId.equals(language) && !t.equals(language)) {
                       /* if (identifier.containsLanguage(s, "tr", 100, -1))
                            System.out.println("Has tr slice!");
                        System.out.println(t + " " + s);*/
                    }
                    result.add(t);
                    //result.add(identifier.identifyWithSampling(s,sliceLength));
                    //result.add(identifier.identifyWithSampling(s, 4));
                }
                if (set.modelId.equals(language)) {
                    System.out.println("Lang test size:" + set.size());
                    correctlyFound = result.getCount(language);
                    correctAmount = set.size();
                    List<String> sorted = result.getSortedList();
                    for (String s : sorted) {
                        System.out.println(s + " : " + result.getCount(s));
                    }
                    continue;
                } else {
                    int fpcount = result.getCount(language);
                    if (fpcount > 0)
                        System.out.println(set.modelId + " " + fpcount);
                }
                falsePositives += result.getCount(language);
            }
            double elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
            System.out.println(String.format("Id per second: %.2f", (1000d * totalCount / elapsed)));
            System.out.println("False positive count: " + falsePositives);
            System.out.println("All: " + totalCount);
            System.out.println(String.format("Precision:%.2f ", (100d * correctlyFound / correctAmount)));
            System.out.println(String.format("Recall: %.2f", (100d * (totalCount - falsePositives) / totalCount)));
        }
    }

    public List<String> slice(String chunk, int sliceCount, int sliceSize) {
        int point;
        List<String> testStrings = new ArrayList<String>();
        for (int i = 0; i < sliceCount; i++) {
            point = i * sliceSize;
            if (point + sliceSize > chunk.length())
                break;
            String s = chunk.substring(point, point + sliceSize);
            testStrings.add(s);
        }
        return testStrings;
    }

    class TestSet {
        String modelId;
        List<String> testPieces;

        TestSet(String modelId, List<String> testPieces) {
            this.modelId = modelId;
            this.testPieces = testPieces;
        }

        int size() {
            return testPieces.size();
        }
    }

    List<TestSet> allSets(int maxSliceCount, int sliceLength) throws IOException {
        List<File> files = Files.crawlDirectory(new File("/home/kodlab/data/language-data/subtitle"));
        files.addAll(Files.crawlDirectory(new File("/home/kodlab/data/language-data/wiki")));
        Map<String, TestSet> testSets = Maps.newHashMap();
        for (File file : files) {
            if (file.getName().contains("test")) {
                System.out.println(file);
                String langStr = file.getName().substring(0, file.getName().indexOf("-"));
                String chunk = SimpleTextReader.trimmingUTF8Reader(file).asString();
                chunk = Strings.whiteSpacesToSingleSpace(chunk);
                List<String> test = slice(chunk, maxSliceCount, sliceLength);
                //System.out.println(langStr);
                if (testSets.containsKey(langStr)) {
                    testSets.get(langStr).testPieces.addAll(test);
                } else {
                    testSets.put(langStr, new TestSet(langStr, test));
                }
            }
        }
        for (TestSet testSet : testSets.values()) {
            if (testSet.testPieces.size() > maxSliceCount)
                testSet.testPieces = testSet.testPieces.subList(0, maxSliceCount);
        }
        return Lists.newArrayList(testSets.values());
    }

    public static void main(String[] args) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
//        LanguageIdentifier identifier = LanguageIdentifier.fromCompressedModelsDir(new File("/home/kodlab/data/language-data/models/compressed"));
        String[] langs = {"tr", "en"};
        //String[]  langs = {"tr", "ar", "az", "hy", "bg", "en", "el", "ka", "ku", "fa", "de","fr","nl","diq"};
        // String[] langs = Language.allLanguages();
        //LanguageIdentifier identifier = LanguageIdentifier.generateFromCounts(new File("/home/kodlab/data/language-data/models/counts3"),Language.allLanguages());
        LanguageIdentifier identifier = LanguageIdentifier.fromInternalModelGroup("tr_group");
        //LanguageIdentifier identifier = LanguageIdentifier.fromInternalModelGroup("tr_group", langs);
        //LanguageIdentifier identifier = LanguageIdentifier.generateFromCounts(Language.allLanguages());
        System.out.println("Model generation: " + sw.elapsed(TimeUnit.MILLISECONDS));
        ConfusionTest confusionTest = new ConfusionTest(identifier);
        //confusionTest.testAll();
        confusionTest.testContains();
        // confusionTest.testOher();
    }

}
