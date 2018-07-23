package zemberek.core.embeddings;

public interface SubWordHashProvider {

  int[] getHashes(String word, int wordId);

  int getMinN();

  int getMaxN();

}
