package zemberek.morphology.ambiguity;

import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.text.TextIO;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

public class AmbiguityResolutionTests {

  @Test
  public void issue157ShouldNotThrowNPE() {
    String input = "Yıldız Kızlar Dünya Şampiyonası FIVB'nin düzenlediği ve 18 "
        + "yaşının altındaki voleybolcuların katılabildiği bir şampiyonadır.";
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(input);
    Assert.assertEquals(TurkishTokenizer.DEFAULT.tokenize(input).size(), analysis.size());
    for (SentenceWordAnalysis sentenceWordAnalysis : analysis) {
      String token = sentenceWordAnalysis.getWordAnalysis().getInput();
      SingleAnalysis an = sentenceWordAnalysis.getBestAnalysis();
      System.out.println(token + " = " + an.formatLong());
    }
  }

  @Test
  public void shouldNotThrowException() throws IOException {
    List<String> lines = TextIO.loadLinesFromResource("corpora/cnn-turk-10k");
    lines = lines.subList(0,1000);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    for (String line : lines) {
      List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraph(line);
      for (String sentence : sentences) {
        morphology.analyzeAndDisambiguate(sentence);
      }
    }
  }

}
