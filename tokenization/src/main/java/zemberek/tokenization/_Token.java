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

  enum Type {

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

    // numerals
    RomanNumeral,
    RatioNumeral,
    RangeNumeral,
    RealNumeral,
    DistributionNumeral,
    OrdinalNumeral,
    CardinalNumeral,
    PercentNumeral,

    // temporal
    Time,
    Date,

    // web related
    URL,
    Email,
    HashTag,
    Mention,

    Emoji,
    Emoticon,

    UnknownWord,
    Unknown,
  }
}
