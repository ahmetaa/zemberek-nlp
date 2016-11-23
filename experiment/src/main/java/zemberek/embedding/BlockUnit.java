package zemberek.embedding;

import java.util.PriorityQueue;

public class BlockUnit {
    public final WordVector vector;
    PriorityQueue<WordDistance> distQueue = new PriorityQueue<>();

    public BlockUnit(WordVector vector) {
        this.vector = vector;
    }
}
