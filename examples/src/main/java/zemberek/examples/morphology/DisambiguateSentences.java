package zemberek.examples.morphology;

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
    String sentence = "Yarın akşam kar yağacak gibi.";

    List<WordAnalysis> parses = morphology.analyzeSentence(sentence);
    SentenceAnalysis result = morphology.disambiguate(sentence, parses);

    for(SingleAnalysis a : result.bestAnalysis()) {
      System.out.println(a);
    }
  }
}
