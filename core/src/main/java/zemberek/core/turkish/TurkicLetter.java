package zemberek.core.turkish;

/**
 * This is a Letter which contains Turkic language specific attributes, such as vowel type,
 * englishEquivalent characters.
 */
public class TurkicLetter {

  public static final TurkicLetter UNDEFINED = new TurkicLetter((char) 0);
  public final char charValue;
  public final boolean vowel;
  public final boolean frontal;
  public final boolean rounded;
  public final boolean voiceless;
  public final boolean continuant;

  private TurkicLetter(Builder builder) {
    this.charValue = builder._charValue;
    this.vowel = builder._vowel;
    this.frontal = builder._frontalVowel;
    this.rounded = builder._roundedVowel;
    this.voiceless = builder._voiceless;
    this.continuant = builder._continuant;
  }

  public TurkicLetter(
      char charValue,
      boolean vowel,
      boolean frontal,
      boolean rounded,
      boolean voiceless,
      boolean continuant) {
    this.charValue = charValue;
    this.vowel = vowel;
    this.frontal = frontal;
    this.rounded = rounded;
    this.voiceless = voiceless;
    this.continuant = continuant;
  }

  // only used for illegal letter.
  private TurkicLetter(char c) {
    this.charValue = c;
    vowel = false;
    frontal = false;
    rounded = false;
    voiceless = false;
    continuant = false;
  }

  public static Builder builder(char charValue) {
    return new Builder(charValue);
  }

  public char charValue() {
    return charValue;
  }

  public boolean isVowel() {
    return vowel;
  }

  public boolean isConsonant() {
    return !vowel;
  }

  public boolean isFrontal() {
    return frontal;
  }

  public boolean isRounded() {
    return rounded;
  }

  public boolean isVoiceless() {
    return voiceless;
  }

  public boolean isStopConsonant() {
    return voiceless && !continuant;
  }

  @Override
  public String toString() {
    return String.valueOf(charValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TurkicLetter that = (TurkicLetter) o;

    if (charValue != that.charValue) {
      return false;
    }

    return true;
  }

  TurkicLetter copyFor(char c) {
    return new TurkicLetter(c, vowel, frontal, rounded, voiceless, continuant);
  }

  @Override
  public int hashCode() {
    return (int) charValue;
  }

  public static class Builder {

    private char _charValue;
    private boolean _vowel = false;
    private boolean _frontalVowel = false;
    private boolean _roundedVowel = false;
    private boolean _voiceless = false;
    private boolean _continuant = false;

    public Builder(char charValue) {
      this._charValue = charValue;
    }

    public Builder vowel() {
      this._vowel = true;
      return this;
    }

    public Builder frontalVowel() {
      this._frontalVowel = true;
      return this;
    }

    public Builder roundedVowel() {
      this._roundedVowel = true;
      return this;
    }

    public Builder voiceless() {
      this._voiceless = true;
      return this;
    }

    public Builder continuant() {
      this._continuant = true;
      return this;
    }

    public TurkicLetter build() {
      if (((_voiceless || _continuant) && _vowel) || (!_vowel && (_frontalVowel
          || _roundedVowel))) {
        throw new IllegalArgumentException(
            "Letter seems to have both vowel and Consonant attributes");
      }
      return new TurkicLetter(this);
    }
  }
}
