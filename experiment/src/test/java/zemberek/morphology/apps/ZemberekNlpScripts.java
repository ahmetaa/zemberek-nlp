package zemberek.morphology.apps;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.external.OflazerAnalyzerRunner;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class ZemberekNlpScripts {

  static TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
  //private static Path DATA_PATH = Paths.get("/media/depo/data/aaa");
  private static Path DATA_PATH = Paths.get("/home/ahmetaa/data/nlp");
  //private static Path DATA_PATH = Paths.get("/media/aaa/3t/data/nlp");
  private static Path NLP_TOOLS_PATH = Paths.get("/home/ahmetaa/apps/nlp/tools");
  private static Path OFLAZER_ANALYZER_PATH = NLP_TOOLS_PATH
      .resolve("Morphological-Analyzer/Turkish-Oflazer-Linux64");
  private static Collator turkishCollator = Collator.getInstance(new Locale("tr"));

  @Test
  @Ignore("Not a Test.")
  public void generateSuffixNames() throws IOException {
    List<Morpheme> morphemes = TurkishMorphotactics.getAllMorphemes();
    List<String> result = morphemes.stream().map(s -> s.id).collect(Collectors.toList());
    Files.write(Paths.get("suffix-list"), result);
  }

  @Test
  @Ignore("Not a Test.")
  public void createZemberekVocabulary() throws IOException {
    Path outDir = DATA_PATH.resolve("out");
    Files.createDirectories(outDir);
    TurkishMorphology parser = TurkishMorphology.createWithDefaults();

    List<String> vocab = new ArrayList<>(parser.getLexicon().size());
    for (DictionaryItem item : parser.getLexicon()) {
      vocab.add(item.lemma);
    }
    vocab.sort(turkishCollator::compare);
    Files.write(outDir.resolve("zemberek.vocab"), vocab);
  }

  @Test
  @Ignore("Not a Test.")
  public void parseLargeVocabularyZemberek() throws IOException {
    //Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");
    Path wordFreqFile = DATA_PATH.resolve("all-counts-sorted-freq.txt");
    Path outDir = DATA_PATH.resolve("out");
    Files.createDirectories(outDir);

    TurkishMorphology parser = TurkishMorphology.createWithDefaults();
    Log.info("Loading histogram.");
    Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');
    List<String> accepted = new ArrayList<>(histogram.size() / 3);

    int c = 0;
    for (String s : histogram) {
      try {
        WordAnalysis parses = parser.analyze(s);
        List<SingleAnalysis> analyses = parses.getAnalysisResults();
        if (analyses.size() > 0 &&
            analyses.get(0).getDictionaryItem().primaryPos != PrimaryPos.Unknown) {
          accepted.add(s);
        }
        if (c > 0 && c % 10000 == 0) {
          Log.info("Processed = " + c);
        }
        c++;
      } catch (Exception e) {
        Log.info("Exception in %s", s);
      }
    }

    save(outDir.resolve("zemberek-parsed-words.txt"), accepted);
    sortAndSave(outDir.resolve("zemberek-parsed-words.tr.txt"), accepted);
  }

  @Test
  @Ignore("Not a Test.")
  public void failedWordTestIssue124() throws IOException {
    Path failPath = DATA_PATH.resolve("fails.txt");
    LinkedHashSet<String> words = new LinkedHashSet<>(
        Files.readAllLines(failPath, StandardCharsets.UTF_8));

    LinkedHashSet<String> accepted = new LinkedHashSet<>();
    TurkishMorphology parser = TurkishMorphology.createWithDefaults();
    for (String s : words) {
      WordAnalysis parses = parser.analyze(s);
      List<SingleAnalysis> analyses = parses.getAnalysisResults();
      for (SingleAnalysis parse : analyses) {
        if (parse.isUnknown() || parse.isRuntime()) {
          continue;
        }
        accepted.add(s);
      }
    }
    for (String s : accepted) {
      words.remove(s);
    }
    Path failReduced = DATA_PATH.resolve("fails-reduced.txt");
    try (PrintWriter pw = new PrintWriter(failReduced.toFile(), "utf-8")) {
      words.forEach(pw::println);
    }

    Log.info("Word count = %d Found = %d Not Found = %d", words.size(), accepted.size(),
        words.size() - accepted.size());
  }

  private void save(Path outPath, List<String> accepted) throws IOException {
    Log.info("Writing.");
    try (PrintWriter pw = new PrintWriter(outPath.toFile(), "utf-8")) {
      accepted.forEach(pw::println);
    }
  }

  private void sortAndSave(Path outPath, List<String> accepted) throws IOException {
    Log.info("Sorting %d words.", accepted.size());
    accepted.sort(turkishCollator::compare);
    Log.info("Writing.");
    try (PrintWriter pw = new PrintWriter(outPath.toFile(), "utf-8")) {
      accepted.forEach(pw::println);
    }
  }

  @Test
  @Ignore("Not a Test.")
  public void getFrequentZemberek() throws IOException {
    int min = 30;
    Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");
    Path outDir = DATA_PATH.resolve("out");
    Log.info("Loading histogram.");
    Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');
    List<String> all = TextIO.loadLines(outDir.resolve("zemberek-parsed-words.txt"));
    List<String> result = all.stream().filter(s -> histogram.getCount(s) >= min)
        .collect(Collectors.toList());
    sortAndSave(outDir.resolve("zemberek-parsed-words-min" + min + ".txt"), result);
  }

  @Test
  @Ignore("Not a Test.")
  public void parseLargeVocabularyOflazer() throws IOException {

    OflazerAnalyzerRunner runner = new OflazerAnalyzerRunner(
        OFLAZER_ANALYZER_PATH.toFile(), OFLAZER_ANALYZER_PATH.resolve("tfeaturesulx.fst").toFile());

    Path wordFile = DATA_PATH.resolve("all-words-sorted-freq.txt");
    //Path wordFile = DATA_PATH.resolve("vocab-corpus-and-zemberek");
    Path outDir = DATA_PATH.resolve("out");
    Files.createDirectories(outDir);

    Path outPath = outDir.resolve("oflazer-parses.txt");
    runner.parseSentences(wordFile.toFile(), outPath.toFile());
  }

  @Test
  @Ignore("Not a Test.")
  public void parseLargeVocabularyZemberek2() throws IOException {

    Path wordFile = DATA_PATH.resolve("all-words-sorted-freq.txt");
    //Path wordFile = DATA_PATH.resolve("vocab-corpus-and-zemberek");
    Path outDir = DATA_PATH.resolve("out");
    Files.createDirectories(outDir);
    Path outCorrect = outDir.resolve("zemberek-parses.txt");
    Path outIncorrect = outDir.resolve("zemberek-incorrect.txt");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<String> words = Files.readAllLines(wordFile).subList(0, 2000_000);
    List<String> correct = new ArrayList<>();
    List<String> incorrect = new ArrayList<>();
    for (String word : words) {
      if (morphology.analyze(word).isCorrect()) {
        correct.add(word);
      } else {
        incorrect.add(word);
      }
    }
    Files.write(outCorrect, correct, StandardCharsets.UTF_8);
    Files.write(outIncorrect, incorrect, StandardCharsets.UTF_8);

  }

  @Test
  @Ignore("Not a Test.")
  public void ambiguousWords() throws IOException {
    Path outDir = DATA_PATH.resolve("out");
    Files.createDirectories(outDir);
    Path correct = outDir.resolve("zemberek-parses.txt");
    Path outAmbAn = outDir.resolve("zemberek-ambigious-analyses.txt");
    Path outAmbWord = outDir.resolve("zemberek-ambigious-words.txt");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<String> words = Files.readAllLines(correct).subList(0, 100_000);
    List<String> ambWords = new ArrayList<>();
    List<WordAnalysis> amb = new ArrayList<>();
    for (String word : words) {
      WordAnalysis analysis = morphology.analyze(word);
      if (!analysis.isCorrect() || analysis.analysisCount() == 1) {
      } else {
        HashSet<String> stems = new HashSet<>(4);
        for (SingleAnalysis s : analysis) {
          stems.add(s.getStem());
          if (stems.size() > 1) {
            amb.add(analysis);
            ambWords.add(word);
            break;
          }
        }
      }
    }
    Log.info("Writing %d words", amb.size());
    try (PrintWriter pwa = new PrintWriter(outAmbAn.toFile(), "utf-8")) {
      for (WordAnalysis wa : amb) {
        pwa.println(wa.getInput());
        for (SingleAnalysis analysis : wa) {
          pwa.println(analysis.formatLong());
        }
        pwa.println();
      }
    }
    Files.write(outAmbWord, ambWords, StandardCharsets.UTF_8);
  }

  @Test
  @Ignore("Not a Test.")
  public void extractFromOflazerAnalysisResult() throws IOException {
    Path inPath = DATA_PATH.resolve("out").resolve("oflazer-parses.txt");
    List<String> lines = Files.readAllLines(inPath, StandardCharsets.UTF_8);
    Log.info("Loaded.");
    LinkedHashSet<String> accepted = new LinkedHashSet<>(lines.size() / 5);
    for (String line : lines) {
      if (line.trim().length() == 0 || line.endsWith("+?")) {
        continue;
      }
      accepted.add(line.substring(0, line.indexOf('\t')));
    }
    save(DATA_PATH.resolve("out").resolve("oflazer-parsed-words.txt"),
        new ArrayList<>(accepted));

    sortAndSave(DATA_PATH.resolve("out").resolve("oflazer-parsed-words.tr.txt"),
        new ArrayList<>(accepted));
  }

  @Test
  @Ignore("Not a Test.")
  public void reduceOflazerAnalysisResult() throws IOException {
    Path inPath = DATA_PATH.resolve("out").resolve("oflazer-parses.txt");
    List<String> lines = Files.readAllLines(inPath, StandardCharsets.UTF_8);
    Log.info("Loaded.");
    LinkedHashSet<String> accepted = new LinkedHashSet<>(lines.size() / 5);
    for (String line : lines) {
      if (line.trim().length() == 0 || line.endsWith("+?")) {
        continue;
      }
      accepted.add(line.trim());
    }
    save(DATA_PATH.resolve("out").resolve("oflazer-analyses.txt"),
        new ArrayList<>(accepted));
  }

  @Test
  @Ignore("Not a Test.")
  public void extractTypesFromOflazerAnalysis() throws IOException {
    Path inPath = DATA_PATH.resolve("out").resolve("oflazer-analyses.txt");
    List<String> lines = Files.readAllLines(inPath, StandardCharsets.UTF_8);
    Log.info("Loaded.");
    LinkedHashMultimap<String, String> map = LinkedHashMultimap.create(10, 10000);

    Set<String> secondaryPosKeys = SecondaryPos.converter().getKeysSet();

    for (String line : lines) {
      if (line.trim().length() == 0 || line.endsWith("+?")) {
        continue;
      }
      List<String> parts = Splitter.on("\t").omitEmptyStrings().trimResults().splitToList(line);

      // output sometimes combines root and morph part.
      if (parts.size() < 3) {
        int plusIndex = parts.get(1).indexOf("+");
        String value = parts.get(1).substring(0, plusIndex);
        String morph = parts.get(1).substring(plusIndex + 1);
        parts = Lists.newArrayList(parts.get(0), value, morph);
      }

      String value = parts.get(1);
      List<String> morphs = Splitter.on("+").omitEmptyStrings().trimResults()
          .splitToList(parts.get(2));

      String primaryPos = morphs.get(0).replaceAll("\\^DB", "");
      String secondaryPos =
          morphs.size() > 1 && secondaryPosKeys.contains(morphs.get(1).replaceAll("\\^DB", ""))
              ? morphs.get(1).replaceAll("\\^DB", "") : "";

      if (primaryPos.equals("Verb")) {
        if (alphabet.getLastVowel(value).isFrontal()) {
          value = value + "mek";
        } else {
          value = value + "mak";
        }
      }
      if (primaryPos.equals("Adverb")) {
        primaryPos = "Adv";
      }

      String key = secondaryPos.length() == 0 ? "[P:" + primaryPos + "]"
          : "[P:" + primaryPos + "," + secondaryPos + "]";
      map.put(key, value);
    }

    Path path = DATA_PATH.resolve("out").resolve("dictionary-from-analysis.txt");
    try (PrintWriter pw = new PrintWriter(path.toFile(), StandardCharsets.UTF_8.name())) {
      for (String key : map.keySet()) {
        List<String> values = new ArrayList<>(map.get(key));
        values.sort(turkishCollator::compare);
        for (String value : values) {
          pw.println(value + " " + key);
        }
        pw.println();
      }
    }
  }

  @Test
  @Ignore("Not a Test.")
  public void findZemberekMissingOrDifferent() throws IOException {
    Path path = DATA_PATH.resolve("out");
    LinkedHashSet<String> oSet =
        new LinkedHashSet<>(TextIO.loadLines(path.resolve("dictionary-from-analysis.txt"))
            .stream()
            .filter(s -> !s.contains("Prop")).collect(Collectors.toList()));

    TurkishMorphology parser = TurkishMorphology.createWithDefaults();
    List<String> zemberekTypes = new ArrayList<>(parser.getLexicon().size());
    for (DictionaryItem item : parser.getLexicon()) {
      String lemma = /*item.primaryPos == PrimaryPos.Verb ? item.lemma.replaceAll("mek$|mak$", "") : */item.lemma;
      lemma = TurkishAlphabet.INSTANCE.normalizeCircumflex(lemma);
      String primaryString = /*item.primaryPos == PrimaryPos.Adverb ? "Adverb" :*/ item.primaryPos.shortForm;
      String pos = item.secondaryPos == null
          || item.secondaryPos == SecondaryPos.UnknownSec
          || item.secondaryPos == SecondaryPos.None ?
          "[P:" + primaryString + "]"
          : "[P:" + primaryString + "," + item.secondaryPos.shortForm + "]";
      zemberekTypes.add(lemma + " " + pos);
      if (pos.equals("[P:Noun]")) {
        zemberekTypes.add(lemma + " [P:Adj]");
      }
      if (pos.equals("[P:Adj]")) {
        zemberekTypes.add(lemma + " [P:Noun]");
      }

    }
    zemberekTypes.sort(turkishCollator::compare);
    Files.write(path.resolve("found-in-zemberek"), zemberekTypes);
    LinkedHashSet<String> zSet = new LinkedHashSet<>(zemberekTypes);

    oSet.removeAll(zSet);
    Files.write(path.resolve("not-found-in-zemberek"), oSet);
  }


  @Test
  @Ignore("Not a Test.")
  public void generateOnlyOflazer() throws IOException {
    Path inPath = DATA_PATH.resolve("out");
    List<String> zemberekAll =
        Files.readAllLines(inPath.resolve("zemberek-parsed-words.txt"));
    Log.info("Zemberek Loaded.");
    LinkedHashSet<String> onlyOflazer =
        new LinkedHashSet<>(Files.readAllLines(inPath.resolve("oflazer-parsed-words.txt")));
    Log.info("Oflazer Loaded.");
    zemberekAll.forEach(onlyOflazer::remove);
    Log.info("Writing.");
    Files.write(inPath.resolve("only-oflazer.txt"), onlyOflazer);
    Log.info("Oflazer-only saved.");
  }

  @Test
  @Ignore("Not a Test.")
  public void generateOnlyOflazerWithAnalyzer() throws IOException {
    Path inPath = DATA_PATH.resolve("out");
    List<String> oflazer = Files.readAllLines(inPath.resolve("only-oflazer-2.txt"));
    Log.info("Oflazer Loaded. %d words.", oflazer.size());
    List<String> result = new ArrayList<>(oflazer.size() / 10);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    int i = 0;
    for (String s : oflazer) {
      if (!morphology.analyze(s).isCorrect()) {
        result.add(s);
      }
      if (i++ % 20000 == 0) {
        Log.info("%d processed.", i);
      }
    }
    Log.info("Writing.");
    Files.write(inPath.resolve("only-oflazer-3.txt"), result);
    Log.info("Oflazer-only saved.");
  }

  @Test
  @Ignore("Not a Test.")
  public void extractCircumflexWords() throws IOException {
    Path inPath = DATA_PATH.resolve("out");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (DictionaryItem i : morphology.getLexicon()) {
      if (TurkishAlphabet.INSTANCE.containsCircumflex(i.lemma)) {
        result.add(i.lemma);
      }
    }
    Log.info("Writing.");
    Files.write(inPath.resolve("words-with-circumflex.txt"), result);
  }

  @Test
  @Ignore("Not a Test.")
  public void findObsoleteCircumflexWords() throws IOException {
    Path root = Paths.get("../tdk-out");
    List<Path> htmlList = Files.walk(root, 2)
        .filter(s -> s.toFile().isFile() && s.toString().endsWith("html"))
        .collect(Collectors.toList());
    List<String> obsolete = new ArrayList<>();
    for (Path path : htmlList) {
      String s = TextIO.loadUtfAsString(path);
      if (s.contains("yerine aşağıdakilerden") && !s.contains("id=\"hor-minimalist-a\"")) {

        obsolete.add(URLDecoder.decode(path.toFile().getName().replace(".html", ""), "utf-8"));
      }
    }
    Log.info("Writing.");
    Files.write(Paths.get("../data/vocabulary/words-with-circumflex-obsolete.txt"), obsolete);
  }

  @Test
  @Ignore("Not a Test.")
  public void dictionaryObsoleteCircumflexWordsCheck() throws IOException {

    Path path = Paths.get("../data/vocabulary/words-with-circumflex-obsolete.txt");
    List<String> obsolete = Files.readAllLines(path, StandardCharsets.UTF_8);
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    RootLexicon lexicon = morphology.getLexicon();
    List<String> single = new ArrayList<>();
    for (String s : obsolete) {
      List<DictionaryItem> items = lexicon.getMatchingItems(s);
      List<DictionaryItem> matchingItems = lexicon
          .getMatchingItems(TurkishAlphabet.INSTANCE.normalizeCircumflex(s));
      items.addAll(matchingItems);
      Log.info("%s = %s", s, items);
      if (items.size() == 1) {
        String line = items.get(0).toString();
        line = line.replace("[P:Noun]", "").trim();
        line = line.replace("[P:Noun, Prop]", "").trim();
        line = line.replace("P:Noun; ", "").trim();
        line = line.replace("P:Noun, Prop; ", "").trim();
        line = line.replace("P:Verb; ", "").trim();
        line = line.replace("[A:Voicing]", "").trim();
        single.add(line.replaceAll("\\s+", " ").trim());
      }
    }
    Path pathSingle = Paths.get("../data/vocabulary/words-with-circumflex-obsolete-single.txt");
    Files.write(pathSingle, single, StandardCharsets.UTF_8);
  }

  @Test
  @Ignore("Not a Test.")
  public void generateOnlyZemberek() throws IOException {
    Path dir = DATA_PATH.resolve("out");
    List<String> oflazerAll =
        Files.readAllLines(dir.resolve("oflazer-parsed-words.txt"));
    Log.info("Oflazer Loaded.");

    LinkedHashSet<String> onlyZemberek =
        new LinkedHashSet<>(Files.readAllLines(dir.resolve("zemberek-parsed-words.txt")));
    Log.info("Zemberek Loaded.");

    oflazerAll.forEach(onlyZemberek::remove);
    Log.info("Writing.");

    Files.write(dir.resolve("only-zemberek.txt"), onlyZemberek);
    Log.info("Zemberek-only saved.");

  }

  @Test
  @Ignore("Not a Test.")
  public void frequentUnknown() throws IOException {

    Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");

    Log.info("Loading histogram.");
    Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');

    Path dir = DATA_PATH.resolve("out");
    List<String> oflazerAll =
        Files.readAllLines(dir.resolve("oflazer-parsed-words.txt"));
    List<String> zemberekAll =
        Files.readAllLines(dir.resolve("zemberek-parsed-words.txt"));

    histogram.removeAll(oflazerAll);
    histogram.removeAll(zemberekAll);

    histogram.removeSmaller(10);

    Files.write(dir.resolve("no-parse-freq.txt"), histogram.getSortedList());
    Files.write(dir.resolve("no-parse-tr.txt"),
        histogram.getSortedList((a, b) -> turkishCollator.compare(a, b)));
  }

  @Test
  @Ignore("Not a Test.")
  public void unknownZemberek() throws IOException {

    Path wordFreqFile = DATA_PATH.resolve("vocab.all.freq");

    Log.info("Loading histogram.");
    Histogram<String> histogram = Histogram.loadFromUtf8File(wordFreqFile, ' ');

    Path dir = DATA_PATH.resolve("out");

    Log.info("Loading parseable.");
    List<String> zemberekAll =
        Files.readAllLines(dir.resolve("zemberek-parsed-words.txt"));

    histogram.removeAll(zemberekAll);

    //histogram.removeSmaller(10);
    Log.info("Saving.");

    Files.write(dir.resolve("no-parse-zemberek-freq.txt"), histogram.getSortedList());
    Files.write(dir.resolve("no-parse-zemberek-tr.txt"),
        histogram.getSortedList((a, b) -> turkishCollator.compare(a, b)));
  }


  @Test
  @Ignore("Not a Test.")
  public void guessRootsWithHeuristics() throws IOException {

    Path wordFreqFile = DATA_PATH.resolve("out/no-parse-zemberek-freq.txt");
    Log.info("Loading histogram.");
    List<String> words = Files.readAllLines(wordFreqFile);

    TurkishDictionaryLoader dictionaryLoader = new TurkishDictionaryLoader();
    //dictionaryLoader.load("elma");
    TurkishMorphology morphology =
        TurkishMorphology.builder().setLexicon("elma").disableCache().build();

    Multimap<String, String> res = HashMultimap.create(100000, 3);

    int c = 0;
    for (String s : words) {
      if (s.length() < 4) {
        continue;
      }
      if (!TurkishAlphabet.INSTANCE.containsVowel(s)) {
        continue;
      }

      for (int i = 2; i < s.length(); i++) {
        String candidateRoot = s.substring(0, i + 1);
        if (!TurkishAlphabet.INSTANCE.containsVowel(candidateRoot)) {
          continue;
        }
        List<DictionaryItem> items = new ArrayList<>(3);

        items.add(TurkishDictionaryLoader.loadFromString(candidateRoot)); //assumes noun.
        items.add(TurkishDictionaryLoader.loadFromString(candidateRoot + " [P:Verb]")); //assumes noun.
        char last = candidateRoot.charAt(candidateRoot.length() - 1);
        if (i < s.length() - 1) {
          char next = s.charAt(candidateRoot.length());
          if (Turkish.Alphabet.isVowel(next)) {
            String f = "";
            if (last == 'b') {
              f = candidateRoot.substring(0, candidateRoot.length() - 1) + 'p';
            } else if (last == 'c') {
              f = candidateRoot.substring(0, candidateRoot.length() - 1) + 'ç';
            } else if (last == 'ğ') {
              f = candidateRoot.substring(0, candidateRoot.length() - 1) + 'k';
            }
            if (last == 'd') {
              f = candidateRoot.substring(0, candidateRoot.length() - 1) + 't';
            }
            if (f.length() > 0) {
              items.add(TurkishDictionaryLoader.loadFromString(f));
            }
          }
        }
        for (DictionaryItem item : items) {
          morphology.getMorphotactics().getStemTransitions().addDictionaryItem(item);
          WordAnalysis analyze = morphology.analyze(s);
          for (SingleAnalysis wordAnalysis : analyze) {
            if (!wordAnalysis.isUnknown()) {
              res.put(candidateRoot, s);
            }
          }
          morphology.getMorphotactics().getStemTransitions().removeDictionaryItem(item);
        }

      }
      if (++c % 10000 == 0) {
        Log.info(c);
      }
      if (c == 100000) {
        break;
      }
    }

    Log.info("Writing.");
    try (PrintWriter pw1 = new PrintWriter(DATA_PATH.resolve("out/root-candidates-words").toFile());
        PrintWriter pw2 = new PrintWriter(
            DATA_PATH.resolve("out/root-candidates-vocabulary").toFile())) {
      for (String root : res.keySet()) {
        Collection<String> vals = res.get(root);
        if (vals.size() < 2) {
          continue;
        }
        List<String> wl = new ArrayList<>(vals);
        wl.sort(turkishCollator::compare);
        pw1.println(root + " : " + String.join(", ", vals));
        pw2.println(root);
      }
    }

  }

  @Test
  @Ignore("Not a Test.")
  public void sortAndSaveAgain() throws IOException {
    List<String> lines = Files.readAllLines(DATA_PATH.resolve("out/root-candidates-words"));
    lines.sort(turkishCollator::compare);
    Files.write(DATA_PATH.resolve("out/root-candidates-words.sorted"), lines);
  }


  @Test
  @Ignore("Not a Test.")
  public void performance() throws IOException {
    List<String> lines = Files.readAllLines(
        //Paths.get("/media/depo/data/aaa/corpora/dunya.100k")
        Paths.get("/home/ahmetaa/data/nlp/corpora/dunya.100k")
        //Paths.get("/media/depo/data/aaa/corpora/subtitle-1M")
    );

    TurkishMorphology analyzer = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .disableUnidentifiedTokenAnalyzer()
        .disableCache()
        .build();

    Log.info(lines.size() + " lines will be processed.");
    Log.info("Dictionary has " + analyzer.getLexicon().size() + " items.");

    long tokenCount = 0;
    long tokenCountNoPunct = 0;
    Stopwatch clock = Stopwatch.createStarted();
    TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;
    for (String line : lines) {
      List<Token> tokens = lexer.tokenize(line);
      tokenCount += tokens.stream()
          .filter(s -> (s.getType() != Token.Type.SpaceTab))
          .count();
      tokenCountNoPunct += tokens.stream()
          .filter(s -> (s.getType() != Token.Type.Punctuation
              && s.getType() != Token.Type.SpaceTab))
          .count();
    }
    long elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
    Log.info("Elapsed Time = " + elapsed);
    Log.info("Token Count = " + tokenCount);
    Log.info("Token Count (No Punctuation) = " + tokenCountNoPunct);
    Log.info("Tokenization Speed = %.1f tokens/sec",
        tokenCount * 1000d / elapsed);
    Log.info("Tokenization Speed (No Punctuation) = %.1f tokens/sec ",
        tokenCountNoPunct * 1000d / elapsed);
    Log.info("");
    Log.info("Sentence word analysis test:");
    int counter = 0;
    clock.reset().start();
    for (String line : lines) {
      try {
        List<WordAnalysis> res = analyzer.analyzeSentence(line);
        counter += res.size(); // for preventing VM optimizations.
      } catch (Exception e) {
        Log.info(line);
        e.printStackTrace();
      }
    }
    elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
    Log.info("Elapsed Time = " + elapsed);
    Log.info("Tokenization + Analysis speed = %.1f tokens/sec"
        , tokenCount * 1000d / elapsed);
    Log.info("Tokenization + Analysis speed (no punctuation) = %.1f tokens/sec"
        , tokenCountNoPunct * 1000d / elapsed);
    Log.info(analyzer.toString());
    Log.info("");

    Log.info("Disambiguation Test:");
    analyzer.invalidateCache();
    clock.reset().start();
    for (String line : lines) {
      try {
        SentenceAnalysis results = analyzer.analyzeAndDisambiguate(line);
        counter += results.size(); // for preventing VM optimizations.
      } catch (Exception e) {
        Log.info(line);
        e.printStackTrace();
      }
    }

    elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
    Log.info("Elapsed Time = " + elapsed);
    Log.info("Tokenization + Analysis + Disambiguation speed = %.1f tokens/sec"
        , tokenCount * 1000d / elapsed);
    Log.info("Tokenization + Analysis + Disambiguation speed (no punctuation) = %.1f tokens/sec"
        , tokenCountNoPunct * 1000d / elapsed);
    Log.info(counter);
  }


  @Test
  @Ignore("Not a Test.")
  public void memoryStressTest() throws IOException {
    List<String> words = Files.readAllLines(Paths.get("dunya"));
    TurkishMorphology parser = TurkishMorphology
        .builder()
        .setLexicon(RootLexicon.fromResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES))
        //.disableCache()
        //.disableUnidentifiedTokenAnalyzer()
        .build();

    int c = 0;
    for (int i = 0; i < 100; i++) {
      Stopwatch sw = Stopwatch.createStarted();
      for (String s : words) {
        WordAnalysis parses = parser.analyze(s);
        c += parses.analysisCount();
      }
      Log.info(sw.elapsed(TimeUnit.MILLISECONDS));
      Log.info(parser.toString());
    }

    Log.info(c);
  }


  @Test
  @Ignore("Not a Test.")
  public void generateWords() throws IOException {
    getStrings();
  }

  private LinkedHashSet<String> getStrings() throws IOException {
    List<String> lines = Files.readAllLines(Paths.get("/media/depo/data/aaa/corpora/dunya.500k"));
    LinkedHashSet<String> words = new LinkedHashSet<>();
    TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;
    for (String line : lines) {
      words.addAll(lexer.tokenizeToStrings(line));
    }
    Log.info("Line count = %d", lines.size());
    Log.info("Unique word count = %d", words.size());
    Files.write(Paths.get("dunya"), words);
    return words;
  }

  @Test
  @Ignore("Not a Test")
  public void readmeExample1() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    WordAnalysis results = morphology.analyze("kalemin");
    results.forEach(s -> System.out.println(s.formatLong()));
  }

  @Test
  @Ignore("Not a Test")
  public void readmeExample2() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    WordAnalysis result = morphology.analyze("kitabımızsa");
    for (SingleAnalysis analysis : result) {
      System.out.println(analysis.formatLong());
      System.out.println("\tStems = " + analysis.getStems());
      System.out.println("\tLemmas = " + analysis.getLemmas());
    }
  }

  @Test
  @Ignore("Not a Test")
  public void disambiguationExample() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    String sentence = "Yarın kar yağacak.";
    System.out.println("Sentence  = " + sentence);
    List<WordAnalysis> analysis = morphology.analyzeSentence(sentence);

    System.out.println("Before disambiguation.");
    for (WordAnalysis entry : analysis) {
      System.out.println("Word = " + entry.getInput());
      for (SingleAnalysis single : entry) {
        System.out.println(single.formatLong());
      }
    }
    System.out.println("\nAfter disambiguation.");
    SentenceAnalysis after = morphology.disambiguate(sentence, analysis);
    after.bestAnalysis().forEach(s -> System.out.println(s.formatLong()));
  }

  @Test
  @Ignore("Not a Test")
  public void removeWordsFromVocabulary() throws IOException {
    Path vocab = Paths.get("/home/ahmetaa/data/zemberek.vocab");
    Path vocabf = Paths.get("/home/ahmetaa/data/zemberek.vocab.filtered");
    Path loc = Paths.get(
        "/home/ahmetaa/projects/zemberek-nlp/morphology/src/main/resources/tr/locations-tr.dict");
    Path prop = Paths.get(
        "/home/ahmetaa/projects/zemberek-nlp/morphology/src/main/resources/tr/proper-from-corpus.dict");
    LinkedHashSet<String> foo = new LinkedHashSet<>(
        Files.readAllLines(vocab, StandardCharsets.UTF_8));
    foo.removeAll(Files.readAllLines(loc));
    foo.removeAll(Files.readAllLines(prop));
    Files.write(vocabf, foo);
  }

}
