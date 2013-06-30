package zemberek.morphology.phonetics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import smoothnlp.core.io.IOs;
import smoothnlp.core.io.KeyValueReader;
import zemberek.core.turkish.TurkishAlphabet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TurkishPhonemes {
    private static Map<Character, Phoneme> charToPhonemeMap = Maps.newHashMap();
    private static Map<String, Phoneme> phonemeMap = Maps.newHashMap();
    private static Map<Character, Character> charToTurkish = Maps.newHashMap();

    public static final Phoneme P_a = new Phoneme("a", "a", "a", PhoneticSound.OPEN_BACK_UNROUNDED_VOWEL_A);
    public static final Phoneme P_a_LONG = new Phoneme("a:", "a:", "a", PhoneticSound.OPEN_BACK_UNROUNDED_VOWEL_A_LONG);
    public static final Phoneme P_a1 = new Phoneme("a1", "a1", "a", PhoneticSound.OPEN_FRONT_UNROUNDED_VOWEL_a);
    public static final Phoneme P_a1_LONG = new Phoneme("a1:", "a1:", "a", PhoneticSound.OPEN_FRONT_UNROUNDED_VOWEL_a_LONG);

    public static final Phoneme P_b = new Phoneme("b", "b", "b", PhoneticSound.VOICED_BILABIAL_PLOSIVE_b);
    public static final Phoneme P_c = new Phoneme("c", "c", "c", PhoneticSound.VOICED_POSTALVEOLAR_AFFRICATE_dZ);
    public static final Phoneme P_C = new Phoneme(
            String.valueOf(TurkishAlphabet.C_cc),
            "c1",
            String.valueOf(TurkishAlphabet.C_cc),
            PhoneticSound.VOICELESS_POSTALVEOLAR_AFFRICATE_tS);
    public static final Phoneme P_d = new Phoneme("d", "d", "d", PhoneticSound.VOICED_ALVEOLAR_PLOSIVE_d);
    public static final Phoneme P_e = new Phoneme("e", "e", "e", PhoneticSound.CLOSE_MID_FRONT_UNROUNDED_VOWEL_e);
    public static final Phoneme P_e_LONG = new Phoneme("e:", "e:", "e", PhoneticSound.CLOSE_MID_FRONT_UNROUNDED_VOWEL_e_LONG);
    public static final Phoneme P_e1 = new Phoneme("e1", "e1", "e", PhoneticSound.OPEN_MID_FRONT_UNROUNDED_VOWEL_E);
    public static final Phoneme P_e1_LONG = new Phoneme("e1:", "e1:", "e", PhoneticSound.OPEN_MID_FRONT_UNROUNDED_VOWEL_E_LONG);
    public static final Phoneme P_f = new Phoneme("f", "f", "f", PhoneticSound.VOICELESS_LABIODENTAL_FRICATIVE_f);
    public static final Phoneme P_g = new Phoneme("g", "g", "g", PhoneticSound.VOICED_VELAR_PLOSIVE_g);
    public static final Phoneme P_g1 = new Phoneme("g1", "g1", "g", PhoneticSound.VOICED_PALATAL_PLOSIVE_J_);
    public static final Phoneme P_g4 = new Phoneme(
            String.valueOf(TurkishAlphabet.C_gg),
            "g4",
            String.valueOf(TurkishAlphabet.C_gg),
            PhoneticSound.VOICED_VELAR_FRICATIVE_g);
    public static final Phoneme P_h = new Phoneme("h", "h", "h", PhoneticSound.VOICELESS_GLOTTAL_FRICATIVE_h);
    public static final Phoneme P_I = new Phoneme(
            String.valueOf(TurkishAlphabet.C_ii),
            "i4",
            String.valueOf(TurkishAlphabet.C_ii),
            PhoneticSound.CLOSE_BACK_UNROUNDED_VOWEL_M);
    public static final Phoneme P_I_LONG = new Phoneme(
            String.valueOf(TurkishAlphabet.C_ii + ":"),
            "i4:",
            String.valueOf(TurkishAlphabet.C_ii),
            PhoneticSound.CLOSE_BACK_UNROUNDED_VOWEL_M_LONG);
    public static final Phoneme P_i = new Phoneme("i", "i", "i", PhoneticSound.CLOSE_FRONT_ROUND_UNROUNDED_VOWEL_i);
    public static final Phoneme P_i_LONG = new Phoneme("i:", "i:", "i", PhoneticSound.CLOSE_FRONT_ROUND_UNROUNDED_VOWEL_i_LONG);
    public static final Phoneme P_j = new Phoneme("j", "j", "j", PhoneticSound.VOICED_POSTALVEOLAR_FRICATIVE_Z);
    public static final Phoneme P_k = new Phoneme("k", "k", "k", PhoneticSound.VOICELESS_VELAR_PLOSIVE_k);
    public static final Phoneme P_k1 = new Phoneme("k1", "k1", "k", PhoneticSound.VOICELESS_PALATAL_PLOSIVE_c);
    public static final Phoneme P_l = new Phoneme("l", "l", "l", PhoneticSound.VELARIZED_ALVEOLAR_LATERAL_APPROXIMANT_5);
    public static final Phoneme P_l1 = new Phoneme("l1", "l1", "l", PhoneticSound.ALVEOLAR_LATERAL_APPROXIMANT_l);
    public static final Phoneme P_m = new Phoneme("m", "m", "m", PhoneticSound.BILABIAL_NASAL_m);
    public static final Phoneme P_n = new Phoneme("n", "n", "n", PhoneticSound.ALVEOLAR_NASAL_n);
    public static final Phoneme P_n1 = new Phoneme("n1", "n1", "n", PhoneticSound.VELAR_NASAL_N);
    public static final Phoneme P_o = new Phoneme("o", "o", "o", PhoneticSound.CLOSE_MID_BACK_ROUNDED_VOWEL_o);
    public static final Phoneme P_o_LONG = new Phoneme("o:", "o:", "o", PhoneticSound.CLOSE_MID_BACK_ROUNDED_VOWEL_o_LONG);
    public static final Phoneme P_O = new Phoneme(
            String.valueOf(TurkishAlphabet.C_oo),
            "o4",
            String.valueOf(TurkishAlphabet.C_oo),
            PhoneticSound.CLOSE_MID_FRONT_ROUNDED_VOWEL_2);
    public static final Phoneme P_O_LONG = new Phoneme(
            String.valueOf(TurkishAlphabet.C_oo + ":"),
            "o4",
            String.valueOf(TurkishAlphabet.C_oo),
            PhoneticSound.CLOSE_MID_FRONT_ROUNDED_VOWEL_2_LONG);
    public static final Phoneme P_p = new Phoneme("p", "p", "p", PhoneticSound.VOICELESS_BILABIAL_PLOSIVE_p);
    public static final Phoneme P_r = new Phoneme("r", "r", "r", PhoneticSound.ALVEOLAR_TRILL_r);
    public static final Phoneme P_r1 = new Phoneme("r1", "r1", "r", PhoneticSound.ALVEOLAR_FLAP_4);
    // TODO: Could not find the IPA of r at the end of a word
    public static final Phoneme P_r2 = new Phoneme("r2", "r2", "r", PhoneticSound.ALVEOLAR_FLAP_4);
    public static final Phoneme P_s = new Phoneme("s", "s", "s", PhoneticSound.VOICELESS_ALVEOLAR_FRICATIVE_s);
    public static final Phoneme P_S = new Phoneme(
            String.valueOf(TurkishAlphabet.C_ss),
            "s1",
            String.valueOf(TurkishAlphabet.C_ss),
            PhoneticSound.VOICELESS_POSTALVEOLAR_FRICATIVE_S);
    public static final Phoneme P_t = new Phoneme("t", "t", "t", PhoneticSound.VOICELESS_ALVEOLAR_PLOSIVE_t);
    public static final Phoneme P_u = new Phoneme("u", "u", "u", PhoneticSound.CLOSE_BACK_ROUNDED_VOWEL_u);
    public static final Phoneme P_u_LONG = new Phoneme("u:", "u:", "u", PhoneticSound.CLOSE_BACK_ROUNDED_VOWEL_u_LONG);
    public static final Phoneme P_U = new Phoneme(
            String.valueOf(TurkishAlphabet.C_uu),
            "u4",
            String.valueOf(TurkishAlphabet.C_uu),
            PhoneticSound.CLOSE_FRONT_ROUNDED_VOWEL_y);
    public static final Phoneme P_U_LONG = new Phoneme(
            String.valueOf(TurkishAlphabet.C_uu + ":"),
            "u4:",
            String.valueOf(TurkishAlphabet.C_uu),
            PhoneticSound.CLOSE_FRONT_ROUNDED_VOWEL_y_LONG);
    public static final Phoneme P_v = new Phoneme("v", "v", "v", PhoneticSound.VOICED_LABIODENTAL_FRICATIVE_v);
    public static final Phoneme P_v1 = new Phoneme("v1", "v1", "v", PhoneticSound.VOICED_LABIO_VELAR_APPROXIMANT_w);
    public static final Phoneme P_y = new Phoneme("y", "y", "y", PhoneticSound.PALATAL_APPROXIMANT_j);
    public static final Phoneme P_z = new Phoneme("z", "z", "z", PhoneticSound.VOICED_LABIODENTAL_FRICATIVE_z);
    public static final Phoneme P_z1 = new Phoneme("z1", "z1", "z", PhoneticSound.VOICED_RETROFLEX_FRICATIVE_z_);

    static final public List<Phoneme> phonemes = Arrays.asList(
            P_a, P_a_LONG, P_a1, P_a1_LONG, P_b, P_c, P_C, P_d, P_e, P_e1, P_e1_LONG, P_e_LONG, P_f, P_g, P_g1, P_g4, P_h,
            P_I, P_I_LONG, P_i, P_i_LONG, P_j, P_k, P_k1, P_l, P_l1, P_m, P_n, P_n1, P_o, P_o_LONG, P_O, P_O_LONG,
            P_p, P_r, P_r1, P_r2, P_s, P_S, P_t, P_u, P_u_LONG, P_U, P_U_LONG, P_v, P_v1, P_y, P_z, P_z1);

    static {

        for (Phoneme phoneme : phonemes) {
            phonemeMap.put(phoneme.getId(), phoneme);
            char surfaceForm = phoneme.getSurfaceForm().charAt(0);
            if (!charToPhonemeMap.containsKey(surfaceForm))
                charToPhonemeMap.put(surfaceForm, phoneme);
        }
        phonemeMap.put(Phoneme.PAUSE.id, Phoneme.PAUSE);
        phonemeMap.put("SIL", Phoneme.PAUSE);
        phonemeMap.put("Z", Phoneme.PAUSE);

        // non Turkish to Turkish char conversion
        try {
            Map<String, String> map = new KeyValueReader("=").loadFromStream(
                    IOs.getClassPathResourceAsStream("/resources/tr/phonetics/char-to-turkish-phoneme.txt"), "utf-8");
            for (String key : map.keySet()) {
                char chr = map.get(key).trim().charAt(0);
                char turk = key.trim().charAt(0);
                charToTurkish.put(chr, turk);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean charExists(char c) {
        return charToPhonemeMap.containsKey(c);
    }

    public static Phoneme getPhoneme(String phonemeId) {
        if (!phonemeMap.containsKey(phonemeId)) {
            throw new IllegalArgumentException("Cannot find Phoneme with id:" + phonemeId);
        }
        return phonemeMap.get(phonemeId);
    }

    public static boolean isPhonemeExist(String id) {
        return phonemeMap.containsKey(id);
    }

    public static Phoneme getDefaultPhoneme(char c) {
        if (!charToPhonemeMap.containsKey(c)) {
            throw new IllegalArgumentException("Cannot find Phoneme with id:" + c);
        }
        return charToPhonemeMap.get(c);
    }

    public static List<Phoneme> getDefaultPhonemes(String word) {
        List<Phoneme> phonemes = Lists.newArrayList();
        for (char c : word.toCharArray()) {
            if (!TurkishPhonemes.charExists(c)) // if char does not exist as a phoneme, ignore it
                continue;
            phonemes.add(TurkishPhonemes.getDefaultPhoneme(c));
        }
        return phonemes;
    }

    public static String convertToTurkishChars(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (charToTurkish.containsKey(c))
                sb.append(charToTurkish.get(c));
            else
                sb.append(c);
        }
        return sb.toString();
    }

}

