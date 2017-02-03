package zemberek.morphology.lexicon.graph;


import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;

import java.util.EnumSet;

public abstract class MorphNode {
    public final String surfaceForm;
    public TerminationType termination = TerminationType.TERMINAL;
    public EnumSet<PhoneticExpectation> expectations = EnumSet.noneOf(PhoneticExpectation.class);
    public EnumSet<PhoneticAttribute> attributes = EnumSet.noneOf(PhoneticAttribute.class);
    public SuffixData exclusiveSuffixData = new SuffixData();


    protected MorphNode(
            String surfaceForm,
            TerminationType termination,
            EnumSet<PhoneticAttribute> attributes,
            EnumSet<PhoneticExpectation> expectations,
            SuffixData exclusiveSuffixData
    ) {
        this.surfaceForm = surfaceForm;
        this.termination = termination;
        this.attributes = attributes;
        this.expectations = expectations;
        this.exclusiveSuffixData = exclusiveSuffixData;
    }

    protected MorphNode(
            String surfaceForm,
            TerminationType termination,
            EnumSet<PhoneticAttribute> attributes,
            EnumSet<PhoneticExpectation> expectations
    ) {
        this.surfaceForm = surfaceForm;
        this.termination = termination;
        this.attributes = attributes;
        this.expectations = expectations;
    }

    protected MorphNode(
            String surfaceForm,
            EnumSet<PhoneticAttribute> attributes,
            EnumSet<PhoneticExpectation> expectations
    ) {
        this.surfaceForm = surfaceForm;
        this.attributes = attributes;
        this.expectations = expectations;
    }

    public EnumSet<PhoneticExpectation> getExpectations() {
        return expectations;
    }

    public boolean isNullMorpheme() {
        return surfaceForm.length() == 0;
    }

    public void setTermination(TerminationType termination) {
        this.termination = termination;
    }

}

