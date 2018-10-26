package zemberek.examples.morphology;

import java.io.IOException;
import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class AnalyzeAndConvertInformalWords {

  public static void main(String[] args) throws IOException {

    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
    TurkishMorphotactics morphotactics = new InformalTurkishMorphotactics(lexicon);

    TurkishMorphology morphology = TurkishMorphology.builder()
        .useAnalyzer(RuleBasedAnalyzer.instance(morphotactics))
        .useLexicon(lexicon)
        .build();

    List<SingleAnalysis> analyses = morphology
        .analyzeAndDisambiguate("okuycam diyo")
        .bestAnalysis();

    for (SingleAnalysis a : analyses) {
      System.out.println(a.surfaceForm() + "-" + a);
    }

    System.out.println("Converting formal surface form:");

    InformalAnalysisConverter converter =
        new InformalAnalysisConverter(morphology.getWordGenerator());

    for (SingleAnalysis a : analyses) {
      System.out.println(converter.convert(a.surfaceForm(), a));
    }

  }
}


