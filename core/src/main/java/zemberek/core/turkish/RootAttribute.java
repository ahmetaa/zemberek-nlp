package zemberek.core.turkish;

import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;

/**
 * These represents attributes of roots.
 */
public enum RootAttribute implements StringEnum {

  // Generally Present tense (Aorist) suffix has the form [Ir]; such as gel-ir, bul-ur, kapat-ır.
  // But for most verbs with single syllable and compound verbs it forms as [Ar].
  // Such as yap-ar, yet-er, hapsed-er. There are exceptions for this case, such as "var-ır".
  // Below two represents the attributes for clearing the ambiguity. These attributes does not
  // modify the root form.
  Aorist_I,
  Aorist_A,

  // If a verb ends with a vowel and Progressive suffix [Iyor] appended, last vowel of the root
  // form drops. Such as "ara → ar-ıyor" "ye → y-iyor".
  // This also applies to suffixes, such as Negative "mA" suffix. "yap-ma → yap-m-ıyor".
  // But suffix case is handled during graph generation.
  //
  // This attribute is added automatically.
  // TODO: This may be combined with LastVowelDrop or changed as LastLetterDrop.
  ProgressiveVowelDrop,

  // For verbs that ends with a vowel or letter "l" Passive voice suffix fors as [+In] and [+InIl].
  // ara-n, ara-nıl and "sarıl-ın-an".
  // For other verbs [+nIl] is used. Such as ser-il, yap-ıl, otur-ul.
  //
  // This attribute is added automatically.
  // TODO: [+nIl] may be changed to [+Il]
  Passive_In,

  // For verbs that has more than one syllable and end with a vowel or letters "l" or "r",
  // Causative suffix form as [t]. Such as: ara-t, oku-t, getir-t, doğrul-t, bağır-t
  // Otherwise it forms as [tIr]. Such as: ye-dir, sat-tır, dol-dur, seyret-tir
  //
  // This attribute is added automatically.
  Causative_t,


  // If last letter of a word or suffix is a stop consonant (tr: süreksiz sert sessiz), and a
  // suffix that starts with a vowel is appended to that word, last letter changes.
  // This is called voicing. Changes are p-b, ç-c, k-ğ, t-d, g-ğ.
  // Such as kitap → kitab-a, pabuç → pabuc-u, cocuk → cocuğ-a, hasat → hasad-ı
  //
  // It also applies to some verbs: et→ed-ecek. But for verb roots, only ‘t’ endings are voiced.
  // And most suffixes: elma-cık→elma-cığ-ı, yap-acak→yap-acağ-ım.
  //
  // When a word ends with ‘nk‘, then ‘k’ changes to ‘g’ instead of ‘ğ’.
  // Such as cenk → ceng-e, çelenk → çeleng-i
  //
  // For some loan words, g-ğ change occurs. psikolog → psikoloğ-a
  //
  // Usually if the word has only one syllable, rule does not apply.
  // Such as turp → turp-u, kat → kat-a, kek → kek-e, küp → küp-üm.
  // But this rule has some exceptions as well: harp → harb-e
  //
  // Some multi syllable words also do not obey this rule.
  // Such as taksirat → taksirat-ı, kapat → kapat-ın
  Voicing,

  // NoVoicing attribute is only used for explicitly marking a word in the dictionary
  // that should not have automatic Voicing attribute. So after a DictionaryItem is created
  // only checking Voicing attribute is enough.
  NoVoicing,

  // For some loan words, suffix vowel harmony rules does not apply. This usually happens in some
  // loan words. Such as saat-ler and alkol-ü
  InverseHarmony,

  // When a suffix that starts with a vowel is added to some words, last letter is doubled.
  // Such as hat → hat-tı
  //
  // If last letter is also changed by the appended suffix, transformed letter is repeated.
  // Such as ret → red-di
  Doubling,

