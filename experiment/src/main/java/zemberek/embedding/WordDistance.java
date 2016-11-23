package zemberek.embedding;

public class WordDistance implements Comparable<WordDistance> {
    public final WordVector v;
    public final float distance;

    public WordDistance(WordVector v, float distance) {
        this.v = v;
        this.distance = distance;
    }

    public String toString() {
        return v.word + " " + distance;
    }

    @Override
    public int compareTo(WordDistance o) {
        return Float.compare(distance, o.distance);
    }
}
