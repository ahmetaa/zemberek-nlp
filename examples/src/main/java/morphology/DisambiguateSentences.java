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

  public static void main(String[] args) throws IOException {

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    String sentence = "Bu akşam kar yağacak gibi.";
    Log.info("Sentence  = " + sentence);
    List<WordAnalysis> analyses = morphology.analyzeSentence(sentence);

    Log.info("Sentence word analysis result:");
    for (WordAnalysis entry : analyses) {
      Log.info("Word = " + entry.getInput());
      for (SingleAnalysis analysis : entry) {
        Log.info(analysis.formatLong());
      }
    }
    SentenceAnalysis result = morphology.disambiguate(sentence, analyses);

    Log.info("\nBest analyses : ");
    result.bestAnalysis().forEach(Log::info);
  }
}
