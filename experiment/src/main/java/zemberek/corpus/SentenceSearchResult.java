package zemberek.corpus;

public class SentenceSearchResult {

  public final int key;
  public final int docKey;
  public final String sentence;

  public SentenceSearchResult(int key, int docKey, String sentence) {
    this.key = key;
    this.docKey = docKey;
    this.sentence = sentence;
  }

  @Override
  public String toString() {
    return "[" + key + ", " + docKey + ", " + sentence + "]";
  }
}
