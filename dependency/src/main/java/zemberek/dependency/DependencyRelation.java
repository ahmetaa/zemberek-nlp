package zemberek.dependency;

import java.util.HashMap;
import java.util.Map;

public enum DependencyRelation {
    SUBJECT,
    S_MODIFIER,
    COLLOCATION,
    QUESTION_PARTICLE,
    INSTRUMENTAL_ADJUNCT,
    NEGATIVE_PARTICLE,
    ETOL,
    FOCUS_PARTICLE,
    LOCATIVE_ADJUNCT,
    VOCATIVE,
    DERIV,
    CLASSIFIER,
    ROOT,
    MODIFIER,
    notconnected,
    RELATIVIZER,
    DATIVE_ADJUNCT,
    COORDINATION,
    OBJECT,
    SENTENCE,
    APPOSITION,
    ABLATIVE_ADJUNCT,
    EQU_ADJUNCT,
    DETERMINER,
    INTENSIFIER,
    POSSESSOR,
    UNDEFINED;

    private static Map<String, DependencyRelation> mapz = new HashMap<String, DependencyRelation>();

    static {
        for (DependencyRelation tag : DependencyRelation.values()) {
            mapz.put(tag.name(), tag);
        }
    }

    public static DependencyRelation getFromName(String name) {
        if (name.equals("_"))
            return UNDEFINED;
        return mapz.get(name.replaceAll("[\\.]", "_"));
    }

    String getAsConnlString() {
        if (this == UNDEFINED)
            return "_";
        return name().replaceAll("[_]", ".");
    }
}
