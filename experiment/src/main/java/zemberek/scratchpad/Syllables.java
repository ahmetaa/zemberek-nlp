package zemberek.scratchpad;

import com.google.common.collect.HashMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class Syllables {

  public static void getTwoConsonantStartWords() throws IOException {
    HashMultimap<String, String> map = HashMultimap.create();

    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    for (DictionaryItem item : lexicon) {

      String lemma = item.lemma;
      if (item.attributes.contains(RootAttribute.Dummy)) {
        continue;
      }
      if (item.secondaryPos == SecondaryPos.Abbreviation) {
        continue;
      }
      if (lemma.length() < 4 || TurkishAlphabet.INSTANCE.vowelCount(lemma) < 2) {
        continue;
      }
      if (!TurkishAlphabet.INSTANCE.isVowel(lemma.charAt(0)) &&
          !TurkishAlphabet.INSTANCE.isVowel(lemma.charAt(1))) {
        map.put(lemma.substring(0, 2), lemma);
      }
    }
    List<String> list = new ArrayList<>(map.keySet());
    list.sort((a, b) -> Integer.compare(
        map.get(b).size(),
        map.get(a).size()));

    List<String> result = new ArrayList<>();
    List<String> acceptedPrefixes = new ArrayList<>();
    for (String s : list) {
      result.add(s + " " + String.join(",", map.get(s)));
      if (Character.isUpperCase(s.charAt(0))) {
        if (map.get(s).size() > 3) {
          acceptedPrefixes.add(s.substring(0, 2).toLowerCase(Turkish.LOCALE));
        }
      } else {
        acceptedPrefixes.add(s.substring(0, 2).toLowerCase(Turkish.LOCALE));
      }
    }
    acceptedPrefixes = new ArrayList<>(new LinkedHashSet<>(acceptedPrefixes));
    acceptedPrefixes.sort(Turkish.STRING_COMPARATOR_ASC);

    Files.write(Paths.get("two-consonant-words"), list);
    Files.write(Paths.get("two-consonant-words.all"), result);
    Files.write(Paths.get("accepted-syllable-prefixes"), acceptedPrefixes);


  }

  public static void main(String[] args) throws IOException {
    getTwoConsonantStartWords();
  }

}
