package zemberek.morphology.lexicon.graph;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.morphology.lexicon.DictionaryItem;

import java.util.EnumSet;

public class StemNode extends MorphNode {

    DictionaryItem dictionaryItem;
    SuffixSurfaceNode suffixRootSurfaceNode;

    protected StemNode(String form,
                       DictionaryItem dictionaryItem,
                       SuffixSurfaceNode suffixRootSurfaceNode,
                       TerminationType termination) {
        super(form, termination);
        this.dictionaryItem = dictionaryItem;
        this.suffixRootSurfaceNode = suffixRootSurfaceNode;
    }

    public StemNode(String surfaceForm,
                    DictionaryItem dictionaryItem,
                    TerminationType termination,
                    EnumSet<PhoneticAttribute> phoneticAttributes,
                    EnumSet<PhoneticExpectation> expectations) {
        super(surfaceForm, termination, phoneticAttributes, expectations);
        this.dictionaryItem = dictionaryItem;
    }

    public StemNode(String surfaceForm,
                    DictionaryItem dictionaryItem,
                    TerminationType termination,
                    EnumSet<PhoneticAttribute> phoneticAttributes) {
        super(surfaceForm,
                termination,
                phoneticAttributes,
                EnumSet.noneOf(PhoneticExpectation.class));
        this.dictionaryItem = dictionaryItem;
    }

    public StemNode(String surfaceForm,
                    DictionaryItem dictionaryItem,
                    EnumSet<PhoneticAttribute> phoneticAttributes,
                    EnumSet<PhoneticExpectation> expectations) {
        super(surfaceForm, phoneticAttributes, expectations);
        this.dictionaryItem = dictionaryItem;
    }

    public StemNode(String surfaceForm,
                    DictionaryItem dictionaryItem,
                    TerminationType termination) {
        super(surfaceForm, termination,
                EnumSet.noneOf(PhoneticAttribute.class),
                EnumSet.noneOf(PhoneticExpectation.class));
        this.dictionaryItem = dictionaryItem;
    }

    public SuffixSurfaceNode getSuffixRootSurfaceNode() {
        return suffixRootSurfaceNode;
    }

    public DictionaryItem getDictionaryItem() {
        return dictionaryItem;
    }

    public boolean isTerminal() {
        return termination == TerminationType.TERMINAL;
    }

    @Override
    public String toString() {
        return surfaceForm + ":" + dictionaryItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StemNode stemNode = (StemNode) o;

        if (!dictionaryItem.equals(stemNode.dictionaryItem)) return false;
        if (suffixRootSurfaceNode != null ? !suffixRootSurfaceNode.equals(stemNode.suffixRootSurfaceNode) : stemNode.suffixRootSurfaceNode != null)
            return false;
        if (!attributes.equals(stemNode.attributes)) return false;
        if (!exclusiveSuffixData.equals(stemNode.exclusiveSuffixData)) return false;
        if (!expectations.equals(stemNode.expectations)) return false;
        if (!surfaceForm.equals(stemNode.surfaceForm)) return false;
        if (termination != stemNode.termination) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dictionaryItem.hashCode();
        result = 31 * result + (suffixRootSurfaceNode != null ? suffixRootSurfaceNode.hashCode() : 0);
        result = 31 * result + termination.hashCode();
        result = 31 * result + expectations.hashCode();
        result = 31 * result + attributes.hashCode();
        result = 31 * result + exclusiveSuffixData.hashCode();
        return result;
    }
}
