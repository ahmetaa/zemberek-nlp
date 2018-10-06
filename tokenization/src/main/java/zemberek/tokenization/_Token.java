package zemberek.tokenization;

class _Token {

  public final String content;
  public final String normalized;
  public final Type type;
  public final int start;
  public final int end;

  public _Token(String content, String normalized, Type type, int start, int end) {
    this.content = content;
    this.normalized = normalized;
    this.type = type;
    this.start = start;
    this.end = end;
  }

  boolean isNumeral() {
    return type == Type.CardinalNumber ||
        type == Type.RealNumber ||
        type == Type.Range ||
        type == Type.Ratio ||
        type == Type.OrdinalNumber ||
        type == Type.RomanNumber ||
        type == Type.Distribution ||
        type == Type.Percent;
  }

  boolean isWhiteSpace() {
    return type == Type.SpaceTab ||
        type == Type.NewLine;
  }

  boolean isWebRelated() {
    return
        type == Type.HashTag ||
            type == Type.Mention ||
            type == Type.URL ||
            type == Type.MetaTag ||
            type == Type.Email;
  }

  boolean isEmoji() {
    return type == Type.Emoji || type == Type.Emoticon;
  }

  boolean isUnidentified() {
    return type == Type.Unknown || type == Type.UnknownWord;
  }

  boolean isWord() {
    return type == Type.Word ||
        type == Type.Abbreviation;
  }


  enum Type {

    // white space
    SpaceTab,
    NewLine,

    // words
    Word,
    Abbreviation,

    Punctuation,

    // numerals. May contain suffixes.
    RomanNumber,
    Ratio,
    Range,
    RealNumber,
    Distribution,
    OrdinalNumber,
    CardinalNumber,
    Percent,

    // temporal
    Time,
    Date,

    // web related
    URL,
    Email,
    HashTag,
    Mention,
    MetaTag,

    Emoji,
    Emoticon,

    UnknownWord,
    Unknown,
  }
}
