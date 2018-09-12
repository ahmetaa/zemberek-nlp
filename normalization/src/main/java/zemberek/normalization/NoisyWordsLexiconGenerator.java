package zemberek.normalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.IntMap;
import zemberek.core.collections.UIntValueMap;
import zemberek.core.logging.Log;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;


/**
 * A modified implementation of Hassan and Menezes's 2013 paper "Social Text Normalization using
 * Contextual Graph Random Walks".
 */
public class NoisyWordsLexiconGenerator {


  public static void main(String[] args) throws Exception {

    NoisyWordsLexiconGenerator generator = new NoisyWordsLexiconGenerator();

    Path corporaRoot = Paths.get("/media/ahmetaa/depo/zemberek/data/corpora");
    Path outRoot = Paths.get("/media/ahmetaa/depo/zemberek/data/normalization/test");
    Path rootList = Paths.get("/media/ahmetaa/depo/zemberek/data/corpora/vocab-list");
    List<String> rootNames = Files.readAllLines(rootList);

    List<Path> roots = new ArrayList<>();
    rootNames.forEach(s -> roots.add(corporaRoot.resolve(s)));

    List<Path> corpora = new ArrayList<>();
    for (Path corpusRoot : roots) {
      corpora.addAll(Files.walk(corpusRoot, 1)
          .filter(s -> s.toFile().isFile())
          .collect(Collectors.toList()));
    }

    Log.info("There are %d corpus files.", corpora.size());

    Files.createDirectories(outRoot);

    // create graph
    generator.buildGraph(outRoot, corpora);
  }

  /**
   * Generates a bipartite graph that represents contextual similarity.
   */
  ContextualSimilarityGraph buildGraph(Path vocabRoot, List<Path> corpora) throws IOException {
    Path correct = vocabRoot.resolve("correct");
    Path incorrect = vocabRoot.resolve("incorrect");

    ContextualSimilarityGraph graph = new ContextualSimilarityGraph(correct, incorrect, 1, 2, 2);

    int i = 0;
    for (Path path : corpora) {
      LinkedHashSet<String> sentences = getSentences(path);
      for (String sentence : sentences) {
        graph.add(sentence);
        if (i % 10_000 == 0) {
          Log.info(i);
        }
        i++;
      }
    }
    Log.info("Context hash count = %d", graph.contextHashCount());
    Log.info("Edge count = %d", graph.edgeCount());
    return graph;
  }

  LinkedHashSet<String> getSentences(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
        .filter(s -> !s.startsWith("<"))
        .map(TextUtil::normalizeSpacesAndSoftHyphens)
        .collect(Collectors.toList());
    return new LinkedHashSet<>(TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines));
  }


  public static final String SENTENCE_START = "<s>";
  public static final String SENTENCE_END = "</s>";

  public class ContextualSimilarityGraph {

    IntMap<IntIntMap> contextHashToWordCounts = new IntMap<>();
    IntIntMap[] wordToContexts;

    List<String> words;
    UIntValueMap<String> indexes = new UIntValueMap<>();
    int outWordStart;
    int contextSize;

    public ContextualSimilarityGraph(
        Path correct,
        Path incorrect,
        int contextSize,
        int correctMinCount,
        int incorrectMinCount) throws IOException {

      if (contextSize < 1 || contextSize > 2) {
        throw new IllegalArgumentException("Context must be 1 or 2 but it is " + contextSize);
      }
      this.contextSize = contextSize;

      Histogram<String> inWords = Histogram.loadFromUtf8File(correct, ' ');
      Histogram<String> outWords = Histogram.loadFromUtf8File(incorrect, ' ');
      inWords.removeSmaller(correctMinCount);
      outWords.removeSmaller(incorrectMinCount);
      this.outWordStart = inWords.size();
      this.words = new ArrayList<>(inWords.getSortedList());
      words.addAll(outWords.getSortedList());
      int i = 0;
      for (String word : words) {
        indexes.put(word, i);
        i++;
      }
      wordToContexts = new IntIntMap[words.size()];

    }

    void add(String sentence) {

      List<String> tokens = new ArrayList<>();
      for (int i = 0; i < contextSize; i++) {
        tokens.add(SENTENCE_START);
      }
      sentence = sentence.toLowerCase(Turkish.LOCALE);
      tokens.addAll(TurkishTokenizer.DEFAULT.tokenizeToStrings(sentence));
      for (int i = 0; i < contextSize; i++) {
        tokens.add(SENTENCE_END);
      }
      String[] context = new String[contextSize * 2];
      for (int i = contextSize; i < tokens.size() - contextSize; i++) {

        // if current word is out of vocabulary, continue.
        int wordIndex = indexes.get(tokens.get(i));
        if (wordIndex == -1) {
          continue;
        }

        // gather context and calculate hash
        if (contextSize == 1) {
          context[0] = tokens.get(i - 1);
          context[1] = tokens.get(i + 1);
        } else {
          context[0] = tokens.get(i - 2);
          context[1] = tokens.get(i - 1);
          context[2] = tokens.get(i + 1);
          context[3] = tokens.get(i + 2);
        }
        int hash = hash(context);

        // update context -> word counts
        IntIntMap wordCounts = contextHashToWordCounts.get(hash);
        if (wordCounts == null) {
          wordCounts = new IntIntMap(1);
          contextHashToWordCounts.put(hash, wordCounts);
        }
        wordCounts.increment(wordIndex, 1);

        // update word -> context counts.
        IntIntMap contextCounts = wordToContexts[wordIndex];
        if (contextCounts == null) {
          contextCounts = new IntIntMap(1);
          wordToContexts[wordIndex] = contextCounts;
        }
        contextCounts.increment(hash, 1);
      }
    }

    int contextHashCount() {
      return contextHashToWordCounts.size();
    }

    long edgeCount() {
      long i = 0;
      for (IntIntMap m : contextHashToWordCounts) {
        i += m.size();
      }
      return i;
    }

  }

  private static int hash(String[] context) {
    int d = 0x811C9DC5;
    for (String s : context) {
      for (int i = 0; i < s.length(); i++) {
        d = (d ^ s.charAt(i)) * 16777619;
      }
    }
    return d & 0x7fffffff;
  }

}
