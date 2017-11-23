package morphology;

import java.io.IOException;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.analysis.tr.TurkishSentenceAnalyzer;

public class DisambiguateSentences {

  TurkishSentenceAnalyzer sentenceAnalyzer;

  public DisambiguateSentences(TurkishSentenceAnalyzer sentenceAnalyzer) {
    this.sentenceAnalyzer = sentenceAnalyzer;
  }

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Z3MarkovModelDisambiguator disambiguator = new Z3MarkovModelDisambiguator();
    TurkishSentenceAnalyzer sentenceAnalyzer = new TurkishSentenceAnalyzer(
        morphology,
        disambiguator
    );
    new DisambiguateSentences(sentenceAnalyzer)
        .analyzeAndDisambiguate("86 lira harcardım.");
  }

  void analyzeAndDisambiguate(String sentence) {
    System.out.println("Sentence  = " + sentence);
    SentenceAnalysis result = sentenceAnalyzer.analyze(sentence);

    System.out.println("Before disambiguation.");
    writeParseResult(result);

    System.out.println("\nAfter disambiguation.");
    sentenceAnalyzer.disambiguate(result);

    writeParseResult(result);

  }

  private void writeParseResult(SentenceAnalysis sentenceAnalysis) {
    for (SentenceAnalysis.Entry entry : sentenceAnalysis) {
      System.out.println("Word = " + entry.input);
      for (WordAnalysis analysis : entry.parses) {
        System.out.println(analysis.formatLong());
      }
    }
  }
}
