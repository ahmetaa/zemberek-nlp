package zemberek.core.turkish;


import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

public enum PhoneticAttribute implements StringEnum {

  // Turkish vowels are: [a, e, ı, i, o, ö, u, ü]
  LastLetterVowel("LLV"),

  // Turkish consonants are: [b, c, ç, d, f, g, ğ, h, j, k, l, m, n, p, r, s, ş, t, v, y, z]
  LastLetterConsonant("LLC"),

  // Turkish Frontal vowels are: [e, i, ö, ü]
  LastVowelFrontal("LVF"),

  // Back vowels are: [a, ı, o, u]
  LastVowelBack("LVB"),

  // Rounded vowels are: [o, u, ö, ü]
  LastVowelRounded("LVR"),

  // Unrounded vowels are: [a, e, ı, i]
  LastVowelUnrounded("LVuR"),

  // Turkish voiceless consonants are [ç, f, h, k, p, s, ş, t]
  LastLetterVoiceless("LLVless"),

  // Turkish voiced consonants are [b, c, d, g, ğ, h, j, l, m, n, r, v, y, z]
  LastLetterVoiced("LLVo"),

  // Turkish Voiceless stop consonants are: [ç, k, p, t]. Voiced stop consonants are [b, c, d, g, ğ]
  LastLetterVoicelessStop("LLVlessStop"),

  FirstLetterVowel("FLV"),
  FirstLetterConsonant("FLC"),

  HasNoVowel("NoVow"),

  // ---- experimental -----

  ExpectsVowel("EV"),
  ExpectsConsonant("EC"),
  ModifiedPronoun("MP"), //ben,sen -> ban, san form.
  UnModifiedPronoun("UMP"), //ben,sen -> ben, sen form.

  // for verbs that and with a vowel and to connect `iyor` progressive tense suffix.
  LastLetterDropped("LWD"),
  CannotTerminate("CNT");

  private final static StringEnumMap<PhoneticAttribute> shortFormToPosMap = StringEnumMap
      .get(PhoneticAttribute.class);

  private final String shortForm;

  PhoneticAttribute(String shortForm) {
    this.shortForm = shortForm;
  }

  public static StringEnumMap<PhoneticAttribute> converter() {
    return shortFormToPosMap;
  }

  @Override
  public String getStringForm() {
    return shortForm;
  }
}
