package zemberek.scratchpad;

import com.google.common.collect.TreeMultimap;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import zemberek.core.logging.Log;
import zemberek.core.text.Regexps;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
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

  public static void saveRegular() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    Set<String> set = new HashSet<>();
    for (DictionaryItem item : morphology.getLexicon()) {

      String lemma = item.lemma;
      if (item.attributes.contains(RootAttribute.Dummy)) {
        continue;
      }
      if (item.primaryPos == PrimaryPos.Punctuation
          /*|| item.secondaryPos == SecondaryPos.ProperNoun
          || item.secondaryPos == SecondaryPos.Abbreviation*/) {
        continue;
      }

      set.add(lemma);
      TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
      if (alphabet.containsCircumflex(lemma)) {
        set.add(alphabet.normalizeCircumflex(lemma));
      }
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
    //saveRegular();
    extractGroups(Paths.get("tdk-out"),Paths.get("foo.txt"));    //saveProperNouns();
    //matchingLines("P:Det", Paths.get("det.txt"));
  }

  public static void extractGroups(Path root, Path output) throws IOException {

    Pattern p1 = Pattern
        .compile("(<td> <a href=\"index\\.php\\?option=com_gts&amp;arama=gts&amp;kelime=)(.+?)(&)");

    TreeMultimap<String, String> result = TreeMultimap.create();

    List<Path> files = Files.walk(root, 2)
        .filter(s -> s.toString().endsWith(".html"))
        .sorted(Comparator.comparing(a -> a.toFile().getName()))
        .collect(Collectors.toList());
    Log.info("There are %d files", files.size());
    for (Path file : files) {
      String s = TextIO.loadUtfAsString(file);

      List<String> matches = Regexps.getMatchesForGroup(s, p1, 2);
      String name = URLDecoder.decode(file.toFile().getName().replaceAll("\\.html",""), "utf-8");
      result.putAll(name, matches);
      if(matches.size()>0) {
        System.out.println(file);
        System.out.println("---");
        for (String match : matches) {
          System.out.println(match.trim());
        }
        System.out.println();
      }

    }

    try(PrintWriter pw = new PrintWriter(output.toFile(), "utf-8" )) {
      for(String key : result.keySet()) {
        List<String> vals = new ArrayList<>(result.get(key));
        String v = String.join("|",vals);
        if(v.length()==0) {
          v = "_";
        }
        pw.println(key+ " = "+ v );
      }
    }


  }

}
