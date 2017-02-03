package zemberek.core.turkish;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class TurkishAlphabetTest {

    @Test
    public void getLetterByChar() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getLetter('c'), TurkishAlphabet.L_c);
        Assert.assertEquals(alphabet.getLetter('a'), TurkishAlphabet.L_a);
        Assert.assertEquals(alphabet.getLetter('w'), TurkishAlphabet.L_w);
        Assert.assertEquals(alphabet.getLetter('z'), TurkishAlphabet.L_z);
        Assert.assertEquals(alphabet.getLetter('x'), TurkishAlphabet.L_x);
        Assert.assertEquals(alphabet.getLetter(TurkishAlphabet.C_cc), TurkishAlphabet.L_cc);
        Assert.assertEquals(alphabet.getLetter(TurkishAlphabet.C_ii), TurkishAlphabet.L_ii);
    }

    @Test
    public void getLetterByIndex() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getLetter(4), TurkishAlphabet.L_cc);
        Assert.assertEquals(alphabet.getLetter(1), TurkishAlphabet.L_a);
        Assert.assertEquals(alphabet.getLetter(3), TurkishAlphabet.L_c);
        Assert.assertEquals(alphabet.getLetter(29), TurkishAlphabet.L_z);
        Assert.assertEquals(alphabet.getLetter(32), TurkishAlphabet.L_x);
        Assert.assertEquals(alphabet.getLetter(11), TurkishAlphabet.L_ii);
    }

    @Test
    public void getAlphabeticIndex() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getAlphabeticIndex('a'), 1);
        Assert.assertEquals(alphabet.getAlphabeticIndex('c'), 3);
        Assert.assertEquals(alphabet.getAlphabeticIndex('z'), 29);
        Assert.assertEquals(alphabet.getAlphabeticIndex('x'), 32);
        Assert.assertEquals(alphabet.getAlphabeticIndex(TurkishAlphabet.C_cc), 4);
        Assert.assertEquals(alphabet.getAlphabeticIndex(TurkishAlphabet.C_ii), 11);
    }

    @Test
    public void getCharByAlphabeticIndex() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getCharByAlphabeticIndex(1), 'a');
        Assert.assertEquals(alphabet.getCharByAlphabeticIndex(3), 'c');
        Assert.assertEquals(alphabet.getCharByAlphabeticIndex(29), 'z');
        Assert.assertEquals(alphabet.getCharByAlphabeticIndex(32), 'x');
        Assert.assertEquals(alphabet.getCharByAlphabeticIndex(4), TurkishAlphabet.C_cc);
        Assert.assertEquals(alphabet.getCharByAlphabeticIndex(11), TurkishAlphabet.C_ii);
    }

    @Test
    public void getAsciiEquivalentLetter() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_a), TurkishAlphabet.L_a);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_x), TurkishAlphabet.L_x);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_cc), TurkishAlphabet.L_c);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_ii), TurkishAlphabet.L_i);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_ss), TurkishAlphabet.L_s);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_gg), TurkishAlphabet.L_g);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_oo), TurkishAlphabet.L_o);
        Assert.assertEquals(alphabet.getAsciiEquivalentLetter(TurkishAlphabet.L_uu), TurkishAlphabet.L_u);
    }

    @Test
    public void getasciiEquivalentLetterByChar() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getAsciEquivalentLetter('a'), TurkishAlphabet.L_a);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter('x'), TurkishAlphabet.L_x);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter(TurkishAlphabet.C_cc), TurkishAlphabet.L_c);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter(TurkishAlphabet.C_ii), TurkishAlphabet.L_i);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter(TurkishAlphabet.C_ss), TurkishAlphabet.L_s);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter(TurkishAlphabet.C_gg), TurkishAlphabet.L_g);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter(TurkishAlphabet.C_oo), TurkishAlphabet.L_o);
        Assert.assertEquals(alphabet.getAsciEquivalentLetter(TurkishAlphabet.C_uu), TurkishAlphabet.L_u);
    }

    @Test
    public void asciiEqual() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertTrue(alphabet.asciiEqual('a', 'a'));
        Assert.assertTrue(alphabet.asciiEqual('x', 'x'));
        Assert.assertTrue(alphabet.asciiEqual(TurkishAlphabet.C_gg, 'g'));
        Assert.assertTrue(alphabet.asciiEqual(TurkishAlphabet.C_cc, 'c'));
        Assert.assertTrue(alphabet.asciiEqual(TurkishAlphabet.C_ii, 'i'));
        Assert.assertTrue(alphabet.asciiEqual(TurkishAlphabet.C_ss, 's'));
        Assert.assertTrue(alphabet.asciiEqual(TurkishAlphabet.C_oo, 'o'));
        Assert.assertTrue(alphabet.asciiEqual(TurkishAlphabet.C_uu, 'u'));
        Assert.assertFalse(alphabet.asciiEqual(TurkishAlphabet.C_uu, 'a'));
    }


    @Test
    public void getAsciiEquivalentChar() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        Assert.assertEquals(alphabet.getAsciiEquivalentChar('a'), 'a');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar('x'), 'x');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_gg), 'g');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_cc), 'c');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_ii), 'i');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_ss), 's');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_oo), 'o');
        Assert.assertEquals(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_uu), 'u');
        Assert.assertNotSame(alphabet.getAsciiEquivalentChar(TurkishAlphabet.C_uu), 'a');
    }

    @Test
    public void isVowelTest() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        String vowels = "aeiuüıoöâîû";
        for (char c : vowels.toCharArray()) {
            Assert.assertTrue(alphabet.isVowel(c));
        }
        String nonvowels = "bcçdfgğjklmnprştvxwzq.";
        for (char c : nonvowels.toCharArray()) {
            Assert.assertFalse(alphabet.isVowel(c));
        }
    }

    @Test
    public void vowelCountTest() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        String[] entries = {"a", "aa", "", "bb", "bebaba"};
        int[] expCounts = {1, 2, 0, 0, 3};
        int i = 0;
        for (String entry : entries) {
            Assert.assertEquals(expCounts[i++], alphabet.vowelCount(entry));
        }
    }

    @Test
    public void toIndexes() {
        TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
        byte[] expected = {1, 2, 3};
        Assert.assertArrayEquals(expected, alphabet.toByteIndexes("abc"));
    }

    @Test
    public void alphabetShouldNotHaveDuplicateChars() {
        final HashMultiset<Character> lowerCaseChars = HashMultiset.create(
                Collections2.transform(Lists.newArrayList(TurkishAlphabet.TURKISH_LETTERS),
                new Function<TurkicLetter, Character>() {
                    @Override
                    public Character apply(TurkicLetter input) {
                        return input.charValue();
                    }
                }));

        for (Multiset.Entry<Character> characterEntry : lowerCaseChars.entrySet()) {
            Assert.assertThat("For char " + characterEntry.getElement() + ", count must be null", characterEntry.getCount(), is(1));
        }
    }
}
