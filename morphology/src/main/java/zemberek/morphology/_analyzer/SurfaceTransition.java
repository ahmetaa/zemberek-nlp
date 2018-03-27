package zemberek.morphology._analyzer;

import static zemberek.core.turkish.PhoneticAttribute.LastLetterVoiceless;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVowel;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelBack;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelFrontal;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelRounded;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelUnrounded;
import static zemberek.core.turkish.TurkishAlphabet.L_a;
import static zemberek.core.turkish.TurkishAlphabet.L_e;
import static zemberek.core.turkish.TurkishAlphabet.L_i;
import static zemberek.core.turkish.TurkishAlphabet.L_ii;
import static zemberek.core.turkish.TurkishAlphabet.L_u;
import static zemberek.core.turkish.TurkishAlphabet.L_uu;
import static zemberek.morphology.structure.Turkish.Alphabet;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology._morphotactics.AttributeSet;
import zemberek.morphology._morphotactics.Morpheme;
import zemberek.morphology._morphotactics.MorphemeState;
import zemberek.morphology._morphotactics.MorphemeTransition;
import zemberek.morphology._morphotactics.SuffixTransition;

// TODO: find a better name. Move some methods outside.
// not a transition.
public class SurfaceTransition {

  public final String surface;
  // TODO: this can be removed if SearchPath contains StemTransition.
  public final MorphemeTransition lexicalTransition;

  public SurfaceTransition(String surface, MorphemeTransition transition) {
    this.surface = surface;
    this.lexicalTransition = transition;
  }

  public boolean isDerivative() {
    return lexicalTransition.to.derivative;
  }

  public MorphemeState getState() {
    return lexicalTransition.to;
  }

  public Morpheme getMorpheme() {
    return lexicalTransition.to.morpheme;
  }

  public boolean isDerivationalOrRoot() {
    return getState().derivative || getState().posRoot;
  }

  @Override
  public String toString() {
    return surfaceString() + getState().id;
  }

  public String toMorphemeString() {
    return surfaceString() + getState().morpheme.id;
  }

  private String surfaceString() {
    return surface.isEmpty() ? "" : surface + ":";
  }

  public static String generate(
      SuffixTransition transition,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {

    String cached = transition.getFromSurfaceCache(phoneticAttributes);
    if (cached != null) {
      return cached;
    }

    TurkishLetterSequence seq = new TurkishLetterSequence();
    int index = 0;
    for (SuffixTemplateToken token : transition.getTokenList()) {
      AttributeSet<PhoneticAttribute> attrs =
          AttributesHelper.getMorphemicAttributes(seq, phoneticAttributes);
      switch (token.type) {
        case LETTER:
          seq.append(token.letter);
          break;

        case A_WOVEL:
          // TODO: document line below.
          if (index == 0 && phoneticAttributes.contains(LastLetterVowel)) {
            break;
          }
          if (attrs.contains(LastVowelBack)) {
            seq.append(L_a);
          } else if (attrs.contains(LastVowelFrontal)) {
            seq.append(L_e);
          } else {
            throw new IllegalArgumentException("Cannot generate A form!");
          }
          break;

        case I_WOVEL:
          // TODO: document line below. With templates like +Im this would not be necessary
          if (index == 0 && phoneticAttributes.contains(LastLetterVowel)) {
            break;
          }
          if (attrs.contains(LastVowelFrontal) && attrs.contains(LastVowelUnrounded)) {
            seq.append(L_i);
          } else if (attrs.contains(LastVowelBack) && attrs.contains(LastVowelUnrounded)) {
            seq.append(L_ii);
          } else if (attrs.contains(LastVowelBack) && attrs.contains(LastVowelRounded)) {
            seq.append(L_u);
          } else if (attrs.contains(LastVowelFrontal) && attrs.contains(LastVowelRounded)) {
            seq.append(L_uu);
          } else {
            throw new IllegalArgumentException("Cannot generate I form!");
          }
          break;

        case APPEND:
          if (attrs.contains(LastLetterVowel)) {
            seq.append(token.letter);
          }
          break;

        case DEVOICE_FIRST:
          TurkicLetter ld = token.letter;
          if (attrs.contains(LastLetterVoiceless)) {
            ld = Alphabet.devoice(token.letter);
          }
          seq.append(ld);
          break;

        case LAST_VOICED:
        case LAST_NOT_VOICED:
          ld = token.letter;
          seq.append(ld);
          break;
      }
      index++;
    }
    String s = seq.toString();
    transition.addToSurfaceCache(phoneticAttributes, s);
    return s;
  }

  public enum TemplateTokenType {
    I_WOVEL,
    A_WOVEL,
    DEVOICE_FIRST,
    //VOICE_LAST,
    LAST_VOICED,
    LAST_NOT_VOICED,
    APPEND,
    LETTER
  }

  public static class SuffixTemplateToken {

    TemplateTokenType type;
    TurkicLetter letter;
    boolean append = false;

    private SuffixTemplateToken(TemplateTokenType type, TurkicLetter letter) {
      this.type = type;
      this.letter = letter;
    }

    private SuffixTemplateToken(TemplateTokenType type, TurkicLetter letter, boolean append) {
      this.type = type;
      this.letter = letter;
      this.append = append;
    }
  }

  // TODO: consider making templates like "+Im" possible. Also change + syntax to ()
  public static class SuffixTemplateTokenizer implements Iterator<SuffixTemplateToken> {

    private final String generationWord;
    private int pointer;

    public SuffixTemplateTokenizer(String generationWord) {
      this.generationWord = generationWord;
    }

    public boolean hasNext() {
      return generationWord != null && pointer < generationWord.length();
    }

    public SuffixTemplateToken next() {
      if (!hasNext()) {
        throw new NoSuchElementException("no elements left!");
      }
      char c = generationWord.charAt(pointer++);
      char cNext = 0;
      if (pointer < generationWord.length()) {
        cNext = generationWord.charAt(pointer);
      }

      switch (c) {
        case '+':
          pointer++;
          if (cNext == 'I') {
            return new SuffixTemplateToken(TemplateTokenType.I_WOVEL, TurkicLetter.UNDEFINED, true);
          } else if (cNext == 'A') {
            return new SuffixTemplateToken(TemplateTokenType.A_WOVEL, TurkicLetter.UNDEFINED, true);
          } else {
            return new SuffixTemplateToken(TemplateTokenType.APPEND, Alphabet.getLetter(cNext));
          }
        case '>':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.DEVOICE_FIRST,
              Alphabet.getLetter(cNext));
        case '~':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.LAST_VOICED, Alphabet.getLetter(cNext));
        case '!':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.LAST_NOT_VOICED,
              Alphabet.getLetter(cNext));
        case 'I':
          return new SuffixTemplateToken(TemplateTokenType.I_WOVEL, TurkicLetter.UNDEFINED);
        case 'A':
          return new SuffixTemplateToken(TemplateTokenType.A_WOVEL, TurkicLetter.UNDEFINED);
        default:
          return new SuffixTemplateToken(TemplateTokenType.LETTER, Alphabet.getLetter(c));

      }
    }

    public void remove() {
    }
  }
}
