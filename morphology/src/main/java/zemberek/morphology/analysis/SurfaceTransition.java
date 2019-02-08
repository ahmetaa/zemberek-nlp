package zemberek.morphology.analysis;

import static zemberek.core.turkish.PhoneticAttribute.LastLetterVoiceless;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVowel;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelBack;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelFrontal;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelRounded;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelUnrounded;

import java.util.Iterator;
import java.util.NoSuchElementException;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.SuffixTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

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

  static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  public static String generateSurface(
      SuffixTransition transition,
      AttributeSet<PhoneticAttribute> phoneticAttributes) {

    String cached = transition.getFromSurfaceCache(phoneticAttributes);
    if (cached != null) {
      return cached;
    }

    StringBuilder sb = new StringBuilder();
    int index = 0;
    for (SuffixTemplateToken token : transition.getTokenList()) {
      AttributeSet<PhoneticAttribute> attrs =
          AttributesHelper.getMorphemicAttributes(sb, phoneticAttributes);
      switch (token.type) {
        case LETTER:
          sb.append(token.letter);
          break;

        case A_WOVEL:
          // TODO: document line below.
          if (index == 0 && phoneticAttributes.contains(LastLetterVowel)) {
            break;
          }
          if (attrs.contains(LastVowelBack)) {
            sb.append('a');
          } else if (attrs.contains(LastVowelFrontal)) {
            sb.append('e');
          } else {
            throw new IllegalArgumentException("Cannot generate A form! ");
          }
          break;

        case I_WOVEL:
          // TODO: document line below. With templates like +Im this would not be necessary
          if (index == 0 && phoneticAttributes.contains(LastLetterVowel)) {
            break;
          }
          if (attrs.contains(LastVowelFrontal) && attrs.contains(LastVowelUnrounded)) {
            sb.append('i');
          } else if (attrs.contains(LastVowelBack) && attrs.contains(LastVowelUnrounded)) {
            sb.append('ı');
          } else if (attrs.contains(LastVowelBack) && attrs.contains(LastVowelRounded)) {
            sb.append('u');
          } else if (attrs.contains(LastVowelFrontal) && attrs.contains(LastVowelRounded)) {
            sb.append('ü');
          } else {
            throw new IllegalArgumentException("Cannot generate I form!");
          }
          break;

        case APPEND:
          if (attrs.contains(LastLetterVowel)) {
            sb.append(token.letter);
          }
          break;

        case DEVOICE_FIRST:
          char ld = token.letter;
          if (attrs.contains(LastLetterVoiceless)) {
            ld = alphabet.devoice(ld);
          }
          sb.append(ld);
          break;

        case LAST_VOICED:
        case LAST_NOT_VOICED:
          ld = token.letter;
          sb.append(ld);
          break;
      }
      index++;
    }
    String s = sb.toString();
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
    char letter;
    boolean append = false;

    private SuffixTemplateToken(TemplateTokenType type, char letter) {
      this.type = type;
      this.letter = letter;
    }

    private SuffixTemplateToken(TemplateTokenType type, char letter, boolean append) {
      this.type = type;
      this.letter = letter;
      this.append = append;
    }

    public TemplateTokenType getType() {
      return type;
    }

    public char getLetter() {
      return letter;
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

      char undefined = (char) 0;
      switch (c) {
        case '+':
          pointer++;
          if (cNext == 'I') {
            return new SuffixTemplateToken(TemplateTokenType.I_WOVEL, undefined, true);
          } else if (cNext == 'A') {
            return new SuffixTemplateToken(TemplateTokenType.A_WOVEL, undefined, true);
          } else {
            return new SuffixTemplateToken(TemplateTokenType.APPEND, cNext);
          }
        case '>':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.DEVOICE_FIRST, cNext);
        case '~':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.LAST_VOICED, cNext);
        case '!':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.LAST_NOT_VOICED, cNext);
        case 'I':
          return new SuffixTemplateToken(TemplateTokenType.I_WOVEL, undefined);
        case 'A':
          return new SuffixTemplateToken(TemplateTokenType.A_WOVEL, undefined);
        default:
          return new SuffixTemplateToken(TemplateTokenType.LETTER, c);

      }
    }

    public void remove() {
    }
  }
}
