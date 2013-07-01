package zemberek.spelling;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.DoubleValueSet;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TestSingleWordSpellChecker {

    @Test
    public void singleWordDictionaryTest() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        Assert.assertTrue(spellChecker.decode(vocabulary).contains(vocabulary));
        check1Distance(spellChecker, "elma");

        spellChecker.addWord("armut");
        spellChecker.addWord("ayva");
        check1Distance(spellChecker,"armut");
    }

    @Test
    public void singleWordDictionaryTest2() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        spellChecker.addWord("çapa");
        spellChecker.addWord("lapa");
        spellChecker.addWord("çapak");
        spellChecker.addWord("sapak");
        DoubleValueSet<String> result = spellChecker.decode("çapak");
        for (String s : result) {
            System.out.println(s + "-" + result.get(s));
        }


    }


    private void check1Distance(SingleWordSpellChecker spellChecker, String expected) {
        Set<String> randomDeleted = randomDelete(expected, 1);
        for (String s : randomDeleted) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> randomInserted = randomInsert(expected, 1);
        for (String s : randomInserted) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> randomSubstitute = randomSubstitute(expected, 1);
        for (String s : randomSubstitute) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }

        Set<String> transpositions = transpositions(expected);
        for (String s : transpositions) {
            DoubleValueSet<String> res = spellChecker.decode(s);
            Assert.assertEquals(s, 1, res.size());
            Assert.assertTrue(s, res.contains(expected));
        }
    }

    @Test
    public void nearKeyCheck() {
        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1,true);
        String vocabulary = "elma";
        spellChecker.addWord(vocabulary);
        Assert.assertTrue(spellChecker.decode(vocabulary).contains(vocabulary));

        // "r" is near key "e" therefore it give a smaller penalty.
        DoubleValueSet<String> res1 = spellChecker.decode("rlma");
        Assert.assertTrue(res1.contains("elma"));
        DoubleValueSet<String> res2 = spellChecker.decode("ylma");
        Assert.assertTrue(res2.contains("elma"));
        Assert.assertTrue(res1.get("elma") < res2.get("elma"));

    }

    Set<String> randomDelete(String input, int d) {
        Set<String> result = new HashSet<>();
        Random r = new Random(0xbeef);
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(input);
            for (int j = 0; j < d; j++)
                sb.deleteCharAt(r.nextInt(sb.length()));
            result.add(sb.toString());
        }
        return result;
    }

    Set<String> randomInsert(String input, int d) {
        Set<String> result = new HashSet<>();
        Random r = new Random(0xbeef);
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(input);
            for (int j = 0; j < d; j++)
                sb.insert(r.nextInt(sb.length() + 1), "x");
            result.add(sb.toString());
        }
        return result;
    }

    Set<String> randomSubstitute(String input, int d) {
        Set<String> result = new HashSet<>();
        Random r = new Random(0xbeef);
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(input);
            for (int j = 0; j < d; j++) {
                int start = r.nextInt(sb.length());
                sb.replace(start, start + 1, "x");
            }
            result.add(sb.toString());
        }
        return result;
    }

    Set<String> transpositions(String input) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < input.length()-1; i++) {
            StringBuilder sb = new StringBuilder(input);
            char tmp = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(i+1));
            sb.setCharAt(i+1, tmp);
            result.add(sb.toString());
        }
        return result;
    }


}
