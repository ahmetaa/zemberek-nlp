package zemberek.core.turkish;

/**
 * This is a Letter which contains Turkic language specific attributes, such as vowel type, englishEquivalent characters.
 */
public class TurkicLetter {

    public final char charValue;
    public final int alphabeticIndex;
    public final boolean vowel;
    public final boolean frontal;
    public final boolean rounded;
    public final boolean voiceless;
    public final boolean continuant;
    public final boolean inAscii;
    public final boolean foreign;
    public final char englishEquivalentChar;

    public static final TurkicLetter UNDEFINED = new TurkicLetter((char) 0, -1);

    public static Builder builder(char charValue, int alphabeticIndex) {
        return new Builder(charValue).alphabeticIndex(alphabeticIndex);
    }

    public static class Builder {
        private char _charValue = 0;
        private int _alphabeticIndex = -1;
        private boolean _vowel = false;
        private boolean _frontalVowel = false;
        private boolean _roundedVowel = false;
        private boolean _voiceless = false;
        private boolean _continuant = false;
        private boolean _inAscii = true;
        private boolean _foreign = false;
        private char _englishEquivalentChar = 0;

        public Builder(char charValue) {
            this._charValue = charValue;
            this._englishEquivalentChar = charValue;
        }

        public Builder alphabeticIndex(int alphabeticIndex) {
            this._alphabeticIndex = alphabeticIndex;
            return this;
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

        public Builder notInAscii() {
            this._inAscii = false;
            return this;
        }

        public Builder foreign() {
            this._foreign = true;
            return this;
        }

        public Builder similarAscii(char equivalent) {
            this._englishEquivalentChar = equivalent;
            return this;
        }

        public TurkicLetter build() {
            if (((_voiceless || _continuant) && _vowel) || (!_vowel && (_frontalVowel || _roundedVowel))) {
                throw new IllegalArgumentException("Letter seems to have both vowel and Consonant attributes");
            } else if ((!_inAscii) && (_charValue < 'a' && _charValue > 'z')) {
                throw new IllegalArgumentException("Marked as english alphabet but it is not." + _charValue);
            } else if (_alphabeticIndex < 0) {
                throw new IllegalArgumentException("Alphabetical index must be positive:" + _alphabeticIndex);
            }

            if ((!_inAscii) && (_charValue < 'a' && _charValue > 'z')) ;

            TurkicLetter tl = new TurkicLetter(this);
            return tl;
        }
    }

    private TurkicLetter(Builder builder) {
        this.charValue = builder._charValue;
        this.alphabeticIndex = builder._alphabeticIndex;
        this.vowel = builder._vowel;
        this.frontal = builder._frontalVowel;
        this.rounded = builder._roundedVowel;
        this.voiceless = builder._voiceless;
        this.continuant = builder._continuant;
        this.inAscii = builder._inAscii;
        this.foreign = builder._foreign;
        this.englishEquivalentChar = builder._englishEquivalentChar;
    }

    // only used for illegal letter.
    private TurkicLetter(char c, int alphabeticIndex) {
        this.charValue = c;
        this.alphabeticIndex = alphabeticIndex;
        vowel = false;
        frontal = false;
        rounded = false;
        voiceless = false;
        continuant = false;
        inAscii = false;
        foreign = false;
        englishEquivalentChar = c;
    }

    public char charValue() {
        return charValue;
    }

    public int alphabeticIndex() {
        return alphabeticIndex;
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

    public boolean isContinuant() {
        return continuant;
    }

    public boolean isInAscii() {
        return inAscii;
    }

    public char englishEquivalentChar() {
        return englishEquivalentChar;
    }

    public boolean isStopConsonant() {
        return voiceless && !continuant;
    }

    @Override
    public String toString() {
        return String.valueOf(charValue + ":" + englishEquivalentChar);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TurkicLetter that = (TurkicLetter) o;

        if (charValue != that.charValue) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) charValue;
    }
}
