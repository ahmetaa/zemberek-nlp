package zemberek.core.turkish;


import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

public enum SecondaryPos implements StringEnum {
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
    Date("Date"),

    // Below POS information is for Oflazer compatibility.
    // They indicate that words before Post positive words should end with certain suffixes.
    PCDat("PCDat"),
    PCAcc("PCAcc"),
    PCIns("PCIns"),
    PCNom("PCNom"),
    PCGen("PCGen"),
    PCAbl("PCAbl");


    public String shortForm;

    SecondaryPos(String shortForm) {
        this.shortForm = shortForm;
    }

    private static StringEnumMap<SecondaryPos> shortFormToPosMap = StringEnumMap.get(SecondaryPos.class);

    public static StringEnumMap<SecondaryPos> converter() {
        return shortFormToPosMap;
    }

    public static boolean exists(String stringForm) {
        return shortFormToPosMap.enumExists(stringForm);
    }

    public String getStringForm() {
        return shortForm;
    }
}
