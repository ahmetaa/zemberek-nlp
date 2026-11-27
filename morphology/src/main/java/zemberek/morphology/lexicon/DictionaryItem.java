package zemberek.morphology.lexicon;

import java.util.EnumSet;
import java.util.List;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;

/**
 * DictionaryItem represents an entity from a dictionary.
 */
public non-sealed class DictionaryItem implements RootLexicon.LemmaEntry {

  public static final DictionaryItem UNKNOWN = new DictionaryItem("UNK", "UNK", "UNK",
      PrimaryPos.Unknown, SecondaryPos.UnknownSec);

  /**
   * the exact surface form of the item used in dictionary.
   */
  public final String lemma;
  /**
   * Form which will be used during graph generation. Such as, dictionary Item [gelmek Verb]'s root
   * is "gel"
   */
  public final String root;
  /**
   * Primary POS information
   */
  public final PrimaryPos primaryPos;
  /**
   * Secondary POS information
   */
  public final SecondaryPos secondaryPos;
  /**
   * Attributes that this item carries. Such as voicing or vowel drop.
   */
  public final EnumSet<RootAttribute> attributes;
  /**
   * Primary pronunciation of the item.
   */
  public final String pronunciation;

  /**
   * Optional secondary pronunciations for items with multiple accepted suffix harmonies.
   */
  public final List<String> alternativePronunciations;

  /**
   * This is the unique ID of the item. It is generated from Pos and lemma. If there are multiple
   * items with same POS and Lemma user needs to add an index for distinction. Structure of the ID:
   * lemma_POS or lemma_POS_index
   */
  public String id;

  private DictionaryItem referenceItem;

  public int index;

  public DictionaryItem(String lemma,
      String root,
      String pronunciation,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos,
      EnumSet<RootAttribute> attributes) {
    this(lemma, root, pronunciation, primaryPos, secondaryPos, attributes, 0, List.of());
  }

  public DictionaryItem(
      String lemma,
      String root,
      String pronunciation,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos,
      EnumSet<RootAttribute> attributes,
      int index) {
    this(lemma, root, pronunciation, primaryPos, secondaryPos, attributes, index, List.of());
  }

  public DictionaryItem(
      String lemma,
      String root,
      String pronunciation,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos,
      EnumSet<RootAttribute> attributes,
      int index,
      List<String> alternativePronunciations) {
    this.pronunciation = pronunciation;
    this.lemma = lemma;
    this.primaryPos = primaryPos;
    this.secondaryPos = secondaryPos;
    this.attributes = attributes;
    this.root = root;
    this.index = index;
    this.alternativePronunciations =
        alternativePronunciations == null || alternativePronunciations.isEmpty()
            ? List.of()
            : List.copyOf(alternativePronunciations);
    this.id = generateId(lemma, primaryPos, secondaryPos, index);
  }

  public DictionaryItem(String lemma,
      String root,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos,
      EnumSet<RootAttribute> attributes) {
    this(lemma, root, root, primaryPos, secondaryPos, attributes, 0, List.of());
  }

  public DictionaryItem(String lemma,
      String root,
      String pronunciation,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos) {
    this(lemma, root, pronunciation, primaryPos, secondaryPos, EnumSet.noneOf(RootAttribute.class), 0, List.of());
  }

  public static String generateId(String lemma, PrimaryPos pos, SecondaryPos spos, int index) {
    StringBuilder sb = new StringBuilder(lemma).append("_").append(pos.shortForm);
    if (spos != null && spos != SecondaryPos.None) {
      sb.append("_").append(spos.shortForm);
    }
    if (index > 0) {
      sb.append("_").append(index);
    }
    return sb.toString();
  }

  public DictionaryItem getReferenceItem() {
    return referenceItem;
  }

  public void setReferenceItem(DictionaryItem referenceItem) {
    this.referenceItem = referenceItem;
  }

  public boolean isUnknown() {
    return this == UNKNOWN;
  }

  public boolean isProperNoun() {
    return secondaryPos == SecondaryPos.ProperNoun;
  }

  public boolean isAbbreviation() {
    return secondaryPos == SecondaryPos.Abbreviation;
  }

  public boolean isExtended() {
    return hasAttribute(RootAttribute.Ext);
  }

  public boolean hasAttribute(RootAttribute attribute) {
    return attributes.contains(attribute);
  }

  public boolean hasAnyAttribute(RootAttribute... attributes) {
    for (RootAttribute attribute : attributes) {
      if (this.attributes.contains(attribute)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return if this is a Verb, removes -mek -mak suffix. Otherwise returns the `lemma`
   */
  public String normalizedLemma() {
    return primaryPos == PrimaryPos.Verb ? lemma.substring(0, lemma.length() - 3) : lemma;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(lemma + " " + "[P:" + primaryPos.shortForm);
    if (secondaryPos != null && secondaryPos != SecondaryPos.None) {
      sb.append(", ").append(secondaryPos.shortForm);
    }
    if (attributes != null && attributes.isEmpty()) {
      sb.append("]");
    } else {
      printAttributes(sb, attributes);
    }
    return sb.toString();
  }

  public boolean hasDifferentPronunciation() {
    return !pronunciation.equals(root);
  }

  private void printAttributes(StringBuilder sb, EnumSet<RootAttribute> attrs) {
    if (attrs != null && !attrs.isEmpty()) {
      sb.append("; A:");
    } else {
      return;
    }
    int i = 0;
    for (RootAttribute attribute : attrs) {
      sb.append(attribute.name());
      if (i++ < attrs.size() - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DictionaryItem that = (DictionaryItem) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
