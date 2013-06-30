package zemberek.morphology.lexicon;


import zemberek.core.turkish.PrimaryPos;

public class RootSuffix extends  Suffix {

    public PrimaryPos pos;

    public RootSuffix(String id) {
        super(id);
    }

    public RootSuffix(String id, PrimaryPos pos) {
        super(id);
        this.pos = pos;
    }
}
