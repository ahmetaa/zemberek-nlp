package zemberek.core.embeddings;

public class EmbeddingHashProviders {


  /**
   * this algorithm is also slightly different than the original. This basically computes the
   * character ngrams from a word But it does not use the ngram, instead it calculates a hash of
   * it.
   *
   * minn defines the minimum n-gram length, maxn defines the maximum ngram length. For example, For
   * word 'zemberek' minn = 3 and maxn = 6 these ngrams are calculated:
   * <pre>_ze, _zem, _zemb, _zembe
   * zem, zemb, zembe, zember emb, embe, ember, embere mbe, mber, mbere, mberek ber, bere, berek,
   * berek_ ere, erek, erek_ rek, rek_, ek_
   * </pre>
   * <p>
   * If wordId is not -1, wordId value is added to result[0]
   */
  public static class CharacterNgramHashProvider implements SubWordHashProvider {

    int minn;
    int maxn;

    public CharacterNgramHashProvider(int minn, int maxn) {
      this.minn = minn;
      this.maxn = maxn;
    }

    @Override
    public int[] getHashes(String word, int wordId) {

      int endGram = maxn < word.length() ? maxn : word.length();
      int size = 0;
      for (int i = minn; i <= endGram; i++) {
        size += (word.length() - i + 1);
      }

      int[] result;
      int counter;
      if (wordId == -1) {
        result = new int[size];
        counter = 0;
      } else {
        result = new int[size + 1];
        result[0] = wordId;
        counter = 1;
      }

      if (word.length() < minn) {
        return result;
      }

      for (int i = 0; i <= word.length() - minn; i++) {
        int n = minn;
        while (i + n <= word.length() && n <= endGram) {
          result[counter] = Dictionary.hash(word, i, i + n);
          n++;
          counter++;
        }
      }
      return result;
    }

    @Override
    public int getMinN() {
      return minn;
    }

    @Override
    public int getMaxN() {
      return maxn;
    }
  }

  public static class EmptySubwordHashProvider implements SubWordHashProvider {

    @Override
    public int[] getHashes(String word, int wordId) {
      if (wordId == -1) {
        return new int[0];
      } else {
        int[] result = new int[1];
        result[0] = wordId;
        return result;
      }
    }

    @Override
    public int getMinN() {
      return 0;
    }

    @Override
    public int getMaxN() {
      return 0;
    }
  }

  public static class SuffixPrefixHashProvider implements SubWordHashProvider {

    int minn;
    int maxn;

    public SuffixPrefixHashProvider(int minn, int maxn) {
      this.minn = minn;
      this.maxn = maxn;
    }

    @Override
    public int[] getHashes(String word, int wordId) {

      int endGram = maxn < word.length() ? maxn : word.length();
      int size = (endGram - minn) * 2;

      int[] result;
      int counter;
      if (wordId == -1) {
        result = new int[size];
        counter = 0;
      } else {
        result = new int[size + 1];
        result[0] = wordId;
        counter = 1;
      }

      if (word.length() < minn) {
        return result;
      }

      // prefixes
      for (int i = minn; i < endGram; i++) {
        result[counter] = Dictionary.hash(word, 0, i);
        counter++;
      }
      // suffixes
      for (int i = word.length() - endGram + 1; i <= word.length() - minn; i++) {
        result[counter] = Dictionary.hash(word, i, word.length());
        counter++;
      }

      return result;
    }

    @Override
    public int getMinN() {
      return minn;
    }

    @Override
    public int getMaxN() {
      return maxn;
    }
  }

}
