package zemberek.morphology.lexicon.graph;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.morphology.lexicon.SuffixForm;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class SuffixSurfaceNode extends MorphNode {

    SuffixForm suffixForm;
    Set<SuffixSurfaceNode> successors = new HashSet<>(1);

    public SuffixSurfaceNode(
            SuffixForm suffixForm,
            String surfaceForm,
            EnumSet<PhoneticAttribute> attributes,
            EnumSet<PhoneticExpectation> expectations,
            SuffixData exclusiveSuffixData,
            TerminationType termination) {
        super(surfaceForm, termination, attributes, expectations, exclusiveSuffixData);
        this.suffixForm = suffixForm;
    }

    public SuffixSurfaceNode(
            SuffixForm suffixForm,
            String surfaceForm,
            EnumSet<PhoneticAttribute> attributes,
            TerminationType termination) {
        // TODO: expectations and exclusive suffix data is empty.
        super(surfaceForm, termination, attributes, EnumSet.noneOf(PhoneticExpectation.class), new SuffixData());
        this.suffixForm = suffixForm;
        this.attributes = attributes;
    }

    public SuffixForm getSuffixForm() {
        return suffixForm;
    }

    public EnumSet<PhoneticAttribute> getAttributes() {
        return attributes;
    }

    public SuffixSurfaceNode addSuccessorNode(SuffixSurfaceNode form) {
        this.successors.add(form);
        return this;
    }

    public Set<SuffixSurfaceNode> getSuccessors() {
        return successors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuffixSurfaceNode that = (SuffixSurfaceNode) o;
        if (!surfaceForm.equals(that.surfaceForm)) return false;
        if (!attributes.equals(that.attributes)) return false;
        if (!expectations.equals(that.expectations)) return false;
        //TODO: equals uses suffixFormId but not the hashCode.
        if (!suffixForm.getId().equals(that.suffixForm.getId())) return false;
        if (!exclusiveSuffixData.equals(that.exclusiveSuffixData)) return false;
        if (!termination.equals(that.termination)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = suffixForm.hashCode();
        result = 31 * result + attributes.hashCode();
        result = 31 * result + expectations.hashCode();
        result = 31 * result + surfaceForm.hashCode();
        result = 31 * result + exclusiveSuffixData.hashCode();
        result = 31 * result + termination.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return suffixForm.id + ":" + this.surfaceForm;
    }


    public String dump() {
        String surface = surfaceForm.length() == 0 ? "NULL" : surfaceForm;
        StringBuilder sb = new StringBuilder(" [set:" + suffixForm.id + "|" + surface + "]");
        if (successors.size() > 0) {
            sb.append(" [Successors:");
            int i = 0;
            for (SuffixSurfaceNode successor : successors) {
                sb.append(successor.suffixForm.getId());
                if (i++ < successors.size() - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }
        printAttributes(sb);
        printExpectations(sb);
        sb.append(" [T:" + termination.name() + "] ");
        return sb.toString();
    }

    private void printAttributes(StringBuilder sb) {
        if (!attributes.isEmpty())
            sb.append(" [A:");
        else return;
        int i = 0;
        for (PhoneticAttribute attribute : attributes) {
            sb.append(attribute.getStringForm());
            if (i++ < attributes.size() - 1)
                sb.append(", ");
        }
        sb.append("]");
    }

    private void printExpectations(StringBuilder sb) {
        if (!expectations.isEmpty())
            sb.append(" [E:");
        else return;
        int i = 0;
        for (PhoneticExpectation attribute : expectations) {
            sb.append(attribute.name());
            if (i++ < expectations.size() - 1)
                sb.append(", ");
        }
        sb.append("]");
    }
}
