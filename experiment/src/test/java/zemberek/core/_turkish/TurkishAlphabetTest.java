package zemberek.core._turkish;

import static org.hamcrest.core.Is.is;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.turkish.TurkicLetter;

public class TurkishAlphabetTest {

  @Test
  public void getLetterByChar() {
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
    Assert.assertEquals(alphabet.getLetter('c'), _TurkishAlphabet.L_c);
    Assert.assertEquals(alphabet.getLetter('a'), _TurkishAlphabet.L_a);
    Assert.assertEquals(alphabet.getLetter('w'), _TurkishAlphabet.L_w);
    Assert.assertEquals(alphabet.getLetter('z'), _TurkishAlphabet.L_z);
    Assert.assertEquals(alphabet.getLetter('x'), _TurkishAlphabet.L_x);
    Assert.assertEquals(alphabet.getLetter('ç'), _TurkishAlphabet.L_cc);
    Assert.assertEquals(alphabet.getLetter('ı'), _TurkishAlphabet.L_ii);
  }

  @Test
  public void getasciiEquivalentLetterByChar() {
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
    Assert.assertEquals(alphabet.getAsciEquivalentLetter('a'), _TurkishAlphabet.L_a);
    Assert.assertEquals(alphabet.getAsciEquivalentLetter('x'), _TurkishAlphabet.L_x);
    Assert
        .assertEquals(alphabet.getAsciEquivalentLetter('ç'), _TurkishAlphabet.L_c);
    Assert
        .assertEquals(alphabet.getAsciEquivalentLetter('ı'), _TurkishAlphabet.L_i);
    Assert
        .assertEquals(alphabet.getAsciEquivalentLetter('ş'), _TurkishAlphabet.L_s);
    Assert
        .assertEquals(alphabet.getAsciEquivalentLetter('ğ'), _TurkishAlphabet.L_g);
    Assert
        .assertEquals(alphabet.getAsciEquivalentLetter('ö'), _TurkishAlphabet.L_o);
    Assert
        .assertEquals(alphabet.getAsciEquivalentLetter('ü'), _TurkishAlphabet.L_u);
  }

  @Test
  public void asciiEqual() {
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
    Assert.assertTrue(alphabet.asciiEqual('a', 'a'));
    Assert.assertTrue(alphabet.asciiEqual('x', 'x'));
    Assert.assertTrue(alphabet.asciiEqual('ğ', 'g'));
    Assert.assertTrue(alphabet.asciiEqual('ç', 'c'));
    Assert.assertTrue(alphabet.asciiEqual('ı', 'i'));
    Assert.assertTrue(alphabet.asciiEqual('ş', 's'));
    Assert.assertTrue(alphabet.asciiEqual('ö', 'o'));
    Assert.assertTrue(alphabet.asciiEqual('ü', 'u'));
    Assert.assertFalse(alphabet.asciiEqual('ü', 'a'));
  }

  @Test
  public void isVowelTest() {
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
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
    _TurkishAlphabet alphabet = _TurkishAlphabet.INSTANCE;
    String[] entries = {"a", "aa", "", "bb", "bebaba"};
    int[] expCounts = {1, 2, 0, 0, 3};
    int i = 0;
    for (String entry : entries) {
      Assert.assertEquals(expCounts[i++], alphabet.vowelCount(entry));
    }
  }

  @Test
  public void alphabetShouldNotHaveDuplicateChars() {
    final HashMultiset<Character> lowerCaseChars = HashMultiset.create(
        Collections2.transform(Lists.newArrayList(_TurkishAlphabet.TURKISH_LETTERS),
            TurkicLetter::charValue));

    for (Multiset.Entry<Character> characterEntry : lowerCaseChars.entrySet()) {
      Assert.assertThat("For char " + characterEntry.getElement() + ", count must be null",
          characterEntry.getCount(), is(1));
    }
  }
}
