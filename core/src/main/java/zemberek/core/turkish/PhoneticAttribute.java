package zemberek.core.turkish;


import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

public enum PhoneticAttribute implements StringEnum {

    // Turkish vowels are: [a, e, ı, i, o, ö, u, ü]
    LastLetterVowel("LLV"),

    // Turkish consonants are: [b, c, ç, d, f, g, ğ, h, j, k, l, m, n, p, r, s, ş, t, v, y, z]
    LastLetterConsonant("LLC"),

    // Turkish Frontal vowels are: [e, i, ö, ü]
    LastVowelFrontal("LVF"),

    // Back vowels are: [a, ı, o, u]
    LastVowelBack("LVB"),

    // Rounded wovels are: [o, u, ö, ü]
    LastVowelRounded("LVR"),

    // Unrounded wovels are: [a, e, ı, i]
    LastVowelUnrounded("LVuR"),

    // Turkish voiceless consonants are [ç, f, h, k, p, s, ş, t]
    LastLetterVoiceless("LLVless"),

    // Turkish voiced consonants are [b, c, d, g, ğ, h, j, l, m, n, r, v, y, z]
    LastLetterNotVoiceless("LLNotVless"),

    // Turkish Voiceless stop consonants are: [ç, k, p, t]. Voiced stop consonants are [b, c, d, g, ğ]
    // TODO: short form should be LLVlessStop
    LastLetterVoicelessStop("LLStop"),

    FirstLetterVowel("FLV"),
    FirstLetterConsonant("FLC"),

    HasNoVowel("NoVow");

    private final static StringEnumMap<PhoneticAttribute> shortFormToPosMap = StringEnumMap.get(PhoneticAttribute.class);

    private final String shortForm;

    private PhoneticAttribute(String shortForm) {
        this.shortForm = shortForm;
    }

    @Override
    public String getStringForm() {
        return shortForm;
    }

    public static StringEnumMap<PhoneticAttribute> converter() {
        return shortFormToPosMap;
    }
}
