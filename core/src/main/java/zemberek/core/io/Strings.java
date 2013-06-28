/*
 *
 * Copyright (c) 2008, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Portions of the code may be copied from Google Collections
 * or Apache Commons projects.
 */

package zemberek.core.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Helper methods for String operations.
 * Some of the code is copied and slightly modified from Apache commons-lang  2.3
 */
public final class Strings {

    /**
     * The empty String <code>""</code>.
     */
    public static final String EMPTY_STRING = "";

    /**
     * Zero length String array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * <p>The maximum size to which the padding constant(s) can expand.</p>
     */
    private static final int PAD_LIMIT = 8192;

    private Strings() {
    }

    /**
     * <p>Checks if a String is empty ("") or null.</p>
     * <p/>
     * <pre>
     * Strings.isEmpty(null)      = true
     * Strings.isEmpty("")        = true
     * Strings.isEmpty(" ")       = false
     * Strings.isEmpty("bob")     = false
     * Strings.isEmpty("  bob  ") = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * checks if a string has text content other than white space.
     *
     * @param s value to check
     * @return true if it is not null, or has content other than white space white space
     */
    public static boolean hasText(String s) {
        return s != null && s.length() > 0 && s.trim().length() > 0;
    }

    /**
     * checks if all of the Strings has text (NOT null, zero length or only whitespace)
     *
     * @param strings arbitrry number of Strings.
     * @return true if ALL strings contain text.
     */
    public static boolean allHasText(String... strings) {
        checkVarargString(strings);
        for (String s : strings) {
            if (!hasText(s)) return false;
        }
        return true;
    }

    private static void checkVarargString(String... strings) {
        if (strings == null)
            throw new NullPointerException("Input array should be non null!");
        if (strings.length == 0)
            throw new IllegalArgumentException("At least one parameter is required.");
    }

    /**
     * checks if all of the Strings are empty (null or zero length)
     *
     * @param strings arbitrry number of Strings.
     * @return true if all Strings are empty
     */
    public static boolean allNullOrEmpty(String... strings) {
        checkVarargString(strings);
        for (String s : strings) {
            if (!isNullOrEmpty(s)) return false;
        }
        return true;
    }

