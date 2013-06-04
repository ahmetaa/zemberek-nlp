package zemberek.morphology.phonetics;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * A subset of IPA sounds
 * Long vowels are marked as ..._LONG
 */
public enum PhoneticSound {
    OPEN_BACK_UNROUNDED_VOWEL_A("\u0251", "A", true),
    OPEN_BACK_UNROUNDED_VOWEL_A_LONG("\u0251\u02d0", "A:", true),
    OPEN_FRONT_UNROUNDED_VOWEL_a("a", "a", true),
    OPEN_FRONT_UNROUNDED_VOWEL_a_LONG("a\u02d0", "a:", true),
    VOICED_BILABIAL_PLOSIVE_b("b", "b", false),
    VOICED_POSTALVEOLAR_AFFRICATE_dZ("\u02A4", "dZ", false),
    VOICELESS_POSTALVEOLAR_AFFRICATE_tS("\u02A7", "tS", false),
    VOICED_ALVEOLAR_PLOSIVE_d("d", "d", false),
    CLOSE_MID_FRONT_UNROUNDED_VOWEL_e("e", "e", true),
    CLOSE_MID_FRONT_UNROUNDED_VOWEL_e_LONG("e\u02d0", "e:", true),
    OPEN_MID_FRONT_UNROUNDED_VOWEL_E("\u025B", "E", true),
    OPEN_MID_FRONT_UNROUNDED_VOWEL_E_LONG("\u025B\u02d0", "E:", true),
    VOICELESS_LABIODENTAL_FRICATIVE_f("f", "f", false),
    VOICED_VELAR_PLOSIVE_g("\u0261", "g", false),
    VOICED_VELAR_FRICATIVE_g("\u0263", "G", false),
    VOICED_PALATAL_PLOSIVE_J_("\u025F", "J\\", false),
    VOICELESS_GLOTTAL_FRICATIVE_h("h", "h", false),
    CLOSE_BACK_UNROUNDED_VOWEL_M("\u026F", "M", true),
    CLOSE_BACK_UNROUNDED_VOWEL_M_LONG("\u026F\u02d0", "M:", true),
    CLOSE_FRONT_ROUND_UNROUNDED_VOWEL_i("i", "i", true),
    CLOSE_FRONT_ROUND_UNROUNDED_VOWEL_i_LONG("i\u02d0", "i:", true),
    VOICED_POSTALVEOLAR_FRICATIVE_Z("\u0292", "Z", false),
    VOICELESS_VELAR_PLOSIVE_k("k", "k", false),
    VOICELESS_PALATAL_PLOSIVE_c("c", "c", false),
    VELARIZED_ALVEOLAR_LATERAL_APPROXIMANT_5("\u026b", "5", false),
    ALVEOLAR_LATERAL_APPROXIMANT_l("l", "l", false),
    BILABIAL_NASAL_m("m", "m", false),
    ALVEOLAR_NASAL_n("n", "n", false),
    VELAR_NASAL_N("\u014b", "N", false),
    CLOSE_MID_BACK_ROUNDED_VOWEL_o("o", "o", true),
    CLOSE_MID_BACK_ROUNDED_VOWEL_o_LONG("o\u02d0", "o:", true),
    CLOSE_MID_FRONT_ROUNDED_VOWEL_2("\u00f8", "2", true),
    CLOSE_MID_FRONT_ROUNDED_VOWEL_2_LONG("\u00f8\u02d0", "2:", true),
    VOICELESS_BILABIAL_PLOSIVE_p("p", "p", false),
    ALVEOLAR_TRILL_r("r", "r", false),
    ALVEOLAR_FLAP_4("\u027e", "4", false),
    VOICELESS_ALVEOLAR_FRICATIVE_s("s", "s", false),
    VOICELESS_POSTALVEOLAR_FRICATIVE_S("\u0283", "S", false),
    VOICELESS_ALVEOLAR_PLOSIVE_t("t", "t", false),
    CLOSE_BACK_ROUNDED_VOWEL_u("u", "u", true),
    CLOSE_BACK_ROUNDED_VOWEL_u_LONG("u\u02d0", "u:", true),
    CLOSE_FRONT_ROUNDED_VOWEL_y("y", "y", true),
    CLOSE_FRONT_ROUNDED_VOWEL_y_LONG("y", "y", true),
    VOICED_LABIODENTAL_FRICATIVE_v("v", "v", false),
    VOICED_LABIO_VELAR_APPROXIMANT_w("w", "w", false),
    PALATAL_APPROXIMANT_j("j", "j", false),
    VOICED_LABIODENTAL_FRICATIVE_z("z", "z", false),
    VOICED_RETROFLEX_FRICATIVE_z_("\u0290", "z`", false),

    UNDEFINED("", "", false);

    public String unicode;
    public String xSampa;
    public boolean vowel;

    private static Map<String, PhoneticSound> xSampaToPhoneticSound = Maps.newHashMap();
    private static Map<String, PhoneticSound> unicodeToPhoneticSound = Maps.newHashMap();

    static {
        for (PhoneticSound phoneticSound : PhoneticSound.values()) {
            xSampaToPhoneticSound.put(phoneticSound.xSampa, phoneticSound);
            unicodeToPhoneticSound.put(phoneticSound.unicode, phoneticSound);
        }
    }

    PhoneticSound(String unicode, String xSampa, boolean vowel) {
        this.unicode = unicode;
        this.xSampa = xSampa;
        this.vowel = vowel;
    }

    public static PhoneticSound getByXSampa(String xSampa) {
        if (xSampaToPhoneticSound.containsKey(xSampa))
            return xSampaToPhoneticSound.get(xSampa);
        throw new IllegalArgumentException("PhoneticSound for XSAMPA symbol cannot be found. Symbol:" + xSampa);
    }

    public static PhoneticSound getByUnicode(String unicode) {
        if (unicodeToPhoneticSound.containsKey(unicode))
            return unicodeToPhoneticSound.get(unicode);
        throw new IllegalArgumentException("PhoneticSound for unicode symbol cannot be found. Symbol:" + unicode);
    }



}

