package zemberek.hyphenation;


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

    private final TurkishAlphabet alphabet = new TurkishAlphabet();

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
     * Giren harf dizisinin sonunda mantikli olarak yer alan hecenin harf
     * sayisini dondurur.
     * Sistem, -trak ve benzeri harf dizilimine sahip kelimeleri hecelemiyor.
     *
     * @param seq: turkce harf dizisi.
     * @return int, 1,2,3 ya da 4 donerse giris dizisinin dizinin sondan o
     *         kadarharfi heceyi temsil eder -1 donerse hecenin bulunamadigi
     *         anlamina gelir. sistem yabanci harf ya da isaretlerin oldugu ya
     *         da kural disi kelimeleri heceleyemez. (ornegin, three, what vs.)
     *         sistem su anda basta bulunan iki harf sessiz oldugu
     *         durumlari kabul etmekte ama buna kisitlama getirilmesi iyi olur.
     *         sadece "tr", "st", "kr" gibi girislere izin verilmeli
     */
    private int letterCountForLastSyllable(TurkicSeq seq) {

        final int boy = seq.length();
        TurkicLetter harf = seq.getLetter(boy - 1);
        TurkicLetter oncekiHarf = seq.getLetter(boy - 2);

        if (boy == 0)
            return -1;

        if (harf.isVowel()) {
            //seq sadece sesli.
            if (boy == 1)
                return 1;
            //onceki harf sesli seq="saa" ise son ek "a"
            if (oncekiHarf.isVowel())
                return 1;
            //onceki harf sessiz ise ve seq sadece 2 harf ise hece tum seq. "ya"
            if (boy == 2)
                return 2;

            TurkicLetter ikiOncekiHarf = seq.getLetter(boy - 3);

            //ste-tos-kop -> ste
            if (!ikiOncekiHarf.isVowel() && boy == 3) {
                return 3;
            }
            return 2;
        } else {

            // tek sessiz ile hece olmaz.
            if (boy == 1)
                return -1;

            TurkicLetter ikiOncekiHarf = seq.getLetter(boy - 3);
            if (oncekiHarf.isVowel()) {

                //seq iki harfli (el, al) ya da iki onceki harf sesli (saat)
                if (boy == 2 || ikiOncekiHarf.isVowel())
                    return 2;

                TurkicLetter ucOncekiHarf = seq.getLetter(boy - 4);
                // seq uc harfli (kal, sel) ya da uc onceki harf sesli (kanat),
                if (boy == 3 || ucOncekiHarf.isVowel())
                    return 3;

                //seq dort harfli ise yukaridaki kurallari gecmesi nedeniyle hecelenemez sayiyoruz.
                // tren, strateji, krank, angstrom gibi kelimeler henuz hecelenmiyor.
                if (boy == 4)
                    return -1;

                TurkicLetter dortOncekiHarf = seq.getLetter(boy - 5);
                if (!dortOncekiHarf.isVowel())
                    return 3;
                return 3;

            } else {

                if (boy == 2 || !ikiOncekiHarf.isVowel())
                    return -1;
                TurkicLetter ucOncekiHarf = seq.getLetter(boy - 4);
                if (boy > 3 && !ucOncekiHarf.isVowel())
                    return 4;
                return 3;
            }

        }

    }

}