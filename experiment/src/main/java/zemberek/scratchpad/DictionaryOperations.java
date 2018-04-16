package zemberek.scratchpad;

import java.util.HashSet;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.old_analysis.tr.TurkishMorphology;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.core.turkish.Turkish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DictionaryOperations {


  public static void matchingLines(String toMatch, Path out) throws IOException {

    Path dictionaryRoot = Paths.get("morphology/src/main/resources");
    List<String> resources = TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES;

    List<String> matches = new ArrayList<>();
    for (String resource : resources) {
      Path resourcePath = dictionaryRoot.resolve(resource);
      List<String> lines = Files.readAllLines(resourcePath).stream().filter(
          s -> s.contains(toMatch)
      ).collect(Collectors.toList());
      matches.addAll(lines);
    }
    matches.sort(Turkish.STRING_COMPARATOR_ASC);
    Files.write(out, matches);
  }

  public static void saveLemmas(int minLength) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Set<String> set = new HashSet<>();
    for (DictionaryItem item : morphology.getLexicon()) {

      String lemma = item.lemma;
      if (item.attributes.contains(RootAttribute.Dummy)) {
        continue;
      }
      if (lemma.length() < minLength) {
        continue;
      }
      if (item.primaryPos == PrimaryPos.Punctuation) {
        continue;
      }
      set.add(lemma);
    }
    List<String> list = new ArrayList<>(set);
    list.sort(Turkish.STRING_COMPARATOR_ASC);
    Files.write(Paths.get("zemberek.vocab"), list);
  }

  public static void saveProperNouns() throws IOException {
    //TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    Set<String> set = new HashSet<>();
    for (DictionaryItem item : lexicon) {

      String lemma = item.lemma;
      if (item.attributes.contains(RootAttribute.Dummy)) {
        continue;
      }
      if (item.secondaryPos != SecondaryPos.ProperNoun) {
        continue;
      }
      set.add(lemma);
    }
    List<String> list = new ArrayList<>(set);
    list.sort(Turkish.STRING_COMPARATOR_ASC);
    Files.write(Paths.get("zemberek.proper.vocab"), list);
  }


  public static void main(String[] args) throws IOException {
    //saveLemmas(1);
    saveProperNouns();
    //matchingLines("P:Det", Paths.get("det.txt"));
  }

}
