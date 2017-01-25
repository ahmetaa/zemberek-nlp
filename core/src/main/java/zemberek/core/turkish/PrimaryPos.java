package zemberek.core.turkish;

import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

public enum PrimaryPos implements StringEnum {
    Noun("Noun"),
    Adjective("Adj"),
    Adverb("Adv"),
    Conjunction("Conj"),
    Interjection("Interj"),
    Verb("Verb"),
    Pronoun("Pron"),
    Numeral("Num"),
    Determiner("Det"),
    PostPositive("Postp"),
    Question("Ques"),
    Duplicator("Dup"),
    Punctuation("Punc"),
    Unknown("Unk");

    public String shortForm;

    PrimaryPos(String shortForm) {
        this.shortForm = shortForm;
    }

    private final static StringEnumMap<PrimaryPos> shortFormToPosMap = StringEnumMap.get(PrimaryPos.class);

    public static StringEnumMap<PrimaryPos> converter() {
        return shortFormToPosMap;
    }

    public String getStringForm() {
        return shortForm;
    }

    public static boolean exists(String stringForm) {
        return shortFormToPosMap.enumExists(stringForm);
    }
}
