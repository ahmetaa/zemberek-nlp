package zemberek.core.lexicon;


import zemberek.core.structure.StringEnum;
import zemberek.core.structure.StringEnumMap;

public enum SecondaryPos implements StringEnum<SecondaryPos> {
    Demonstrative("Demons"),
    Time("Time"),
    Quantitive("Quant"),
    Question("Ques"),
    ProperNoun("Prop"),
    Personal("Pers"),
    Reflexive("Reflex"),
    None("None"),
    Unknown("Unk"),
    Ordinal("Ord"),
    Cardinal("Card"),
    Percentage("Percent"),
    Ratio("Ratio"),
    Range("Range"),
    Real("Real"),
    Distribution("Dist"),
    Clock("Clock"),
    Date("Date");


    public String shortForm;

    SecondaryPos(String shortForm) {
        this.shortForm = shortForm;
    }

    private static StringEnumMap<SecondaryPos> shortFormToPosMap = StringEnumMap.get(SecondaryPos.class);

    public static StringEnumMap<SecondaryPos> converter() {
        return shortFormToPosMap;
    }

    public String getStringForm() {
        return shortForm;
    }
}
