package zemberek.core.io;

import org.junit.Test;

import static org.junit.Assert.*;
import static zemberek.core.io.Strings.*;

/**
 * some parts are copied from commons-lang
 */
public class StringsTest {


    //    ~~~~~~~~~~~ isNullOrEmpty ~~~~~~~~~~~~~~
    @Test
    public void isEmptyTest() {
        assertTrue(isNullOrEmpty(null));
        assertTrue(isNullOrEmpty(""));
        assertFalse(isNullOrEmpty("\n"));
        assertFalse(isNullOrEmpty("\t"));
        assertFalse(isNullOrEmpty(" "));
        assertFalse(isNullOrEmpty("a"));
        assertFalse(isNullOrEmpty("as"));
    }

    //    ~~~~~~~~~~~ hasText ~~~~~~~~~~~~~~
    @Test
    public void hasTextTest() {
        assertFalse(hasText(null));
        assertTrue(hasText("a"));
        assertTrue(hasText("abc"));
        assertFalse(hasText(""));
        assertFalse(hasText(null));
        assertFalse(hasText(" "));
        assertFalse(hasText("\t"));
        assertFalse(hasText("\n"));
        assertFalse(hasText(" \t"));
    }

    @Test
    public void testIfAllHasText() {
        assertTrue(allHasText("fg", "a", "hyh"));
        assertFalse(allHasText("fg", null, "hyh"));
        assertFalse(allHasText("fg", " ", "hyh"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIfAllHasTextExceptionIAE() {
        allHasText();
    }

    @Test
    public void testAllEmpty() {
        assertTrue(allNullOrEmpty("", "", null));
        assertFalse(allNullOrEmpty("", null, "hyh"));
        assertFalse(allNullOrEmpty(" ", "", ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllEmptyExceptionIAE() {
        allNullOrEmpty();
    }

    //    ~~~~~~~~~~~ leftTrim ~~~~~~~~~~~~~~

    @Test
    public void leftTrimTest() {
        assertNull(leftTrim(null));
        assertEquals(leftTrim(""), "");
        assertEquals(leftTrim(" \t "), "");
        assertEquals(leftTrim(" 123"), "123");
        assertEquals(leftTrim("\t123"), "123");
        assertEquals(leftTrim("\n123"), "123");
        assertEquals(leftTrim("123"), "123");
        assertEquals(leftTrim(" \n  123"), "123");
        assertEquals(leftTrim("123 "), "123 ");
        assertEquals(leftTrim(" 3 123 "), "3 123 ");
    }

    //    ~~~~~~~~~~~ rightTrim ~~~~~~~~~~~~~~

    @Test
    public void rightTrimTest() {
        assertNull(rightTrim(null));
        assertEquals(rightTrim(""), "");
        assertEquals(rightTrim(" \t"), "");
        assertEquals(rightTrim("aaa "), "aaa");
        assertEquals(rightTrim("aaa  \t "), "aaa");
        assertEquals(rightTrim("aaa\n "), "aaa");
        assertEquals(rightTrim("aaa"), "aaa");
        assertEquals(rightTrim(" 123 "), " 123");
        assertEquals(rightTrim(" 3 123 \t"), " 3 123");
    }

    //    ~~~~~~~~~~~ repeat ~~~~~~~~~~~~~~

    @Test
    public void repeatTest() {
        assertEquals(repeat('c', -1), "");
        assertEquals(repeat('c', 3), "ccc");
        assertEquals(repeat('c', 1), "c");
        assertEquals(repeat('c', 0), "");

        assertNull(repeat(null, 1));
        assertEquals(repeat("ab", -1), "");
        assertEquals(repeat("ab", 3), "ababab");
        assertEquals(repeat("ab", 1), "ab");
        assertEquals(repeat("ab", 0), "");
    }

    //    ~~~~~~~~~~~ reverse ~~~~~~~~~~~~~~

    @Test
    public void reverseTest() {
        assertNull(reverse(null), null);
        assertEquals(reverse(""), "");
        assertEquals(reverse("a"), "a");
        assertEquals(reverse("ab"), "ba");
        assertEquals(reverse("ab cd "), " dc ba");
    }

    //    ~~~~~~~~~~~ insertFromLeft ~~~~~~~~~~~~~~

    @Test
    public void insertFromLeftTest() {
        final String s = "0123456789";
        assertEquals(insertFromLeft(s, 0, "-"), "0123456789");
        assertEquals(insertFromLeft(s, 1, "-"), "0-1-2-3-4-5-6-7-8-9");
        assertEquals(insertFromLeft("ahmet", 1, " "), "a h m e t");
        assertEquals(insertFromLeft(s, 2, "-"), "01-23-45-67-89");
        assertEquals(insertFromLeft(s, 3, "-"), "012-345-678-9");
        assertEquals(insertFromLeft(s, 5, "-"), "01234-56789");
        assertEquals(insertFromLeft(s, 6, "-"), "012345-6789");
        assertEquals(insertFromLeft(s, 9, "-"), "012345678-9");
        assertEquals(insertFromLeft(s, 10, "-"), "0123456789");
        assertEquals(insertFromLeft(s, 12, "-"), "0123456789");
        assertEquals(insertFromLeft(s, 2, "--"), "01--23--45--67--89");
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertFromLeftExceptionTest2() {
        insertFromLeft("123", -1, "-");
    }

    //    ~~~~~~~~~~~ insertFromRight ~~~~~~~~~~~~~~

    @Test
    public void insertFromRightTest() {
        final String s = "0123456789";
        assertEquals(insertFromRight(s, 0, "-"), "0123456789");
        assertEquals(insertFromRight(s, 1, "-"), "0-1-2-3-4-5-6-7-8-9");
        assertEquals(insertFromRight(s, 2, "-"), "01-23-45-67-89");
        assertEquals(insertFromRight(s, 3, "-"), "0-123-456-789");
        assertEquals(insertFromRight(s, 5, "-"), "01234-56789");
        assertEquals(insertFromRight(s, 6, "-"), "0123-456789");
        assertEquals(insertFromRight(s, 9, "-"), "0-123456789");
        assertEquals(insertFromRight(s, 10, "-"), "0123456789");
        assertEquals(insertFromRight(s, 12, "-"), "0123456789");
        assertEquals(insertFromRight(s, 2, "--"), "01--23--45--67--89");
        assertEquals(insertFromRight(s, 3, "--"), "0--123--456--789");
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertFromRightExceptionTest2() {
        insertFromRight("123", -1, "-");
    }

// ------------ Tests below is taken from commons logging ---------------------------

    @Test
    public void testRightPad_StringInt() {
        assertEquals(null, rightPad(null, 5));
        assertEquals("     ", rightPad("", 5));
        assertEquals("abc  ", rightPad("abc", 5));
        assertEquals("abc", rightPad("abc", 2));
        assertEquals("abc", rightPad("abc", -1));
    }

    @Test
    public void testRightPad_StringIntChar() {
        assertEquals(null, rightPad(null, 5, ' '));
        assertEquals("     ", rightPad("", 5, ' '));
        assertEquals("abc  ", rightPad("abc", 5, ' '));
        assertEquals("abc", rightPad("abc", 2, ' '));
        assertEquals("abc", rightPad("abc", -1, ' '));
        assertEquals("abcxx", rightPad("abc", 5, 'x'));
        String str = rightPad("aaa", 10000, 'a');  // bigger than pad length
        assertEquals(10000, str.length());
    }

    @Test
    public void testRightPad_StringIntString() {
        assertEquals(null, rightPad(null, 5, "-+"));
        assertEquals("     ", rightPad("", 5, " "));
        assertEquals(null, rightPad(null, 8, null));
        assertEquals("abc-+-+", rightPad("abc", 7, "-+"));
        assertEquals("abc-+~", rightPad("abc", 6, "-+~"));
        assertEquals("abc-+", rightPad("abc", 5, "-+~"));
        assertEquals("abc", rightPad("abc", 2, " "));
        assertEquals("abc", rightPad("abc", -1, " "));
        assertEquals("abc  ", rightPad("abc", 5, null));
        assertEquals("abc  ", rightPad("abc", 5, ""));
    }

    @Test
    public void testLeftPad_StringInt() {
        assertEquals(null, leftPad(null, 5));
        assertEquals("     ", leftPad("", 5));
        assertEquals("  abc", leftPad("abc", 5));
        assertEquals("abc", leftPad("abc", 2));
    }

    @Test
    public void testLeftPad_StringIntChar() {
        assertEquals(null, leftPad(null, 5, ' '));
        assertEquals("     ", leftPad("", 5, ' '));
        assertEquals("  abc", leftPad("abc", 5, ' '));
        assertEquals("xxabc", leftPad("abc", 5, 'x'));
        assertEquals("\uffff\uffffabc", leftPad("abc", 5, '\uffff'));
        assertEquals("abc", leftPad("abc", 2, ' '));
        String str = leftPad("aaa", 10000, 'a');  // bigger than pad length
        assertEquals(10000, str.length());
    }

    @Test
    public void testLeftPad_StringIntString() {
        assertEquals(null, leftPad(null, 5, "-+"));
        assertEquals(null, leftPad(null, 5, null));
        assertEquals("     ", leftPad("", 5, " "));
        assertEquals("-+-+abc", leftPad("abc", 7, "-+"));
        assertEquals("-+~abc", leftPad("abc", 6, "-+~"));
        assertEquals("-+abc", leftPad("abc", 5, "-+~"));
        assertEquals("abc", leftPad("abc", 2, " "));
        assertEquals("abc", leftPad("abc", -1, " "));
        assertEquals("  abc", leftPad("abc", 5, null));
        assertEquals("  abc", leftPad("abc", 5, ""));
    }

    @Test
    public void testWhiteSpacesToSingleSpace() {
        assertEquals(whiteSpacesToSingleSpace(null), null);
        assertEquals(whiteSpacesToSingleSpace(""), "");
        assertEquals(whiteSpacesToSingleSpace("asd"), "asd");
        assertEquals(whiteSpacesToSingleSpace("a  a"), "a a");
        assertEquals(whiteSpacesToSingleSpace(" "), " ");
        assertEquals(whiteSpacesToSingleSpace("\t"), " ");
        assertEquals(whiteSpacesToSingleSpace("\n"), " ");
        assertEquals(whiteSpacesToSingleSpace("\t \n"), " ");
        assertEquals(whiteSpacesToSingleSpace("  \t  \n\r \f"), " ");
        assertEquals(whiteSpacesToSingleSpace("  a\t a\r\fa"), " a a a");
    }

    @Test
    public void testEliminateWhiteSpaces() {
        assertEquals(eliminateWhiteSpaces(null), null);
        assertEquals(eliminateWhiteSpaces(""), "");
        assertEquals(eliminateWhiteSpaces("asd"), "asd");
        assertEquals(eliminateWhiteSpaces("a "), "a");
        assertEquals(eliminateWhiteSpaces("a  a "), "aa");
        assertEquals(eliminateWhiteSpaces("a \t a \t\r\f"), "aa");
    }

    @Test
    public void testSubstringAfterFirst() {
        assertEquals(subStringAfterFirst("hello", "el"), "lo");
        assertEquals(subStringAfterFirst("hellohello", "el"), "lohello");
        assertEquals(subStringAfterFirst("hello", "hello"), "");
        assertEquals(subStringAfterFirst("hello", ""), "hello");
        assertEquals(subStringAfterFirst("hello", null), "hello");
        assertEquals(subStringAfterFirst("", "el"), "");
        assertEquals(subStringAfterFirst(null, "el"), null);
    }

    @Test
    public void testSubstringAfterLast() {
        assertEquals(subStringAfterLast("hello\\world", "\\"), "world");
        assertEquals(subStringAfterLast("hello", "el"), "lo");
        assertEquals(subStringAfterLast("hellohello", "el"), "lo");
        assertEquals(subStringAfterLast("hello", "hello"), "");
        assertEquals(subStringAfterLast("hello", ""), "hello");
        assertEquals(subStringAfterLast("hello", null), "hello");
        assertEquals(subStringAfterLast("", "el"), "");
        assertEquals(subStringAfterLast(null, "el"), null);
    }

    @Test
    public void testSubstringUntilFirst() {
        assertEquals(subStringUntilFirst("hello", "el"), "h");
        assertEquals(subStringUntilFirst("hellohello", "el"), "h");
        assertEquals(subStringUntilFirst("hello", "hello"), "");
        assertEquals(subStringUntilFirst("hello", ""), "hello");
        assertEquals(subStringUntilFirst("hello", null), "hello");
        assertEquals(subStringUntilFirst("", "el"), "");
        assertEquals(subStringUntilFirst(null, "el"), null);
    }

    @Test
    public void testSubstringUntilLast() {
        assertEquals(subStringUntilLast("hello", "el"), "h");
        assertEquals(subStringUntilLast("hellohello", "el"), "helloh");
        assertEquals(subStringUntilLast("hello", "hello"), "");
        assertEquals(subStringUntilLast("hello", ""), "hello");
        assertEquals(subStringUntilLast("hello", null), "hello");
        assertEquals(subStringUntilLast("", "el"), "");
        assertEquals(subStringUntilLast(null, "el"), null);
    }

    @Test
    public void testGrams() {
        assertArrayEquals(separateGrams("hello", 1), new String[]{"h", "e", "l", "l", "o"});
        assertArrayEquals(separateGrams("hello", 2), new String[]{"he", "el", "ll", "lo"});
        assertArrayEquals(separateGrams("hello", 3), new String[]{"hel", "ell", "llo"});
        assertArrayEquals(separateGrams("hello", 4), new String[]{"hell", "ello"});
        assertArrayEquals(separateGrams("hello", 5), new String[]{"hello"});
        assertArrayEquals(separateGrams("hello", 6), EMPTY_STRING_ARRAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void gram0sizeExceptionTest() {
        separateGrams("123",0);
    }

}
