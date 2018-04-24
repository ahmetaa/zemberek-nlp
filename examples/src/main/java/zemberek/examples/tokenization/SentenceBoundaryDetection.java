package zemberek.examples.tokenization;

import java.util.List;
import zemberek.core.logging.Log;
import zemberek.tokenization.TurkishSentenceExtractor;

public class SentenceBoundaryDetection {

  public static void simpleSentenceBoundaryDetector() {
    String input =
        "Prof. Dr. Veli Davul açıklama yaptı. Kimse %6.5 lik enflasyon oranını beğenmemiş!" +
            " Oysa maçta ikinci olmuştuk... Değil mi?";
    Log.info("Paragraph = " + input);
    TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
    List<String> sentences = extractor.fromParagraph(input);
    Log.info("Sentences:");
    for (String sentence : sentences) {
      Log.info(sentence);
    }
  }

  public static void main(String[] args) {
    simpleSentenceBoundaryDetector();
  }
}
