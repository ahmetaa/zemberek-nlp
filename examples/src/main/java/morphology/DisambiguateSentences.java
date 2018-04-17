package morphology;

import java.io.IOException;
import java.util.List;
import zemberek.core.logging.Log;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

public class DisambiguateSentences {

  TurkishMorphology morphology;

  public DisambiguateSentences(TurkishMorphology morphology) {
    this.morphology = morphology;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    new DisambiguateSentences(morphology)
        .analyzeAndDisambiguate("Bu akşam kar yağacak gibi.");
  }

  void analyzeAndDisambiguate(String sentence) {
    Log.info("Sentence  = " + sentence);
    List<WordAnalysis> analyses = morphology.analyzeSentence(sentence);

    Log.info("Sentence word analysis result:");
    writeParseResult(analyses);

    SentenceAnalysis result = morphology.disambiguate(sentence, analyses);
    Log.info("\nBest analyses:");
    for (SentenceWordAnalysis sentenceWordAnalysis : result) {
      Log.info(sentenceWordAnalysis.getAnalysis().formatLong());
    }
  }

  private void writeParseResult(List<WordAnalysis> sentenceAnalysis) {
    for (WordAnalysis entry : sentenceAnalysis) {
      Log.info("Word = " + entry.getInput());
      for (SingleAnalysis analysis : entry) {
        Log.info(analysis.formatLong());
      }
    }
  }
}
