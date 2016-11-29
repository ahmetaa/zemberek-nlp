package zemberek.dependency;

public class DependencyItem {
    public int id;
    public String form;
    public String lemma;
    public CoarsePosTag coarsePosTag;
    public PosTag posTag;
    public String feats;
    public int head;
    public DependencyRelation depRelation;
    public int projectiveHead;
    public DependencyRelation projectiveDepRelation;

    public static DependencyItem buildFromConnlLine(String line) {
        DependencyItem di = new DependencyItem();
        String[] splitz = line.trim().split("[\t]");
        di.id = di.readInt(splitz[0]);
        di.form = di.readString(splitz[1]);
        di.lemma = di.readString(splitz[2]);
        di.coarsePosTag = CoarsePosTag.getFromName(splitz[3]);
        di.posTag = PosTag.getFromName(splitz[4]);
        di.feats = di.readString(splitz[5]);
        di.head = di.readInt(splitz[6]);
        di.depRelation = DependencyRelation.getFromName(splitz[7]);
        di.projectiveHead = di.readInt(splitz[8]);
        di.projectiveDepRelation = DependencyRelation.getFromName(splitz[9]);
        return di;
    }

    @Override
    public DependencyItem clone() {
        try {
            return (DependencyItem) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasLemma() {
        return lemma != null && lemma.length() > 0;
    }

    public boolean hasForm() {
        return form != null && form.length() > 0;
    }

    public boolean isPunctuation() {
        return coarsePosTag.equals(CoarsePosTag.Punc);
    }

    private int readInt(String s) {
        if (s.equals("_"))
            return -1;
        else return Integer.parseInt(s);
    }

    String forConnl(int i) {
        return i == -1 ? "_" : String.valueOf(i);
    }

    String forConnl(String s) {
        return s.length() == 0 ? "_" : s;
    }

    private String readString(String s) {
        if (s.equals("_"))
            return "";
        else return s;
    }

    public String toString() {
        return getAsSpaceString();
    }

    public String getAsConnlString() {
        StringBuilder builder = new StringBuilder();
        builder.append(forConnl(id)).append('\t');
        builder.append(forConnl(form)).append('\t');
        builder.append(forConnl(lemma)).append('\t');
        builder.append(coarsePosTag.getAsConnlValue()).append('\t');
        builder.append(posTag.getAsConnlValue()).append('\t');
        builder.append(forConnl(feats)).append('\t');
        builder.append(forConnl(head)).append('\t');
        builder.append(depRelation.getAsConnlString()).append('\t');
        builder.append(forConnl(projectiveHead)).append('\t');
        builder.append(projectiveDepRelation.getAsConnlString());
        return builder.toString();
    }

    public String getAsSpaceString() {
        StringBuilder builder = new StringBuilder();
        builder.append(forConnl(id)).append(' ');
        builder.append(forConnl(form)).append(' ');
        builder.append(forConnl(lemma)).append(' ');
        builder.append(coarsePosTag.getAsConnlValue()).append(' ');
        builder.append(posTag.getAsConnlValue()).append(' ');
        builder.append(forConnl(feats)).append(' ');
        builder.append(forConnl(head)).append(' ');
        builder.append(depRelation.getAsConnlString()).append(' ');
        builder.append(forConnl(projectiveHead)).append(' ');
        builder.append(projectiveDepRelation.getAsConnlString());
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyItem that = (DependencyItem) o;

        if (id != that.id) return false;
        if (form != null ? !form.equals(that.form) : that.form != null) return false;
        if (lemma != null ? !lemma.equals(that.lemma) : that.lemma != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (form != null ? form.hashCode() : 0);
        result = 31 * result + (lemma != null ? lemma.hashCode() : 0);
        return result;
    }
}
