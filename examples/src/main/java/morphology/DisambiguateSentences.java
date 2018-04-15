package morphology;

import java.io.IOException;
import java.util.List;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SentenceWordAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphology;
import zemberek.morphology._analyzer._WordAnalysis;

public class DisambiguateSentences {

  _TurkishMorphology morphology;

  public DisambiguateSentences(_TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();

    new DisambiguateSentences(morphology)
        .analyzeAndDisambiguate("Bu akşam kar yağacak gibi.");
  }

  void analyzeAndDisambiguate(String sentence) {
    System.out.println("Sentence  = " + sentence);
    List<_WordAnalysis> analyses = morphology.analyzeSentence(sentence);

    System.out.println("Sentence word analysis result:");
    writeParseResult(analyses);

    _SentenceAnalysis result = morphology.disambiguate(analyses);
    System.out.println("\nBest analyses:");
    for (_SentenceWordAnalysis sentenceWordAnalysis : result) {
      System.out.println(sentenceWordAnalysis.getAnalysis().formatLong());
    }
  }

  private void writeParseResult(List<_WordAnalysis> sentenceAnalysis) {
    for (_WordAnalysis entry : sentenceAnalysis) {
      System.out.println("Word = " + entry.getInput());
      for (_SingleAnalysis analysis : entry) {
        System.out.println(analysis.formatLong());
      }
    }
  }
}
