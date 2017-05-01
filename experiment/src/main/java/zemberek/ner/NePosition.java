package zemberek.ner;

/**
 * BILOU style NER position information.
 */
enum NePosition {
    BEGIN("B"),
    INSIDE("I"),
    LAST("L"),
    OUTSIDE("O"),
    UNIT("U");

    String shortForm;

    NePosition(String s) {
        this.shortForm = s;
    }

    static NePosition fromString(String s) {
        switch (s) {
            case "B":
                return BEGIN;
            case "I":
                return INSIDE;
            case "L":
                return LAST;
            case "O":
                return OUTSIDE;
            case "U":
                return UNIT;
            default:
                throw new IllegalArgumentException("Unidentified ner position " + s);
        }
    }
}
