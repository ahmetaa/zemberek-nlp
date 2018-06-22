package zemberek.ner;

/**
 * BILOU style NER position information.
 */
enum NePosition {
  BEGIN("B"),  // beginning token of a NE
  INSIDE("I"), // Inside token of a NE
  LAST("L"),   // Last token of a NE
  OUTSIDE("O"),// Not a NE token
  UNIT("U");   // A single NE token

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
