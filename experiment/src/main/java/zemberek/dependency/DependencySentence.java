package zemberek.dependency;

import java.util.List;

public class DependencySentence {
    List<DependencyItem> items;

    public DependencySentence(List<DependencyItem> items) {
        this.items = items;
    }

    public int sentenceCount() {
        int i = 0;
        for (DependencyItem item : items) {
            if (item.depRelation == DependencyRelation.SENTENCE)
                i++;
        }
        return i;
    }

    public List<DependencyItem> getItems() {
        return items;
    }

    /**
     * returns the amount of lemmas. no punctuations.
     *
     * @return lemma count.
     */
    public int lemmaCount() {
        int i = 0;
        for (DependencyItem item : items) {
            if (!item.isPunctuation() && item.hasLemma())
                i++;
        }
        return i;
    }

    public DependencyItem getById(int id) {
        for (DependencyItem item : items) {
            if (item.id == id)
                return item;
        }
        return null;
    }

    public int getOrder(DependencyItem it) {
        return items.indexOf(it);
    }

    public String getAsConnlString() {
        StringBuilder sb = new StringBuilder();
        for (DependencyItem item : items) {
            sb.append(item.getAsConnlString()).append("\n");
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public String getAsSentence() {
        StringBuilder sb = new StringBuilder();
        for (DependencyItem item : items) {
            if (item.form.length() > 0)
                sb.append(item.form.trim()).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencySentence that = (DependencySentence) o;

        if (items != null ? !items.equals(that.items) : that.items != null) return false;

        return true;
    }

    public String toString() {
        return getAsSentence();
    }

    @Override
    public int hashCode() {
        return items != null ? items.hashCode() : 0;
    }
}
