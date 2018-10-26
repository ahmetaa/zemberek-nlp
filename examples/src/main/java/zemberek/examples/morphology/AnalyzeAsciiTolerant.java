package zemberek.examples.morphology;

import java.io.IOException;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.lexicon.DictionarySerializer;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.InformalTurkishMorphotactics;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class AnalyzeAsciiTolerant {

  public static void main(String[] args) throws IOException {
    RootLexicon lexicon = DictionarySerializer.loadFromResources("/tr/lexicon.bin");
    TurkishMorphotactics morphotactics = new InformalTurkishMorphotactics(lexicon);

    TurkishMorphology morphology = TurkishMorphology.builder()
        .useAnalyzer(RuleBasedAnalyzer.ignoreDiacriticsInstance(morphotactics))
        .useLexicon(lexicon)
        .build();

    morphology.analyze("kisi").forEach(System.out::println);
  }

}
