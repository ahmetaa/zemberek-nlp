package zemberek.morphology.lexicon;

import zemberek.morphology.lexicon.graph.TerminationType;

public class SuffixFormTemplate extends SuffixForm {

    public SuffixFormTemplate(int index, String id, Suffix suffix, TerminationType type) {
        super(index, id, suffix, type);
    }

    public SuffixFormTemplate(int index,String id, Suffix suffix) {
        super(index,id, suffix, TerminationType.TRANSFER);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuffixFormTemplate that = (SuffixFormTemplate) o;

        return id.equals(that.id) && suffix.equals(that.suffix);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + suffix.hashCode();
        return result;
    }


}
