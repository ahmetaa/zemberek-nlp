package zemberek.normalization;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import zemberek.core.IntPair;
import zemberek.core.collections.Histogram;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.UIntMap;
import zemberek.core.collections.UIntValueMap;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
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

    Path corporaRoot = Paths.get("/home/aaa/data/corpora");
    Path outRoot = Paths.get("/home/aaa/data/normalization/test");
    Path rootList = Paths.get("/home/aaa/data/corpora/vocab-list");
    List<String> rootNames = TextIO.loadLines(rootList, "#");

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

    Path correct = outRoot.resolve("correct");
    Path incorrect = outRoot.resolve("incorrect");

    NormalizationVocabulary vocabulary = new NormalizationVocabulary(
        correct, incorrect, 2, 3
    );

    // create graph
    Path graphPath = outRoot.resolve("graph");
    //generator.buildGraph(vocabulary, corpora, graphPath);

    // create Random Walker
    Log.info("Constructing random walk graph from %s", graphPath);
    RandomWalker walker = RandomWalker.fromGraphFile(vocabulary, graphPath);

    Log.info("Collecting candidates data.");
    Multimap<String, String> allPossibilities = walker.walk(100, 4);
    Path allCandidates = outRoot.resolve("all-candidates");
    try (PrintWriter pw = new PrintWriter(allCandidates.toFile(), "utf-8")) {
      for (String s : allPossibilities.keySet()) {
        pw.println(s + "=" + String.join(" ", allPossibilities.get(s)));
        pw.println();
      }
    }

    Log.info("Done");

  }

  /**
   * Generates a bipartite graph that represents contextual similarity.
   */
  ContextualSimilarityGraph buildGraph(
      NormalizationVocabulary vocabulary,
      List<Path> corpora,
      Path graphPath) throws IOException {

    ContextualSimilarityGraph graph = new ContextualSimilarityGraph(vocabulary, 1);

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

    Log.info("Serializing graph for random walk structure.");
    graph.serializeForRandomWalk(graphPath);
    Log.info("Serialized to %s", graphPath);

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

  private static class RandomWalkNode {

    int[] keysCounts;
    int totalCounts;

    public RandomWalkNode(int[] keysCounts, int totalCounts) {
      this.keysCounts = keysCounts;
      this.totalCounts = totalCounts;
    }
  }

  private static class RandomWalker {

    UIntMap<RandomWalkNode> contextHashesToWords;
    UIntMap<RandomWalkNode> wordsToContextHashes;
    NormalizationVocabulary vocabulary;
    private static final Random rnd = new Random(1);


    public RandomWalker(
        NormalizationVocabulary vocabulary,
        UIntMap<RandomWalkNode> contextHashesToWords,
        UIntMap<RandomWalkNode> wordsToContextHashes) {
      this.vocabulary = vocabulary;
      this.contextHashesToWords = contextHashesToWords;
      this.wordsToContextHashes = wordsToContextHashes;
    }

    static RandomWalker fromGraphFile(NormalizationVocabulary vocabulary, Path path)
        throws IOException {
      try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
        UIntMap<RandomWalkNode> contextHashesToWords = loadNodes(dis);
        UIntMap<RandomWalkNode> wordsToContextHashes = loadNodes(dis);
        return new RandomWalker(vocabulary, contextHashesToWords, wordsToContextHashes);
      }
    }

    private static UIntMap<RandomWalkNode> loadNodes(DataInputStream dis) throws IOException {
      int nodeCount = dis.readInt();
      Log.info("There are %d nodes.", nodeCount);
      UIntMap<RandomWalkNode> edgeMap = new UIntMap<>(nodeCount / 2);
      for (int i = 0; i < nodeCount; i++) {
        int key = dis.readInt();
        int size = dis.readInt();
        int[] keysCounts = new int[size * 2];
        int totalCount = 0;
        for (int j = 0; j < size * 2; j++) {
          int val = dis.readInt();
          keysCounts[j] = val;
          if ((j & 0x01) == 1) {
            totalCount += val;
          }
        }
        edgeMap.put(key, new RandomWalkNode(keysCounts, totalCount));
        if (i > 0 && i % 500_000 == 0) {
          Log.info("%d node data loaded.", i);
        }
      }
      return edgeMap;
    }

    Multimap<String, String> walk(int walkCount, int maxHopCount) {
      Multimap<String, String> allPossibilities = HashMultimap.create();

      int c = 0;
      for (int wordIndex : wordsToContextHashes.getKeys()) {
        // for all noisy words
        if (vocabulary.isValid(wordIndex)) {
          continue;
        }
        if (c > 0 && c % 10_000 == 0) {
          Log.info("Candidates for %d words collected.");
        }
        c++;
        int node = wordIndex;
        for (int i = 0; i < walkCount; i++) {
          boolean atWordNode = true;
          for (int j = 0; j < maxHopCount; j++) {
            node = atWordNode ?
                selectFromDistribution(wordsToContextHashes.get(node)) :
                selectFromDistribution(contextHashesToWords.get(node));
            atWordNode = !atWordNode;
            if (atWordNode && node != wordIndex && vocabulary.isValid(node)) {
              allPossibilities.put(vocabulary.getWord(wordIndex), vocabulary.getWord(node));
              break;
            }

          }
        }
      }
      return allPossibilities;
    }

    int selectFromDistribution(RandomWalkNode node) {
      int rollDice = rnd.nextInt(node.totalCounts);
      int accumulator = 0;
      int[] keysCounts = node.keysCounts;
      for (int i = 0; i < keysCounts.length; i += 2) {
        accumulator += keysCounts[i + 1];
        if (accumulator >= rollDice) {
          return keysCounts[i];
        }
      }
      throw new IllegalStateException("Unreachable.");
    }

  }

  public static class NormalizationVocabulary {

    List<String> words;
    UIntValueMap<String> indexes = new UIntValueMap<>();
    int outWordStart;

    public NormalizationVocabulary(
        Path correct,
        Path incorrect,
        int correctMinCount,
        int incorrectMinCount) throws IOException {

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
    }

    int totalSize() {
      return words.size();
    }

    boolean isValid(int id) {
      return id >= 0 && id < outWordStart && id < totalSize();
    }

    int getIndex(String word) {
      return indexes.get(word);
    }

    String getWord(int id) {
      return words.get(id);
    }

  }


  public class ContextualSimilarityGraph {

    UIntMap<IntIntMap> contextHashToWordCounts = new UIntMap<>(100_000);
    UIntMap<IntIntMap> wordToContexts = new UIntMap<>(100_000);

    NormalizationVocabulary vocabulary;

    int contextSize;

    public ContextualSimilarityGraph(
        NormalizationVocabulary vocabulary,
        int contextSize) {

      if (contextSize < 1 || contextSize > 2) {
        throw new IllegalArgumentException("Context must be 1 or 2 but it is " + contextSize);
      }
      this.contextSize = contextSize;
      this.vocabulary = vocabulary;
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
        int wordIndex = vocabulary.getIndex(tokens.get(i));
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
        IntIntMap contextCounts = wordToContexts.get(wordIndex);
        if (contextCounts == null) {
          contextCounts = new IntIntMap(1);
          wordToContexts.put(wordIndex, contextCounts);
        }
        contextCounts.increment(hash, 1);
      }
    }

    void serializeForRandomWalk(Path p) throws IOException {
      try (DataOutputStream dos = IOUtil.getDataOutputStream(p)) {
        serializeEdges(dos, contextHashToWordCounts);
        serializeEdges(dos, wordToContexts);
      }
    }

    private void serializeEdges(DataOutputStream dos, UIntMap<IntIntMap> edgesMap)
        throws IOException {

      dos.writeInt(edgesMap.size());

      for (int wordIndex : edgesMap.getKeys()) {
        dos.writeInt(wordIndex);
        IntIntMap map = edgesMap.get(wordIndex);
        if (map == null) {
          throw new IllegalStateException("edge map is null!");
        }
        dos.writeInt(map.size());
        IntPair[] pairs = map.getAsPairs();
        Arrays.sort(pairs, (a, b) -> Integer.compare(b.second, a.second));
        for (IntPair pair : pairs) {
          dos.writeInt(pair.first);
          dos.writeInt(pair.second);
        }
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
