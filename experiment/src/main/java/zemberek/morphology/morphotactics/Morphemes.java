package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PrimaryPos;

public class Morphemes {

  public static final Morpheme root = new Morpheme("Root", "Root");

  public static final Morpheme noun = new Morpheme("Noun", "Noun", PrimaryPos.Noun);

  public static final Morpheme adj = new Morpheme("Adjective", "Adj", PrimaryPos.Adjective);

  public static final Morpheme verb = new Morpheme("Verb", "Verb", PrimaryPos.Verb);

  public static final Morpheme pron = new Morpheme("Pronoun", "Pron", PrimaryPos.Pronoun);

  // Number-Person agreement.

  public static final Morpheme a1sg = new Morpheme("FirstPersonSingular", "A1sg");
  // Third person singular suffix. "elma = apple"
  public static final Morpheme a3sg = new Morpheme("ThirdPersonSingular", "A3sg");
  // Third person plural suffix. "elma-lar = apples"
  public static final Morpheme a3pl = new Morpheme("ThirdPersonPlural", "A3pl");

  // Possessive

  // No possession suffix. This is not a real Morpheme but adds information to analysis. "elma = apple"
  public static final Morpheme pnon = new Morpheme("NoPosession", "Pnon");
  // First person singular possession suffix.  "elma-m = my apple"
  public static final Morpheme p1sg = new Morpheme("FirstPersonSingularPosessive", "P1sg");
  // Third person singular possession suffix. "elma-sı = his/her apple"
  public static final Morpheme p3sg = new Morpheme("ThirdPersonSingularPossesive", "P3sg");

  // Case suffixes

  // Nominal case suffix. It has no surface form (no letters). "elma = apple"
  public static final Morpheme nom = new Morpheme("Nominal", "Nom");
  // Dative case suffix. "elmaya = to apple"
  public static final Morpheme dat = new Morpheme("Dative", "Dat");
  // Accusative case suffix. "elmayı = ~the apple"
  public static final Morpheme acc = new Morpheme("Accusative", "Acc");

  // Derivation suffixes

  // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
  public static final Morpheme dim = new Morpheme("Diminutive", "Dim");
  // With suffix. Noun to Adjective conversion. "elmalı = with apple"
  public static final Morpheme with = new Morpheme("With", "With");

  public static final Morpheme justLike = new Morpheme("JustLike", "JustLike");

  // Zero derivation
  public static final Morpheme zero = new Morpheme("Zero", "Zero");


  // Present Tense
  public static final Morpheme pres = new Morpheme("PresentTense", "Pres");
  public static final Morpheme past = new Morpheme("PastTense", "Past");

  // Verb specific
  public static final Morpheme cop = new Morpheme("Copula", "Cop");

  // Negative Verb
  public static final Morpheme neg = new Morpheme("Negative", "Neg");
  

}
