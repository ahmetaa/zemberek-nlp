package zemberek.dependency;

import java.util.HashMap;
import java.util.Map;

public enum PosTag {
    Noun,
    DemonsP,
    Dup,
    Det,
    Adv,
    Zero,
    Verb,
    Interj,
    Ques,
    APastPart,
    NPastPart,
    APresPart,
    NPresPart,
    AFutPart,
    NFutPart,
    Ord,
    PersP,
    Prop,
    Pron,
    Conj,
    Adj,
    Distrib,
    Postp,
    Range,
    Num,
    ReflexP,
    Real,
    QuesP,
    Punc,
    NInf,
    Card,
    Undefined;

    String[] optionalNames;

    PosTag(String... optionalNames) {
        this.optionalNames = optionalNames;
    }

    private static Map<String, PosTag> mapz = new HashMap<String, PosTag>();

    static {
        for (PosTag tag : PosTag.values()) {
            mapz.put(tag.name(), tag);
        }
    }

    public static PosTag getFromName(String name) {
        if (name.equals("_"))
            return Undefined;
        return mapz.get(name);
    }

    public static boolean exists(String s) {
        return mapz.containsKey(s);
    }

    public String getAsConnlValue() {
        if (this == Undefined)
            return "_";
        else return name();
    }
}
