package zemberek.core.turkish;


import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

public enum PhoneticAttribute implements StringEnum {
    LastLetterVowel("LLV"),
    LastLetterConsonant("LLC"),

    LastVowelFrontal("LVF"),
    LastVowelBack("LVB"),
    LastVowelRounded("LVR"),
    LastVowelUnrounded("LVuR"),

    LastLetterVoiceless("LLVless"),
    LastLetterNotVoiceless("LLNotVless"),

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