  // Last vowel before the last consonant drops in some words when a suffix starting with a vowel
  // is appended.
  // ağız → ağz-a, burun → burn-um, zehir → zehr-e.
  //
  // Some words have this property optionally. Both omuz → omuz-a, omz-a are valid. Sometimes
  // different meaning of the words effect the outcome such as oğul-u and oğl-u. In first case
  // "oğul" means "group of bees", second means "son".
  //
  // Some verbs obeys this rule. kavur → kavr-ul. But it only happens for passive suffix.
  // It does not apply to other suffixes. Such as kavur→kavur-acak, not kavur-kavracak
  //
  // In spoken conversation, some vowels are dropped too but those are grammatically incorrect.
  // Such as içeri → içeri-de (not ‘içerde’), dışarı → dışarı-da (not ‘dışarda’)
  //
  // When a vowel is dropped, the form of the suffix to be appended is determined by the original
  // form of the word, not the form after vowel is dropped.
  // Such as nakit → nakd-e, lütuf → lütf-un.
  //
  // If we were to apply the vowel harmony rule after the vowel is dropped,
  // it would be nakit → nakd-a and lütuf → lütf-ün, which are not correct.
  LastVowelDrop,

  // This is for marking compound words that ends with third person possesive  suffix P3sg [+sI].
  // Such as aşevi, balkabağı, zeytinyağı.
  //
  // These compound words already contains a suffix so their handling is different than other
  // words. For example some suffixes changes the for of the root.
  // Such as zeytinyağı → zeytinyağ-lar-ı atkuyruğu → atkuyruklu
  CompoundP3sg,

  // No suffix can be appended to this.
  // TODO: this is not yet used. But some words are marked in dictionary.
  NoSuffix,

  // Some Compound words adds `n` instead of `y` when used with some suffixes. Such as `Boğaziçi-ne` not `Boğaziçi-ye`
  // TODO: this is not yet used. But some words are marked in dictionary.
  NounConsInsert_n,

  // This attribute is used for formatting a word. If this is used, when a suffix is added to a Proper noun, no single
  // quote is used as a separator. Such as "Türkçenin" not "Türkçe'nin"
  NoQuote,

  // Some compound nouns cannot be used in root form. For example zeytinyağı -> zeytinyağ. For preventing
  // false positives this attribute is added to the zeytinyağ form of the word. So that representing state cannot
  // be terminal.
  // This is added automatically.
  CompoundP3sgRoot,

  // This is for marking reflexive verbs. Reflexive suffix [+In] can only be added to some verbs.
  // TODO: This is defined but not used in morphotactics.
  Reflexive,

  // This is for marking reflexive verbs. Reciprocal suffix [+Iş, +yIş] can only be added to some
  // verbs.
  // TODO: Reciprocal suffix is commented out in morphotactics and reciprocal verbs are added with suffixes.
  // Such as boğuşmak [A:Reciprocal]
  Reciprocal,
  // if a verb cannot be reciprocal.
  NonReciprocal,

  // for items that are not in official TDK dictionary
  Ext,

  // for items that are added to system during runtime
  Runtime,

  //For dummy items. Those are created when processing compound items.
  Dummy,

  // -------------- Experimental attributes.
  ImplicitDative,

  // It contains plural meaning implicitly so adding an external plural suffix is erroneous.
  // This usually applies to arabic loan words. Such as ulema, hayvanat et.
  ImplicitPlural,
  ImplicitP1sg,
  ImplicitP2sg,
  FamilyMember, // annemler etc.
  PronunciationGuessed,

  // This means word is only used in informal language.
  // Some applications may want to analyze them with a given informal dictionary.
  // Examples: kanka, beyfendi, mütahit, antreman, bilimum, gaste, aliminyum, tırt, tweet
  Informal,

  // This is used for words that requires English rules when applying lowercasing and uppercasing.
  // This way, words like "UNICEF" will be lowercased as "unicef", not "unıcef"
  LocaleEn,

  // This is used for temporary DictionaryItems created for words that cannot be analyzed.
  Unknown;

  private static StringEnumMap<RootAttribute> shortFormToPosMap = StringEnumMap
      .get(RootAttribute.class);
  int index;

  RootAttribute() {
    this.index = this.ordinal();
  }

  public static StringEnumMap<RootAttribute> converter() {
    return shortFormToPosMap;
  }

  public String getStringForm() {
    return this.name();
  }
}
