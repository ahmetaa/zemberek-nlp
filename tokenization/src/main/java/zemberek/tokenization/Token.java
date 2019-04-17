package zemberek.tokenization;

public class Token {

  public final String content;
  public final String normalized;
  public final Type type;
  public final int start;
  public final int end;

  public Token(String content, String normalized, Type type, int start, int end) {
    this.content = content;
    this.normalized = normalized;
    this.type = type;
    this.start = start;
    this.end = end;
  }

  public Token(String content, Type type, int start, int end) {
    this.content = content;
    this.normalized = content;
    this.type = type;
    this.start = start;
    this.end = end;
  }

  public Type getType() {
    return type;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  boolean isNumeral() {
    return type == Type.Number ||
        type == Type.RomanNumeral ||
//        type == Type.CardinalNumber ||
//        type == Type.RealNumber ||
//        type == Type.Range ||
//        type == Type.Ratio ||
//        type == Type.Distribution ||
//        type == Type.OrdinalNumber ||
        type == Type.PercentNumeral;
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

  public String getText() {
    return content;
  }

  boolean isEmoji() {
    return type == Type.Emoji || type == Type.Emoticon;
  }

  boolean isUnidentified() {
    return type == Type.Unknown || type == Type.UnknownWord;
  }

  boolean isWord() {
    return type == Type.Word || type == Type.Abbreviation;
  }

  @Override
  public String toString() {
    return "[" + content + " " + type + " " + start + "-" + end + "]";
  }

  public enum Type {

    // white space
    SpaceTab,
    NewLine,

    // words
    Word,
    WordAlphanumerical,
    WordWithSymbol,
    Abbreviation,
    AbbreviationWithDots,

    Punctuation,

    // numerals. May contain suffixes.
    RomanNumeral,
    Number,
    // TODO: in later versions lexer should handle the types below.
    //Ratio,
    //Range,
    //RealNumber,
    //Distribution,
    //OrdinalNumber,
    //CardinalNumber,
    PercentNumeral,

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
