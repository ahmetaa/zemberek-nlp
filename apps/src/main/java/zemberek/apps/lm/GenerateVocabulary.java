package zemberek.apps.lm;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import zemberek.apps.ConsoleApp;
import zemberek.core.SpaceTabTokenizer;
import zemberek.core.collections.Histogram;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.logging.Log;

public class GenerateVocabulary extends ConsoleApp {

  @Parameter(names = {"-corpus"},
      description = "Text corpus file path.",
      required = true)
  File corpus;

  @Parameter(names = {"-include"},
      description = "A file that contains a word per line. All words in this file will be added to vocabulary.")
  File includeFile;

  @Parameter(names = {"-exclude"},
      description = "A file that contains a word per line. All words in this file will be removed from vocabulary.")
  File excludeFile;

  @Parameter(names = {"-top"},
      description = "Size of the resulting vocabulary. Most frequent n words will be kept. " +
          "However, if include and exclude words are defined, they will be processed after top operation.")
  int top = -1;

  @Parameter(names = {"-outFile"},
      description = "path of the output file. If not given, a file named [vocabulary] will be generated in working path.",
      required = true)
  File outFile = null;

  @Parameter(names = {"-sorted"},
      description = "If used, vocabulary file will be ordered by words. Otherwise vocabulary is ordered by frequency.")
  boolean ordered = false;

  @Parameter(names = {"-sortLocale"},
      description = "Locale of to be used the orderText corpus file path. By default English locale is used. This value is applied only if -sorted flag is used.")
  String sortLocale;

  public static void main(String[] args) {
    new GenerateVocabulary().execute(args);
  }

  @Override
  public String description() {
    return "Generates vocabulary from a given corpus.";
  }

  @Override
  public void run() throws Exception {
    if (!corpus.exists()) {
      throw new IllegalArgumentException("Can not find the corpus file: " + corpus);
    }
    if (top < -1 || top == 0) {
      throw new IllegalArgumentException("Illegal value for n: " + top);
    }

    Set<String> wordsToInclude = getWordsFromFile(includeFile);
    Log.info("Amount of words to include using include file: %d", wordsToInclude.size());
    Set<String> wordsToExclude = getWordsFromFile(excludeFile);
    Log.info("Amount of words to exclude using exclude file: %d", wordsToExclude.size());

    Set<String> intersection = Sets.newHashSet(wordsToExclude);
    intersection.retainAll(wordsToInclude);

    if (intersection.size() != 0) {
      Log.warn(
          "There are matching words in both include and exclude files: " + intersection.toString());
    }

    Collator collator = Collator.getInstance(Locale.ENGLISH);
    if (sortLocale != null) {
      collator = Collator.getInstance(new Locale(sortLocale));
    }
    Log.info("Processing corpus: %s", corpus);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(corpus), "utf-8"))) {
      String line;
      Histogram<String> histogram = new Histogram<>(50000);
      SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();
      int count = 0;
      while ((line = reader.readLine()) != null) {
        List<String> words = Lists.newArrayList(tokenizer.split(line));
        if (words.isEmpty()) {
          continue;
        }
        histogram.add(words);
        if (count % 500000 == 0 && count != 0) {
          Log.info("%d lines processed. Vocabulary Size: %d", count, histogram.size());
        }
        count++;
      }
      Log.info("A total of %d lines have been processed. Vocabulary Size: %d", count,
          histogram.size());

      if (top >= histogram.size()) {
        top = histogram.size();
      } else {
        Log.info("Top %d words will be used.", top);
      }

      List<String> mostFrequent = histogram.getTop(top);
      Log.info("Coverage: %.3f",
          100d * ((double) histogram.totalCount(mostFrequent)) / histogram.totalCount());

      LinkedHashSet<String> resultSet = Sets.newLinkedHashSet(mostFrequent);
      resultSet.addAll(wordsToInclude);
      resultSet.removeAll(wordsToExclude);

      List<String> result = Lists.newArrayList(resultSet);
      Log.info("Total size of vocabulary: %d", result.size());
      if (ordered) {
        Log.info("Sorting file with word order.");
        result.sort(collator);
      }
      com.google.common.io.Files.createParentDirs(outFile);
      Log.info("Saving to vocabulary file: %s", outFile);
      SimpleTextWriter.utf8Builder(outFile).addNewLineBeforClose().build().writeLines(result);
      Log.info("Done.");
    }
  }

  private Set<String> getWordsFromFile(File file) throws IOException {
    if (file != null) {
      if (!file.exists()) {
        throw new IllegalArgumentException("Can not find the include file: " + file);
      }
      if (!file.isFile()) {
        throw new IllegalArgumentException("Include file is not a file: " + file);
      }
      return Sets.newHashSet(SimpleTextReader.trimmingUTF8Reader(file).asStringList());
    } else {
      return Collections.emptySet();
    }
  }
}