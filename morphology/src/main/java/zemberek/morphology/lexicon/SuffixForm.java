package zemberek.morphology.lexicon;

import zemberek.morphology.lexicon.graph.SuffixData;
import zemberek.morphology.lexicon.graph.TerminationType;

/**
 * A suffix form represents a lexical form of a suffix and it's connections to other forms.
 * For example for Aorist Tense there are 3 Suffix Forms available.
 * -Ar -Ir -z
 * For all those lexical forms a SuffixForm is used representing the form and the connections before compiling the
 * Lexicon Graph.
 */
public class SuffixForm {
    // an id that defines the node
    public String id;
    // an id that defines the node
    public int index=-1;
    // parent suffix
    public final Suffix suffix;
    // generation word.
    public final String generation;
    // can be an end suffix.
    public TerminationType terminationType = TerminationType.TERMINAL;

    public SuffixData indirectConnections = new SuffixData();
    public SuffixData connections = new SuffixData();

    public SuffixForm(int index, String id, Suffix suffix, String generation) {
        this.index = index;
        this.id = id;
        this.suffix = suffix;
        this.generation = generation;
    }

    public SuffixForm(int index, String id, Suffix suffix, TerminationType type) {
        this.index = index;
        this.id = id;
        this.suffix = suffix;
        this.terminationType = type;
        this.generation = "";
    }

    public SuffixData allConnections() {
        return new SuffixData(indirectConnections, connections);
    }

    public SuffixData allConnectionsUnique() {
        return new SuffixData(indirectConnections, connections);
    }

    public SuffixForm(int index, Suffix suffix, String generation, TerminationType terminationType) {
        this.index = index;
        this.id = suffix.id + "_" + generation;
        this.suffix = suffix;
        this.generation = generation;
        this.terminationType = terminationType;
    }

    public SuffixForm(int index, String id, Suffix suffix, String generation, TerminationType terminationType) {
        this.index = index;
        this.id = id;
        this.suffix = suffix;
        this.generation = generation;
        this.terminationType = terminationType;
    }

    public SuffixForm(int index, Suffix suffix, String generation) {
        this.index = index;
        this.suffix = suffix;
        this.generation = generation;
        this.id = suffix.id + "_" + generation;
    }

    public Suffix getSuffix() {
        return suffix;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuffixForm that = (SuffixForm) o;

        if (index != that.index) return false;
        if (!id.equals(that.id)) return false;
        if (!generation.equals(that.generation)) return false;
        if (!suffix.equals(that.suffix)) return false;

        if (terminationType != that.terminationType) return false;

        if (!connections.equals(that.connections)) return false;
        if (!indirectConnections.equals(that.indirectConnections)) return false;


        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + index;
        result = 31 * result + suffix.hashCode();
        result = 31 * result + generation.hashCode();
        result = 31 * result + terminationType.hashCode();
/*
        wordParseList = 31 * wordParseList + indirectConnections.hashCode();
        wordParseList = 31 * wordParseList + connections.hashCode();
*/

        return result;
    }

}
