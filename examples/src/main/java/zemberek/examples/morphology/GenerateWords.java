package zemberek.examples.morphology;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateWords {

  public static void main(String[] args) {

    generateNouns();
    System.out.println();
    generateVerbs();
  }

  private static void generateNouns() {

    System.out.println("Generating Nouns.");

    String[] number = {"A3sg", "A3pl"};
    String[] possessives = {"P1sg", "P2sg", "P3sg"};
    String[] cases = {"Dat", "Loc", "Abl"};

    TurkishMorphology morphology =
        TurkishMorphology.builder().setLexicon("armut").disableCache().build();

    DictionaryItem item = morphology.getLexicon().getMatchingItems("armut").get(0);
    for (String numberM : number) {
      for (String possessiveM : possessives) {
        for (String caseM : cases) {
          List<Result> results =
              morphology.getWordGenerator().generate(item, numberM, possessiveM, caseM);
          results.forEach(s -> System.out.println(s.surface));
        }
      }
    }
  }


  private static void generateVerbs() {

    System.out.println("Generating Verbs.");

    String[] positiveNegatives = {"", "Neg"};
    String[] times = {"Imp", "Aor", "Past", "Prog1", "Prog2", "Narr", "Fut"};
    String[] persons = {"A1sg", "A2sg", "A3sg", "A1pl", "A2pl", "A3pl"};

    TurkishMorphology morphology =
        TurkishMorphology.builder().setLexicon("okumak").disableCache().build();

    for (String posNeg : positiveNegatives) {
      for (String time : times) {
        for (String person : persons) {
          List<String> seq = Stream.of(posNeg, time, person)
              .filter(s -> s.length() > 0)
              .collect(Collectors.toList());

          String stem = "oku";
          List<Result> results =
              morphology.getWordGenerator().generate(stem, seq);
          if (results.size() == 0) {
            System.out.println("Cennot generate Stem = [" + stem + "] Morphemes = " + seq);
            continue;
          }
          System.out.println(results.stream()
              .map(s -> s.surface)
              .collect(Collectors.joining(" ")) + " " + seq);
        }
      }
    }
  }

}
