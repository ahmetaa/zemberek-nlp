package zemberek.dependency;

import java.util.HashMap;
import java.util.Map;

public enum CoarsePosTag {
    Noun, Postp, Num, Dup, Det, Adv, Zero, Verb, Interj, Ques, Punc, Pron, Conj, Adj, Undefined;

    private static Map<String, CoarsePosTag> mapz = new HashMap<String, CoarsePosTag>();

    static {
        for (CoarsePosTag tag : CoarsePosTag.values()) {
            mapz.put(tag.name(), tag);
        }
    }

    public static CoarsePosTag getFromName(String name) {
        if (name.equals("_"))
            return Undefined;
        return mapz.get(name);
    }

    public String getAsConnlValue() {
        if (this == Undefined)
            return "_";
        else return name();
    }

}
