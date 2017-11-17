package zemberek.core.turkish;


import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

public enum SecondaryPos implements StringEnum {
    DemonstrativePron("Demons"),
    Time("Time"),
    QuantitivePron("Quant"),
    QuestionPron("Ques"),
    ProperNoun("Prop"),
    PersonalPron("Pers"),
    ReflexivePron("Reflex"),
    None("None"),
    UnknownSec("Unk"),
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
