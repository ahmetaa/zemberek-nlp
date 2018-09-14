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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.IntPair;
import zemberek.core.collections.Histogram;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.IntVector;
import zemberek.core.collections.UIntMap;
import zemberek.core.collections.UIntValueMap;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.text.TextUtil;
import zemberek.core.turkish.Turkish;
import zemberek.normalization.NormalizationVocabularyGenerator.Vocabulary;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

/**
 * A modified implementation of Hassan and Menezes's 2013 paper "Social Text Normalization using
 * Contextual Graph Random Walks". Here when building contextual similarity graph, we use hash
 * values of the contexts instead of the contexts itself.
 */
public class NoisyWordsLexiconGenerator {


  public static void main(String[] args) throws Exception {

    NoisyWordsLexiconGenerator generator = new NoisyWordsLexiconGenerator();

    Path corporaRoot = Paths.get("/media/ahmetaa/depo/zemberek/data/corpora");
    Path outRoot = Paths.get("/media/ahmetaa/depo/zemberek/data/normalization/test");
    Path rootList = Paths.get("/media/ahmetaa/depo/zemberek/data/corpora/vocab-list");
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
    ContextualSimilarityGraph graph = generator.buildGraph(vocabulary, corpora, 1);
    Log.info("Context hash count = %d", graph.contextHashCount());
    Log.info("Edge count = %d", graph.edgeCount());

    Log.info("Serializing graph for random walk structure.");
    graph.serializeForRandomWalk(graphPath);
    Log.info("Serialized to %s", graphPath);

    // create Random Walker
    Log.info("Constructing random walk graph from %s", graphPath);
    RandomWalker walker = RandomWalker.fromGraphFile(vocabulary, graphPath);

    Log.info("Collecting candidates data.");
    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    WalkResult walkResult = walker.walk(100, 6, threadCount);
    Path allCandidates = outRoot.resolve("all-candidates");
    try (PrintWriter pw = new PrintWriter(allCandidates.toFile(), "utf-8")) {
      for (String s : walkResult.allCandidates.keySet()) {
        pw.println(s + "=" + String.join(" ", walkResult.allCandidates.get(s)));
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
      int contextSize) throws IOException {

    ContextualSimilarityGraph graph = new ContextualSimilarityGraph(vocabulary, contextSize);

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
    return graph;
  }

  private LinkedHashSet<String> getSentences(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
        .filter(s -> !s.startsWith("<"))
        .map(TextUtil::normalizeSpacesAndSoftHyphens)
        .collect(Collectors.toList());
    return new LinkedHashSet<>(TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines));
  }

  private static final String SENTENCE_START = "<s>";
  private static final String SENTENCE_END = "</s>";

  static class RandomWalkNode {

    int[] keysCounts;
    int totalCounts;

    RandomWalkNode(int[] keysCounts, int totalCounts) {
      this.keysCounts = keysCounts;
      this.totalCounts = totalCounts;
    }
  }

  static class RandomWalker {

    UIntMap<RandomWalkNode> contextHashesToWords;
    UIntMap<RandomWalkNode> wordsToContextHashes;
    NormalizationVocabulary vocabulary;
    private static final Random rnd = new Random(1);


    RandomWalker(
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

    WalkResult walk(int walkCount, int maxHopCount, int threadCount)
        throws Exception {
      List<Work> workList = new ArrayList<>();
      int bathSize = 5_000;
      IntVector vector = new IntVector(bathSize);
      for (int wordIndex : wordsToContextHashes.getKeys()) {
        // only noisy words
        if (vocabulary.isValid(wordIndex)) {
          continue;
        }
        vector.add(wordIndex);
        if (vector.size() == bathSize) {
          workList.add(new Work(vector.copyOf()));
          vector = new IntVector(bathSize);
        }
      }
      // for remaining data.
      if (vector.size() > 0) {
        workList.add(new Work(vector.copyOf()));
      }

      ExecutorService executorService = new BlockingExecutor(threadCount);
      CompletionService<WalkResult> service =
          new ExecutorCompletionService<>(executorService);

      for (Work work : workList) {
        service.submit(new WalkTask(
            this,
            work,
            walkCount,
            maxHopCount
        ));
      }
      executorService.shutdown();

      int workCount = 0;
      WalkResult result = new WalkResult();
      while (workCount < workList.size()) {
        WalkResult threadResult = service.take().get();
        result.allCandidates.putAll(threadResult.allCandidates);
        Log.info("%d words processed.", result.allCandidates.size());
        workCount++;
      }
      return result;
    }

    int selectFromDistribution(RandomWalkNode node) {
      int dice = rnd.nextInt(node.totalCounts + 1);
      int accumulator = 0;
      int[] keysCounts = node.keysCounts;
      for (int i = 0; i < keysCounts.length; i += 2) {
        accumulator += keysCounts[i + 1];
        if (accumulator >= dice) {
          return keysCounts[i];
        }
      }
      throw new IllegalStateException("Unreachable.");
    }
  }

  static class WalkResult {

    Multimap<String, String> allCandidates = HashMultimap.create();
  }

  private static class Work {

    int[] wordIndexes;

    Work(int[] wordIndexes) {
      this.wordIndexes = wordIndexes;
    }
  }

  private static class WalkTask implements Callable<WalkResult> {

    RandomWalker walker;
    Work work;
    int walkCount;
    int maxHopCount;

    WalkTask(RandomWalker walker, Work work, int walkCount, int maxHopCount) {
      this.walker = walker;
      this.work = work;
      this.walkCount = walkCount;
      this.maxHopCount = maxHopCount;
    }

    @Override
    public WalkResult call() {
      WalkResult result = new WalkResult();
      for (int wordIndex : work.wordIndexes) {
        // Only noisy words. Just to be sure.
        if (walker.vocabulary.isValid(wordIndex)) {
          continue;
        }
        for (int i = 0; i < walkCount; i++) {
          int nodeIndex = wordIndex;
          boolean atWordNode = true;
          for (int j = 0; j < maxHopCount; j++) {

            RandomWalkNode node = atWordNode ?
                walker.wordsToContextHashes.get(nodeIndex) :
                walker.contextHashesToWords.get(nodeIndex);

            if (node == null) {
              System.out.println();
            }

            nodeIndex = walker.selectFromDistribution(node);

            atWordNode = !atWordNode;
            if (atWordNode && nodeIndex != wordIndex && walker.vocabulary.isValid(nodeIndex)) {
              result.allCandidates.put(
                  walker.vocabulary.getWord(wordIndex),
                  walker.vocabulary.getWord(nodeIndex));
              break;
            }
          }
        }
      }
      return result;
    }
  }

  static class NormalizationVocabulary {

    List<String> words;
    UIntValueMap<String> indexes = new UIntValueMap<>();
    int outWordStart;

    NormalizationVocabulary(
        Vocabulary vocabulary,
        int correctMinCount,
        int incorrectMinCount) {
      Histogram<String> inWords = vocabulary.correct;
      Histogram<String> outWords = vocabulary.incorrect;
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

    NormalizationVocabulary(
        Path correct,
        Path incorrect,
        int correctMinCount,
        int incorrectMinCount) throws IOException {
      this(new Vocabulary(
              Histogram.loadFromUtf8File(correct, ' '),
              Histogram.loadFromUtf8File(incorrect, ' '),
              new Histogram<>()),
          correctMinCount, incorrectMinCount);
    }

    int totalSize() {
      return words.size();
    }

    boolean isValid(int id) {
      return id >= 0 && id < outWordStart && id < totalSize();
    }

    boolean isValid(String id) {
      return isValid(getIndex(id));
    }

    int getIndex(String word) {
      return indexes.get(word);
    }

    String getWord(int id) {
      return words.get(id);
    }
  }

  class ContextualSimilarityGraph {

    UIntMap<IntIntMap> contextHashToWordCounts = new UIntMap<>();
    UIntMap<IntIntMap> wordToContexts = new UIntMap<>();

    NormalizationVocabulary vocabulary;

    int contextSize;

    ContextualSimilarityGraph(
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
      List<Token> raw = TurkishTokenizer.DEFAULT.tokenize(sentence);
      for (Token token : raw) {
        if(token.getType()==TurkishLexer.Punctuation) {
          continue;
        }
        String text = token.getText();
        if(token.getType()==TurkishLexer.Time ||
            token.getType()==TurkishLexer.PercentNumeral ||
            token.getType()==TurkishLexer.Number ||
            token.getType()==TurkishLexer.Date) {
          text = text.replaceAll("[0-9]+","_d");
        }
        tokens.add(text);
      }

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

      for (int nodeIndex : edgesMap.getKeys()) {
        dos.writeInt(nodeIndex);
        IntIntMap map = edgesMap.get(nodeIndex);
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

  static int hash(String... context) {
    int d = 0x811C9DC5;
    for (String s : context) {
      for (int i = 0; i < s.length(); i++) {
        d = (d ^ s.charAt(i)) * 16777619;
      }
    }
    return d & 0x7fffffff;
  }

}
