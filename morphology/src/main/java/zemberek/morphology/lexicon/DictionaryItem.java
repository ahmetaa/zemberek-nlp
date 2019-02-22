package zemberek.morphology.lexicon;

import java.util.EnumSet;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;

/**
 * DictionaryItem represents an entity from a dictionary.
 */
public class DictionaryItem {

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
   * Pronunciations of the item. TODO: This should be converted to an actual 'Pronunciation' item
   */
  public final String pronunciation;

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
    this.pronunciation = pronunciation;
    this.lemma = lemma;
    this.primaryPos = primaryPos;
    this.secondaryPos = secondaryPos;
    this.attributes = attributes;
    this.root = root;
    this.index = 0;
    this.id = generateId(lemma, primaryPos, secondaryPos, 0);
  }

  public DictionaryItem(
      String lemma,
      String root,
      String pronunciation,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos,
      EnumSet<RootAttribute> attributes,
      int index) {
    this.pronunciation = pronunciation;
    this.lemma = lemma;
    this.primaryPos = primaryPos;
    this.secondaryPos = secondaryPos;
    this.attributes = attributes;
    this.root = root;
    this.index = index;
    this.id = generateId(lemma, primaryPos, secondaryPos, index);
  }

  public DictionaryItem(String lemma,
      String root,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos,
      EnumSet<RootAttribute> attributes) {
    this.lemma = lemma;
    this.pronunciation = root;
    this.primaryPos = primaryPos;
    this.secondaryPos = secondaryPos;
    this.attributes = attributes;
    this.root = root;
    this.index = 0;
    this.id = generateId(lemma, primaryPos, secondaryPos, 0);
  }

  public DictionaryItem(String lemma,
      String root,
      String pronunciation,
      PrimaryPos primaryPos,
      SecondaryPos secondaryPos) {
    this.lemma = lemma;
    this.pronunciation = pronunciation;
    this.primaryPos = primaryPos;
    this.secondaryPos = secondaryPos;
    this.attributes = EnumSet.noneOf(RootAttribute.class);
    this.root = root;
    this.index = 0;
    this.id = generateId(lemma, primaryPos, secondaryPos, 0);
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
