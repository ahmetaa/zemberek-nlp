package zemberek.core.turkish.hyphenation;


import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkicSeq;
import zemberek.core.turkish.TurkishAlphabet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This syllable service is designed for extracting syllable information from Turkish words.
 * This class uses a strict syllable extraction algorithm, meaning that it cannot parse words like
 * "tren", "spor", "sfinks", "angstrom", "mavimtrak", "stetoskop" etc.
 */
public class StrictTurkishSyllableParser implements SyllableParser {

    private final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

    /**
     * Returns the syllables as a String List. if word cannot be parsed, an empty list is returned.
     * <p>Example
     * <p><code>("merhaba") -> ["mer","ha","ba"]</code>
     * <p><code>("mr") -> []</code>
     * <p><code>("al") -> ["al"]</code>
     *
     * @param input input string.
     * @return syllables as string list. if there is no syllables, an empty list.
     */
    public List<String> parse(String input) {
        TurkicSeq sequence = new TurkicSeq(input, alphabet);
        List<String> list = new ArrayList<String>();
        while (input.length() > 0) {
            int index = letterCountForLastSyllable(sequence);
            if (index < 0) {
                return Collections.emptyList();
            }
            int basla = sequence.length() - index;
            list.add(sequence.toString(basla));
            sequence.clip(basla);
        }
        Collections.reverse(list);
        return list;
    }


    /**
     * Returns the letter count of the last syllable of a given word.
     * This algorithm does not work for words that starts with [tr-,st-,pr-] or ends with [-trak]
     * Foreign letter words also cannot be processed.
     *
     * @param seq: TurkicSeq object.
     * @return Size of the last syllable. It can be 1,2,3 or 4. Returns -1 if syllable rules are not met.
     */
    private int letterCountForLastSyllable(TurkicSeq seq) {

        final int length = seq.length();
        TurkicLetter current = seq.getLetter(length - 1);
        TurkicLetter previous = seq.getLetter(length - 2);

        if (length == 0)
            return -1;

        if (current.isVowel()) {
            //seq consist of a single vowel
            if (length == 1) {
                return 1;
            }
            //current and previous letters are vowels. Eg. "saa"
            if (previous.isVowel()) {
                return 1;
            }
            // length is two and previous is vowel. Eg. "ya"
            if (length == 2) {
                return 2;
            }

            TurkicLetter twoBefore = seq.getLetter(length - 3);
            // ste-tos-kop -> ste
            if (!twoBefore.isVowel() && length == 3) {
                return 3;
            }
            return 2;
        } else {

            // single consonant.
            if (length == 1) {
                return -1;
            }

            TurkicLetter twoBefore = seq.getLetter(length - 3);

            if (previous.isVowel()) {

                //For words like [el, al] or two letter before is vowel. (`at` in sa-at)
                if (length == 2 || twoBefore.isVowel()) {
                    return 2;
                }

                TurkicLetter threeBefore = seq.getLetter(length - 4);
                // seq uc harfli (kal, sel) ya da uc onceki harf sesli (kanat),
                if (length == 3 || threeBefore.isVowel()) {
                    return 3;
                }

                // If length is 4 and previous rules could not apply, it is considered not a syllable.
                // Such as tren, strateji, krank, angstrom.
                if (length == 4) {
                    return -1;
                }

                TurkicLetter fourBefore = seq.getLetter(length - 5);
                if (!fourBefore.isVowel()) {
                    return 3;
                }
                return 3;

            } else {
                if (length == 2 || !twoBefore.isVowel()) {
                    return -1;
                }
                TurkicLetter threeBefore = seq.getLetter(length - 4);
                if (length > 3 && !threeBefore.isVowel()) {
                    return 4;
                }
                return 3;
            }
        }

    }

}