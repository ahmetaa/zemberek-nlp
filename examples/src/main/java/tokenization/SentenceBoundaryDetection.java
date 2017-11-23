package tokenization;

import java.util.List;
import zemberek.tokenization.TurkishSentenceExtractor;

public class SentenceBoundaryDetection {

  public static void simpleSentenceBoundaryDetector() {
    String input =
        "Prof. Dr. Veli Davul açıklama yaptı. Kimse %6.5 lik enflasyon oranını beğenmemiş!" +
            " Oysa maçta ikinci olmuştuk... Değil mi?";
    System.out.println("Paragraph = " + input);
    TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
    List<String> sentences = extractor.fromParagraph(input);
    System.out.println("Sentences:");
    for (String sentence : sentences) {
      System.out.println(sentence);
    }
  }

  public static void main(String[] args) {
    simpleSentenceBoundaryDetector();
  }
}
