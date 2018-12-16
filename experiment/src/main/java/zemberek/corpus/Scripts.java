package zemberek.corpus;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.collections.UIntSet;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.normalization.TextCleaner;

public class Scripts {

  public static void main(String[] args) throws IOException {
/*
    Path p = Paths.get("/media/aaa/Data/corpora/final/wowturkey.com");
    checkWeirdChars(p);
*/

    Path corporaRoot = Paths.get("/home/ahmetaa/data/zemberek/data/corpora");
    List<Path> roots = Lists.newArrayList(
        corporaRoot.resolve("www.aljazeera.com.tr"),
        corporaRoot.resolve("open-subtitles"),
        corporaRoot.resolve("wowturkey.com"),
        corporaRoot.resolve("www.cnnturk.com"),
        corporaRoot.resolve("www.haberturk.com"));
    Path out = corporaRoot.resolve("sentences.txt");
    //saveSentences(roots, out);
    //Path p = Paths.get("/media/aaa/Data/corpora/final/wowturkey.com");
    //checkWeirdChars(p);
    //morphemeNames();
    foobar();
  }

  static void saveUnambigious() throws IOException {
    Path goldTest = Paths.get("data/gold/gold-test.sentences");
    //Path goldTest = Paths.get("data/gold/test.txt");
    Path goldTestOut = Paths.get("data/gold/gold-test.txt");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    saveUnambiguous(clean(Files.readAllLines(goldTest)), morphology, goldTestOut);
  }

  static void morphemeNames() throws IOException {
    List<Morpheme> morphemes = TurkishMorphotactics.getAllMorphemes();
    for (Morpheme morpheme : morphemes) {
      System.out.println(morpheme.id + " " + morpheme.name);
    }
  }

  static void foobar() throws IOException {

    Path path = Paths
        .get("/home/aaa/projects/zemberek-nlp/morphology/src/main/resources/tr/person-names.dict");
    Path path2 = Paths
        .get(
            "/home/aaa/projects/zemberek-nlp/morphology/src/main/resources/tr/person-names-reduced.dict");

    List<String> bb = Files.readAllLines(path);

    TurkishMorphology morphology = TurkishMorphology.create(
        RootLexicon.builder().addTextDictionaryResources(
            "tr/master-dictionary.dict",
            "tr/non-tdk.dict",
            "tr/proper.dict",
            "tr/proper-from-corpus.dict",
            "tr/abbreviations.dict").build());

    List<String> r = new ArrayList<>();
    for (String s : bb) {
      if (s.trim().length() == 0) {
        continue;
      }
      s = s.replaceAll("[ ]+", " ").trim();
      DictionaryItem d = TurkishDictionaryLoader.loadFromString(s);
      if (!morphology.getLexicon().containsItem(d)) {
        r.add(s.trim());
      }
    }
    r.sort(Turkish.STRING_COMPARATOR_ASC);

    Files.write(path2, r);


  }

  private static void checkWeirdChars(Path root) throws IOException {
    List<Path> files = Files.walk(root, 1).filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());
    Histogram<String> chars = new Histogram<>();
    for (Path file : files) {
      System.out.println(file);
      LinkedHashSet<String> sentences = getSentences(file);
      for (String sentence : sentences) {
        for (int i = 0; i < sentence.length(); i++) {
          char c = sentence.charAt(i);
          if (c >= 0x300 && c <= 0x036f) {
            chars.add(String.valueOf(c));
          }
          if (Scripts.undesiredChars.contains(c)) {
            chars.add(String.valueOf(c));
          }
        }
      }
    }
    for (String s : chars.getSortedList()) {
      System.out.println(String.format("%x %d", (int) s.charAt(0), chars.getCount(s)));
    }
  }

  private static LinkedHashSet<String> getSentences(Path file) throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    return new LinkedHashSet<>(TextCleaner.cleanAndExtractSentences(lines));
  }

  private static List<String> clean(List<String> input) {
    List<String> normalized = new ArrayList<>();
    for (String sentence : input) {
      if (containsCombiningDiacritics(sentence)) {
        continue;
      }
      sentence = sentence.replaceAll("\\s+|\\u00a0+|\\u200b+", " ");
      sentence = sentence.replaceAll("[\\u00ad]", "");
      normalized.add(sentence);
    }
    return normalized;
  }

  private static UIntSet undesiredChars = UIntSet.of(
      0x200a, // hair space
      0x200b, // hair space
      0x2009, // thin space
      0x00a0, // non-breaking space
      0x2008, // punctuation space
      0x2000, // en quad
      0x2002, // en space
      0x2007, // figure space
      0x3000, // ideographic space
      0x2001, // em quad
      0x2003, // em space
      0x2004, // three-per-em
      0x2005, // four-per-em
      0x2006, // six-per-em
      0x202f, // narrow no-break space
      0x205f, // medium mathematical space

      0x00ad, // soft hyphen
      0x200b, // zero with space
      0x2028, // line separator
      0x2029 // paragraph separator
  );

  public static void saveUnambiguous(
      List<String> sentences,
      TurkishMorphology morphology,
      Path out)
      throws IOException {
    try (PrintWriter pwMorph = new PrintWriter(out.toFile(), "utf-8")) {
      for (String sentence : sentences) {

        SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);

        if (analysis.bestAnalysis().stream().anyMatch(SingleAnalysis::isUnknown)) {
          continue;
        }

        pwMorph.format("S:%s%n", sentence);
        for (SentenceWordAnalysis sw : analysis) {
          WordAnalysis wa = sw.getWordAnalysis();
          pwMorph.println(wa.getInput());

          SingleAnalysis best = sw.getBestAnalysis();
          for (SingleAnalysis singleAnalysis : wa) {
            boolean isBest = singleAnalysis.equals(best);
            if (wa.analysisCount() == 1) {
              pwMorph.println(singleAnalysis.formatLong());
            } else {
              pwMorph.format("%s%s%n", singleAnalysis.formatLong(), isBest ? "*" : "");
            }
          }
        }
        pwMorph.println();
      }
    }
  }

  public static void saveUnambiguous(
      List<SentenceAnalysis> sentences,
      Path out)
      throws IOException {
    try (PrintWriter pwMorph = new PrintWriter(out.toFile(), "utf-8")) {

      for (SentenceAnalysis analysis : sentences) {

        if (analysis.bestAnalysis().stream().anyMatch(SingleAnalysis::isUnknown)) {
          continue;
        }
        pwMorph.format("S:%s%n", analysis.getSentence());
        for (SentenceWordAnalysis sw : analysis) {
          WordAnalysis wa = sw.getWordAnalysis();
          pwMorph.println(wa.getInput());

          SingleAnalysis best = sw.getBestAnalysis();
          for (SingleAnalysis singleAnalysis : wa) {
            boolean isBest = singleAnalysis.equals(best);
            if (wa.analysisCount() == 1) {
              pwMorph.println(singleAnalysis.formatLong());
            } else {
              pwMorph.format("%s%s%n", singleAnalysis.formatLong(), isBest ? "*" : "");
            }
          }
        }
        pwMorph.println();
      }
    }
  }

  private static boolean containsCombiningDiacritics(String sentence) {
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (c >= 0x300 && c <= 0x036f) {
        return true;
      }
    }
    return false;
  }

}
