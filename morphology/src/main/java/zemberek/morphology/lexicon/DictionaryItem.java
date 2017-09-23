package zemberek.morphology.lexicon;

import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;

import java.util.EnumSet;
import java.util.Locale;

/**
 * DictionaryItem represents an entity from a dictionary.
 */
public class DictionaryItem {

    /**
     * This is the unique ID of the item.
     * It is generated from Pos and lemma. If there are multiple items with same POS and Lemma
     * user needs to add an index for distinction. Structure of the ID: lemma_POS or lemma_POS_index
     */
    public String id;

    /**
     * the exact surface form of the item used in dictionary.
     */
    public final String lemma;

    /**
     * Form which will be used during graph generation. Such as, dictionary Item [gelmek Verb]'s root is "gel"
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
     * If this item has special Suffix information. Such as only a special form of a suffix may follow this Item.
     */
    public ExclusiveSuffixData suffixData;

    public SuffixForm specialRootSuffix;

    public DictionaryItem referenceItem;

    public int index;

    public static final DictionaryItem UNKNOWN = new DictionaryItem("UNK", "UNK", "UNK", PrimaryPos.Unknown, SecondaryPos.Unknown);

    public DictionaryItem(String lemma,
                          String root,
                          String pronunciation,
                          PrimaryPos primaryPos,
                          SecondaryPos secondaryPos,
                          EnumSet<RootAttribute> attributes,
                          ExclusiveSuffixData suffixData,
                          SuffixForm specialRootSuffix) {
        this.pronunciation = pronunciation;
        this.lemma = lemma;
        this.primaryPos = primaryPos;
        this.secondaryPos = secondaryPos;
        this.attributes = attributes;
        this.suffixData = suffixData;
        this.root = root;
        this.specialRootSuffix = specialRootSuffix;
        this.index = 0;
        this.id = generateId(lemma, primaryPos, secondaryPos, 0);
    }

    private String generateId(String lemma, PrimaryPos pos, SecondaryPos spos, int index) {
        StringBuilder sb = new StringBuilder(lemma).append("_").append(pos.shortForm);
        if (spos != null && spos != SecondaryPos.None) {
            sb.append("_").append(spos.shortForm);
        }
        if (index > 0)
            sb.append("_").append(index);
        return sb.toString();
    }

    public DictionaryItem(String lemma,
                          String root,
                          String pronunciation,
                          PrimaryPos primaryPos,
                          SecondaryPos secondaryPos,
                          EnumSet<RootAttribute> attributes,
                          ExclusiveSuffixData suffixData,
                          SuffixForm specialRootSuffix,
                          int index) {
        this.pronunciation = pronunciation;
        this.lemma = lemma;
        this.primaryPos = primaryPos;
        this.secondaryPos = secondaryPos;
        this.attributes = attributes;
        this.suffixData = suffixData;
        this.root = root;
        this.specialRootSuffix = specialRootSuffix;
        this.index = index;
        this.id = generateId(lemma, primaryPos, secondaryPos, index);
    }

    public DictionaryItem(String lemma,
                          String root,
                          PrimaryPos primaryPos,
                          SecondaryPos secondaryPos,
                          EnumSet<RootAttribute> attributes,
                          ExclusiveSuffixData suffixData,
                          SuffixForm specialRootSuffix) {
        this.lemma = lemma;
        this.pronunciation = root;
        this.primaryPos = primaryPos;
        this.secondaryPos = secondaryPos;
        this.suffixData = suffixData;
        this.attributes = attributes;
        this.root = root;
        this.specialRootSuffix = specialRootSuffix;
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

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public boolean hasAttribute(RootAttribute attribute) {
        return attributes.contains(attribute);
    }

    public static Locale TURKISH_LOCALE = new Locale("tr");

    public String normalizedLemma() {
        return lemma.toLowerCase(TURKISH_LOCALE);
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(lemma + " " + "[P:" + primaryPos.shortForm);
        if (secondaryPos != null && secondaryPos != SecondaryPos.None)
            sb.append(", ").append(secondaryPos.shortForm);
        if (attributes != null && attributes.isEmpty())
            sb.append("]");
        else
            printAttributes(sb, attributes);
        return sb.toString();
    }

    public boolean hasDifferentPronunciation() {
        return !pronunciation.equals(root);
    }

    private void printAttributes(StringBuilder sb, EnumSet<RootAttribute> attrs) {
        if (attrs !=null && !attrs.isEmpty())
            sb.append("; A:");
        else return;
        int i = 0;
        for (RootAttribute attribute : attrs) {
            sb.append(attribute.name());
            if (i++ < attrs.size() - 1)
                sb.append(", ");
        }
        sb.append("]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DictionaryItem that = (DictionaryItem) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