    /**
     * Trims white spaces from left side.
     *
     * @param s input String
     * @return a new string left white spaces trimmed. null, if <code>s</code> is null
     */
    public static String leftTrim(String s) {
        if (s == null) return null;
        if (s.length() == 0) return EMPTY_STRING;
        int j = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i)))
                j++;
            else
                break;
        }
        return s.substring(j);
    }

    /**
     * Trims white spaces from right side.
     *
     * @param str input String
     * @return a new string right white spaces trimmed. null, if <code>str</code> is null
     */
    public static String rightTrim(String str) {
        if (str == null)
            return null;
        if (str.length() == 0) return EMPTY_STRING;
        int j = str.length();
        for (int i = str.length() - 1; i >= 0; --i) {
            if (Character.isWhitespace(str.charAt(i)))
                j--;
            else
                break;
        }
        return str.substring(0, j);
    }

    // ContainsNone
    //-----------------------------------------------------------------------
    /**
     * <p>Checks that the String does not contain certain characters.</p>
     * <p/>
     * <p>A <code>null</code> String will return <code>true</code>.
     * A <code>null</code> invalid character array will return <code>true</code>.
     * An empty String ("") always returns true.</p>
     * <p/>
     * <pre>
     * Strings.containsNone(null, *)       = true
     * Strings.containsNone(*, null)       = true
     * Strings.containsNone("", *)         = true
     * Strings.containsNone("ab", "")      = true
     * Strings.containsNone("abab", "xyz") = true
     * Strings.containsNone("ab1", "xyz")  = true
     * Strings.containsNone("abz", "xyz")  = false
     * </pre>
     *
     * @param str             the String to check, may be null
     * @param invalidCharsStr string containing invalid chars
     * @return true if it contains none of the invalid chars, or is null
     * @since 2.0
     */
    public static boolean containsNone(String str, String invalidCharsStr) {
        if (str == null || invalidCharsStr == null) {
            return true;
        }
        int strSize = str.length();
        int validSize = invalidCharsStr.length();
        for (int i = 0; i < strSize; i++) {
            char ch = str.charAt(i);
            for (int j = 0; j < validSize; j++) {
                if (invalidCharsStr.charAt(j) == ch) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the input string only contains allowedCharacters.
     * @param str input String.
     * @param allowedChars allowed characters.
     * @return if String contains only the allowed characters or input values are null, returns true. false otherwise.
     */
    public static boolean containsOnly(String str, String allowedChars) {
        if (str == null || allowedChars == null) {
            return true;
        }
        char[] allowed = allowedChars.toCharArray();
        for (char c : str.toCharArray()) {
            boolean found = false;
            for (char v : allowed)
                if (c == v) {
                    found = true;
                    break;
                }
            if (!found)
                return false;
        }
        return true;
    }

    /**
     * Builds a {@link String} by repeating a character a specified number of times.
     * <p/>
     * Author Juan Antonio Ramirez
     *
     * @param c     the character to use to compose the {@link String}
     * @param count how many times to repeat the character argument
     * @return a {@link String} composed of the <code>c</code> character
     *         repeated <code>count</code> times. Empty if <code>count</code> is less then 1
     */
    public static String repeat(char c, int count) {
        if (count < 1) return EMPTY_STRING;
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    /**
     * Builds a {@link String} by repeating a string a specified number of times.
     * Author Juan Antonio Ramirez
     *
     * @param str   the string to use to compose the {@link String}
     * @param count how many times to repeat the string argument
     * @return a {@link String} composed of the <code>str</code> string
     *         repeated <code>count</code> times. null, if <code>str</code> is null.
     *         Empty if <code>count</code> is less then 1.
     */
    public static String repeat(String str, int count) {
        if (str == null)
            return null;
        if (count < 1)
            return EMPTY_STRING;
        StringBuilder builder = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(str);
        }
        return builder.toString();
    }


    /**
     * reverses a string.
     *
     * @param str input string.
     * @return reversed string. null, if <code>str</code> is null
     */
    public static String reverse(String str) {
        if (str == null)
            return null;
        if (str.length() == 0)
            return EMPTY_STRING;
        return new StringBuilder(str).reverse().toString();
    }


    /**
     * inserts the <code>stringToInsert</code> with given <code>interval</code> starting from left.
     * </p>
     * <pre>("0123456", 2, "-") returns "01-23-45-6"</pre>
     *
     * @param str            input string
     * @param interval       : interval amount
     * @param stringToInsert : character to insert.
     * @return the formatted string. null, if <code>str</code> is null
     * @throws IllegalArgumentException if <code>interval</code> is negative
     */
    public static String insertFromLeft(String str, int interval, String stringToInsert) {
        if (interval < 0)
            throw new IllegalArgumentException("interval value cannot be negative.");
        if (str == null || interval == 0 || interval >= str.length() || isNullOrEmpty(stringToInsert))
            return str;
        StringBuilder b = new StringBuilder();
        int i = 0;
        for (char c : str.toCharArray()) {
            b.append(c);
            i++;
            if (i % interval == 0 && i <= str.length() - 1)
                b.append(stringToInsert);
        }
        return b.toString();
    }

    /**
     * inserts the <code>stringToInsert</code> with given <code>interval</code> starting from right.
     * </p>
     * <pre>("0123456", 2, "-") returns "0-12-34-56"</pre>
     *
     * @param str            input string
     * @param interval       : interval amount
     * @param stringToInsert : character to insert.
     * @return the formatted string. null, if <code>str</code> is null
     * @throws IllegalArgumentException if <code>interval</code> is negative
     */
    public static String insertFromRight(String str, int interval, String stringToInsert) {
        if (interval < 0)
            throw new IllegalArgumentException("interval value cannot be negative.");
        if (str == null || interval == 0 || interval >= str.length() || isNullOrEmpty(stringToInsert))
            return str;
        StringBuilder b = new StringBuilder();
        int j = 0;
        for (int i = str.length() - 1; i >= 0; i--) {
            b.append(str.charAt(i));
            j++;
            if (j % interval == 0 && j <= str.length() - 1)
                b.append(stringToInsert);
        }
        return reverse(b.toString());
    }

    /**
     * <p>Right pad a String with spaces (' ').</p>
     * <p/>
     * <p>The String is padded to the size of <code>size</code>.</p>
     * <p/>
     * <pre>
     * StringUtils.rightPad(null, *)   = null
     * StringUtils.rightPad("", 3)     = "   "
     * StringUtils.rightPad("bat", 3)  = "bat"
     * StringUtils.rightPad("bat", 5)  = "bat  "
     * StringUtils.rightPad("bat", 1)  = "bat"
     * StringUtils.rightPad("bat", -1) = "bat"
     * </pre>
     *
     * @param str  the String to pad out, may be null
     * @param size the size to pad to
     * @return right padded String or original String if no padding is necessary,
     *         <code>null</code> if null String input
     */
    public static String rightPad(String str, int size) {
        return rightPad(str, size, ' ');
    }

    /**
     * <p>Right pad a String with a specified character.</p>
     * <p/>
     * <p>The String is padded to the size of <code>size</code>.</p>
     * <p/>
     * <pre>
     * StringUtils.rightPad(null, *, *)     = null
     * StringUtils.rightPad("", 3, 'z')     = "zzz"
     * StringUtils.rightPad("bat", 3, 'z')  = "bat"
     * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
     * StringUtils.rightPad("bat", 1, 'z')  = "bat"
     * StringUtils.rightPad("bat", -1, 'z') = "bat"
     * </pre>
     *
     * @param str     the String to pad out, may be null
     * @param size    the size to pad to
     * @param padChar the character to pad with
     * @return right padded String or original String if no padding is necessary,
     *         <code>null</code> if null String input
     * @since 2.0
     */
    public static String rightPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            return rightPad(str, size, String.valueOf(padChar));
        }
        return str.concat(repeat(padChar, pads));
    }

    /**
     * <p>Right pad a String with a specified String.</p>
     * <p/>
     * <p>The String is padded to the size of <code>size</code>.</p>
     * <p/>
     * <pre>
     * StringUtils.rightPad(null, *, *)      = null
     * StringUtils.rightPad("", 3, "z")      = "zzz"
     * StringUtils.rightPad("bat", 3, "yz")  = "bat"
     * StringUtils.rightPad("bat", 5, "yz")  = "batyz"
     * StringUtils.rightPad("bat", 8, "yz")  = "batyzyzy"
     * StringUtils.rightPad("bat", 1, "yz")  = "bat"
     * StringUtils.rightPad("bat", -1, "yz") = "bat"
     * StringUtils.rightPad("bat", 5, null)  = "bat  "
     * StringUtils.rightPad("bat", 5, "")    = "bat  "
     * </pre>
     *
     * @param str    the String to pad out, may be null
     * @param size   the size to pad to
     * @param padStr the String to pad with, null or empty treated as single space
     * @return right padded String or original String if no padding is necessary,
     *         <code>null</code> if null String input
     */
    public static String rightPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isNullOrEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return rightPad(str, size, padStr.charAt(0));
        }

        if (pads == padLen) {
            return str.concat(padStr);
        } else if (pads < padLen) {
            return str.concat(padStr.substring(0, pads));
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return str.concat(new String(padding));
        }
    }

    /**
     * <p>Left pad a String with spaces (' ').</p>
     * <p/>
     * <p>The String is padded to the size of <code>size<code>.</p>
     * <p/>
     * <pre>
     * StringUtils.leftPad(null, *)   = null
     * StringUtils.leftPad("", 3)     = "   "
     * StringUtils.leftPad("bat", 3)  = "bat"
     * StringUtils.leftPad("bat", 5)  = "  bat"
     * StringUtils.leftPad("bat", 1)  = "bat"
     * StringUtils.leftPad("bat", -1) = "bat"
     * </pre>
     *
     * @param str  the String to pad out, may be null
     * @param size the size to pad to
     * @return left padded String or original String if no padding is necessary,
     *         <code>null</code> if null String input
     */
    public static String leftPad(String str, int size) {
        return leftPad(str, size, " ");
    }

    /**
     * <p>Left pad a String with a specified character.</p>
     * <p/>
     * <p>Pad to a size of <code>size</code>.</p>
     * <p/>
     * <pre>
     * StringUtils.leftPad(null, *, *)     = null
     * StringUtils.leftPad("", 3, 'z')     = "zzz"
     * StringUtils.leftPad("bat", 3, 'z')  = "bat"
     * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
     * StringUtils.leftPad("bat", 1, 'z')  = "bat"
     * StringUtils.leftPad("bat", -1, 'z') = "bat"
     * </pre>
     *
     * @param str     the String to pad out, may be null
     * @param size    the size to pad to
     * @param padChar the character to pad with
     * @return left padded String or original String if no padding is necessary,
     *         <code>null</code> if null String input
     * @since 2.0
     */
    public static String leftPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            return leftPad(str, size, String.valueOf(padChar));
        }
        return repeat(padChar, pads).concat(str);
    }

    /**
     * returns the initial part of a string until the first occurance of a given string.
     * </p>
     * <pre>
     * ("hello","lo") -> hel
     * ("hello", "zo") -> hello
     * ("hello", "hello") -> "" empty string.
     * ("hello",null)-> hello
     * (null,"hello")-> null
     * (null,null)-> null
     * </pre>
     *
     * @param str input string
     * @param s   string to search first occurance.
     * @return the substring
     */
    public static String subStringUntilFirst(String str, String s) {
        if (isNullOrEmpty(str) || isNullOrEmpty(s))
            return str;
        final int pos = str.indexOf(s);
        if (pos < 0)
            return str;
        else return str.substring(0, pos);
    }

    /**
     * returns the initial part of a string until the last occurance of a given string.
     * </p>
     * <pre>
     * ("hellohello","lo") -> hellohel
     * ("hellohello","el") -> helloh
     * ("hellolo", "zo") -> hellolo
     * ("hello", "hello") -> "" empty string.
     * ("hello",null)-> hello
     * (null,"hello")-> null
     * (null,null)-> null
     * </pre>
     *
     * @param str input string
     * @param s   string to search last occurance.
     * @return the substring
     */
    public static String subStringUntilLast(String str, String s) {
        if (isNullOrEmpty(str) || isNullOrEmpty(s))
            return str;
        final int pos = str.lastIndexOf(s);
        if (pos < 0)
            return str;
        return str.substring(0, pos);
    }

    /**
     * <p>returns the last part of a string after the first occurance of a given string.</p>
     * </p>
     * <pre>
     * ("hello","el") -> lo
     * ("hellohello","el") -> lohello
     * ("hello", "zo") -> hello
     * ("hello", "hello") -> "" empty string.
     * ("hello",null)-> hello
     * (null,"hello")-> null
     * (null,null)-> null
     * </pre>
     *
     * @param str input string
     * @param s   string to search first occurance.
     * @return the substring
     */
    public static String subStringAfterFirst(String str, String s) {
        if (isNullOrEmpty(str) || isNullOrEmpty(s))
            return str;
        final int pos = str.indexOf(s);
        if (pos < 0)
            return str;
        else return str.substring(pos + s.length());
    }

    /**
     * returns the last part of a string after the last occurance of a given string.
     * </p>
     * <pre>
     * ("hello","el") -> lo
     * ("hellohello","el") -> lo
     * ("hello", "zo") -> hello
     * ("hello", "hello") -> "" empty string.
     * ("hello",null)-> hello
     * (null,"hello")-> null
     * (null,null)-> null
     * </pre>
     *
     * @param str input string
     * @param s   string to search first occurance.
     * @return the substring
     */
    public static String subStringAfterLast(String str, String s) {
        if (isNullOrEmpty(str) || isNullOrEmpty(s))
            return str;
        final int pos = str.lastIndexOf(s);
        if (pos < 0)
            return str;
        else return str.substring(pos + s.length());
    }

    /**
     * <p>Left pad a String with a specified String.</p>
     * <p/>
     * <p>Pad to a size of <code>size</code>.</p>
     * <p/>
     * <pre>
     * StringUtils.leftPad(null, *, *)      = null
     * StringUtils.leftPad("", 3, "z")      = "zzz"
     * StringUtils.leftPad("bat", 3, "yz")  = "bat"
     * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
     * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
     * StringUtils.leftPad("bat", 1, "yz")  = "bat"
     * StringUtils.leftPad("bat", -1, "yz") = "bat"
     * StringUtils.leftPad("bat", 5, null)  = "  bat"
     * StringUtils.leftPad("bat", 5, "")    = "  bat"
     * </pre>
     *
     * @param str    the String to pad out, may be null
     * @param size   the size to pad to
     * @param padStr the String to pad with, null or empty treated as single space
     * @return left padded String or original String if no padding is necessary,
     *         <code>null</code> if null String input
     */
    public static String leftPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isNullOrEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads == padLen) {
            return padStr.concat(str);
        } else if (pads < padLen) {
            return padStr.substring(0, pads).concat(str);
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding).concat(str);
        }
    }

    private static final Pattern MULTI_SPACE = Pattern.compile(" +");
    private static final Pattern WHITE_SPACE_EXCEPT_SPACE = Pattern.compile("[\\t\\n\\x0B\\f\\r]");
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s");

    /**
     * Converts all white spaces to single space. Also eliminates multiple spaces,
     * </p>
     * <pre>
     * "  a  aaa \t \n    a\taa  " -> " a aaa a aa "
     * </pre>
     *
     * @param str input string.
     * @return all white spaces are converted to space character and multiple space chars reduced to single space.
     *         returns null if <code>str<code> is null
     */
    public static String whiteSpacesToSingleSpace(String str) {
        if (str == null) return null;
        if (str.isEmpty()) return str;
        return MULTI_SPACE.matcher(WHITE_SPACE_EXCEPT_SPACE.matcher(str).replaceAll(" ")).replaceAll(" ");
    }

    /**
     * Eliminates all white spaces.
     *
     * @param str input string.
     * @return returns the string after all white spaces are stripped. null, if str is null
     */
    public static String eliminateWhiteSpaces(String str) {
        if (str == null) return null;
        if (str.isEmpty()) return str;
        return WHITE_SPACE.matcher(str).replaceAll("");
    }

    /**
     * Generates 'gram' Strings from a given String. Such as,
     * </p>
     * <pre>
     * for ("hello",2) it returns ["he","el","ll","lo"]
     * for ("hello",3) it returns ["hel","ell","llo"]
     * </pre>
     *
     * @param word     input String
     * @param gramSize size of the gram.
     * @return the grams as an array. if the gram size is larger than the word itself, it retuns an empty array.
     *         gram size cannot be smaller than 1
     * @throws IllegalArgumentException if gram size is smaller than 1
     */
    public static String[] separateGrams(String word, int gramSize) {
        if (gramSize < 1)
            throw new IllegalArgumentException("Gram size cannot be smaller than 1");
        if (gramSize > word.length())
            return EMPTY_STRING_ARRAY;
        String[] grams = new String[word.length() - gramSize + 1];
        for (int i = 0; i <= word.length() - gramSize; i++) {
            grams[i] = word.substring(i, i + gramSize);
        }
        return grams;
    }

}
