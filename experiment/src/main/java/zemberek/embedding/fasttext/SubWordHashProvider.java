package zemberek.embedding.fasttext;

public interface SubWordHashProvider {
    int[] getHashes(String word, int wordId);
}
