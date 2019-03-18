package zemberek.normalization;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import zemberek.core.IntPair;
import zemberek.core.collections.Histogram;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.IntVector;
import zemberek.core.collections.UIntMap;
import zemberek.core.collections.UIntSet;
import zemberek.core.collections.UIntValueMap;
import zemberek.core.concurrency.BlockingExecutor;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.math.LogMath;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.text.distance.CharDistance;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;

/**
 * A modified implementation of Hassan and Menezes's 2013 paper "Social Text Normalization using
 * Contextual Graph Random Walks".
 * <p></p>
 * Algorithm basically works like this:
 * <p></p>
 * First, we need to have two vocabularies from a corpus. One vocabulary is correct words, other is
 * noisy words. This operation is actually quite tricky as how to decide if a word is noisy is not
 * easy. For Turkish we use morphological analysis but it may actually fail for some proper nouns
 * and for inputs where Turkish characters are not used. For example in sentence "öle olmaz" word
 * "öle" passes morphological analysis but it is actually "öyle".
 * <p></p>
 * Second, a bipartite graph is generated from the corpus. There are two sides in the graph. One
 * represents contexts, other represents words. For example:
 * <pre>
 *  context(bu * eve) -> (sabah:105, zabah:3, akşam:126, aksam:7, mavi:2 ...)
 *  context(sabah * geldim) -> (eve:56, işe:78, okula:64, okulua:2 ...)
 *  word(sabah) -> ([bu eve]:105, [bu kahvaltıda]:23, [her olmaz]:7 ...)
 *  word(zabah) -> ([bu eve]:3 ...)
 * </pre>
 * (bu * eve) represents a context. And sabah:105 means from this context, "sabah" appeared in the
 * middle 105 times. And noisy "zabah" appeared 3 times.
 * <p></p>
 * Here we do something different from original paper, when building contextual similarity graph, we
 * use 32 bit hash values of the contexts instead of the contexts itself. This reduces memory and
 * calculation cost greatly.
 * <p></p>
 * After this graph is constructed, For every noisy word in the graph several random walks are done
 * as below:
 * <pre>
 * - Start from a noisy word.
 * - repeat k times (such as k = 100)
 * -- Select one of the contexts of this word randomly. But, random is not uniform.
 *   Context is selected proportional to occurrence counts.
 * -- From context, similarly randomly hop to a word.
 * -- If word is noisy, continue hops.
 * -- If word is not noisy or hop count reaches to a certain value (such as 4), stop.
 *    Store average-hitting-time
 * - Calculate contextual and lexical similarity and prune. Lexical similarity is calculated
 *   with modified edit distance and longest common substring ratio (from Contractor et al. 2010
 *   Unsupervised cleansing of noisy text)
 *
 * </pre>
 * If random walks are repeated for many times, All candidates that may be the correct version can
 * be collected. After that, a Viterbi search using a language model can be performed for better
 * accuracy.
 */
public class NoisyWordsLexiconGenerator {

  public static final double LOG_BASE_FOR_COUNTS = 1.4;
  public static final int WALK_COUNT = 300;
  public static final int MAX_HOP_COUNT = 7;
  public static final double OVERALL_SCORE_THRESHOLD = 0.41;
  public static final double LEXICAL_SIMLARITY_SCORE_THRESHOLD = 0.4;
  public static final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  NormalizationVocabulary vocabulary;
  int threadCount;

  public NoisyWordsLexiconGenerator(
      NormalizationVocabulary vocabulary, int threadCount) {
    this.vocabulary = vocabulary;
    this.threadCount = threadCount;
  }

  public static void main(String[] args) throws Exception {

    int threadCount = Runtime.getRuntime().availableProcessors() / 2;
    if (threadCount > 22) {
      threadCount = 22;
    }

    Path corporaRoot = Paths.get("/home/aaa/data/normalization/corpus");
    Path workDir = Paths.get("/home/aaa/data/normalization/test-large");
    Path corpusDirList = corporaRoot.resolve("all-list");

    Files.createDirectories(workDir);

    Path correct = workDir.resolve("correct");
    Path incorrect = workDir.resolve("incorrect");
    Path maybeIncorrect = workDir.resolve("possibly-incorrect");

    NormalizationVocabulary vocabulary = new NormalizationVocabulary(
        correct, incorrect, maybeIncorrect, 1, 3, 1);

    NoisyWordsLexiconGenerator generator = new NoisyWordsLexiconGenerator(vocabulary, threadCount);

    BlockTextLoader corpusProvider = BlockTextLoader
        .fromDirectoryRoot(corporaRoot, corpusDirList, 50_000);

    // create graph
    Path graphPath = workDir.resolve("graph");
    generator.createGraph(corpusProvider, graphPath);

    Histogram<String> incorrectWords = Histogram.loadFromUtf8File(incorrect, ' ');
    incorrectWords.add(Histogram.loadFromUtf8File(maybeIncorrect, ' '));
    generator.createCandidates(graphPath, workDir, incorrectWords);

    Log.info("Done");
  }

  void createCandidates(Path graphPath, Path outRoot, Histogram<String> noisyWords)
      throws Exception {
    Stopwatch sw = Stopwatch.createStarted();

    // create Random Walker
    Log.info("Constructing random walk graph from %s", graphPath);
    RandomWalker walker = RandomWalker.fromGraphFile(vocabulary, graphPath);

    Log.info("Collecting candidates data.");

    WalkResult walkResult = walker.walk(WALK_COUNT, MAX_HOP_COUNT, threadCount);
    Path allCandidates = outRoot.resolve("all-candidates");
    Path lookup = outRoot.resolve("lookup-from-graph");

    Log.info("Saving candidates.");

    try (PrintWriter pw = new PrintWriter(allCandidates.toFile(), "utf-8");
        PrintWriter pwLookup = new PrintWriter(lookup.toFile(), "utf-8")) {
      List<String> words = new ArrayList<>(walkResult.allCandidates.keySet());

      words.sort((a, b) -> Integer.compare(noisyWords.getCount(b), noisyWords.getCount(a)));

      for (String s : words) {
        List<WalkScore> scores = new ArrayList<>(walkResult.allCandidates.get(s));
        float lambda1 = 1f;
        float lambda2 = 1f;
        scores.sort(
            (a, b) -> Float.compare(b.getScore(lambda1, lambda2), a.getScore(lambda1, lambda2)));
        scores = scores.stream()
            .filter(w -> w.getScore(lambda1, lambda2) >= OVERALL_SCORE_THRESHOLD)
            .collect(Collectors.toList());
        pw.println(s);
        for (WalkScore score : scores) {
          pw.println(String.format("%s:%.3f (%.3f - %.3f)",
              score.candidate,
              score.getScore(lambda1, lambda2),
              score.contextualSimilarity,
              score.lexicalSimilarity));
        }
        pw.println();

        if (scores.size() == 0) {
          continue;
        }

        List<String> candidates = new ArrayList<>();
        for (WalkScore score : scores) {
          if (score.candidate.equals(s)) {
            continue;
          }
          // if there is an ascii equivalent (but not the same), only return that.
          if (vocabulary.isMaybeIncorrect(s) &&
              alphabet.toAscii(s).equals(alphabet.toAscii(score.candidate))) {
            candidates = new ArrayList<>(1);
            candidates.add(score.candidate);
            break;
          }

          if (score.lexicalSimilarity * lambda2 < LEXICAL_SIMLARITY_SCORE_THRESHOLD) {
            continue;
          }
          candidates.add(score.candidate);
        }

        if (candidates.size() > 0 && vocabulary.isMaybeIncorrect(s) && !candidates.contains(s)) {
          candidates.add(s);
        }
        if (!candidates.isEmpty()) {
          pwLookup.println(s + "=" + String.join(",", candidates));
        }
      }
    }
    Log.info("Candidates collected in %.3f seconds.",
        sw.elapsed(TimeUnit.MILLISECONDS) / 1000d);
  }

  void createGraph(BlockTextLoader corpusProvider, Path graphPath) throws Exception {
    Stopwatch sw = Stopwatch.createStarted();

    ContextualSimilarityGraph graph = buildGraph(corpusProvider, 1);
    Log.info("Serializing graph for random walk structure.");
    graph.serializeForRandomWalk(graphPath);
    Log.info("Serialized to %s", graphPath);
    Log.info("Graph created in %.3f seconds.",
        sw.elapsed(TimeUnit.MILLISECONDS) / 1000d);
  }

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

    ReentrantLock lock = new ReentrantLock();
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
        UIntMap<RandomWalkNode> contextHashesToWords = loadNodes(dis, "context");
        UIntMap<RandomWalkNode> wordsToContextHashes = loadNodes(dis, "word");
        return new RandomWalker(vocabulary, contextHashesToWords, wordsToContextHashes);
      }
    }

    private static UIntMap<RandomWalkNode> loadNodes(DataInputStream dis, String info)
        throws IOException {
      int nodeCount = dis.readInt();
      Log.info("There are %d %s nodes.", nodeCount, info);
      UIntMap<RandomWalkNode> edgeMap = new UIntMap<>(nodeCount / 2);
      for (int i = 0; i < nodeCount; i++) {
        int key = dis.readInt();
        int size = dis.readInt();
        int[] keysCounts = new int[size * 2];
        int totalCount = 0;
        for (int j = 0; j < size * 2; j++) {
          int val = dis.readInt();

          if ((j & 0x01) == 1) {
            val = (int) LogMath.log(LOG_BASE_FOR_COUNTS, val);
            if (val <= 0) {
              val = 1;
            }
            totalCount += val;
          }
          keysCounts[j] = val;

        }
        edgeMap.put(key, new RandomWalkNode(keysCounts, totalCount));
        if (i > 0 && i % 500_000 == 0) {
          Log.info("%d %s node loaded.", i, info);
        }
      }
      return edgeMap;
    }

    WalkResult walk(int walkCount, int maxHopCount, int threadCount) throws Exception {

      // prepare work items for threads. Each work item contains 5000 words.
      List<Work> workList = new ArrayList<>();
      int batchSize = 5_000;
      IntVector vector = new IntVector(batchSize);
      for (int wordIndex : wordsToContextHashes.getKeys()) {
        // only noisy or maybe-noisy words
        if (vocabulary.isCorrect(wordIndex)) {
          continue;
        }
        vector.add(wordIndex);
        if (vector.size() == batchSize) {
          workList.add(new Work(vector.copyOf()));
          vector = new IntVector(batchSize);
        }
      }
      // for remaining data.
      if (vector.size() > 0) {
        workList.add(new Work(vector.copyOf()));
      }

      WalkResult globalResult = new WalkResult();

      ExecutorService executorService = new BlockingExecutor(threadCount);
      for (Work work : workList) {
        executorService.submit(() -> {
          WalkResult result = new WalkResult();
          CharDistance distanceCalculator = new CharDistance();
          for (int wordIndex : work.wordIndexes) {
            // Only incorrect and maybe-incorrect words. Check anyway, to be sure.
            if (vocabulary.isCorrect(wordIndex)) {
              continue;
            }

            Map<String, WalkScore> scores = new HashMap<>();

            for (int i = 0; i < walkCount; i++) {
              int nodeIndex = wordIndex;
              boolean atWordNode = true;
              for (int j = 0; j < maxHopCount; j++) {

                RandomWalkNode node = atWordNode ?
                    wordsToContextHashes.get(nodeIndex) :
                    contextHashesToWords.get(nodeIndex);

                nodeIndex = selectFromDistribution(node);

                atWordNode = !atWordNode;

                // if we reach to a valid word ([...] --> [Context node] --> [Valid word node] )
                boolean maybeIncorrect = vocabulary.isMaybeIncorrect(nodeIndex);
                if (atWordNode && (nodeIndex != wordIndex || maybeIncorrect)
                    && (vocabulary.isCorrect(nodeIndex) || maybeIncorrect)) {
                  String word = vocabulary.getWord(nodeIndex);
                  WalkScore score = scores.get(word);
                  if (score == null) {
                    score = new WalkScore(word);
                    scores.put(word, score);
                  }
                  score.update(j + 1);
                  break;
                }
              }
            }

            // calculate contextual similarity probabilities.
            float totalAverageHittingTime = 0;
            for (WalkScore score : scores.values()) {
              totalAverageHittingTime += score.getAverageHittingTime();
            }

            for (String s : scores.keySet()) {
              WalkScore score = scores.get(s);
              score.contextualSimilarity =
                  score.getAverageHittingTime() /
                      (totalAverageHittingTime - score.getAverageHittingTime());
            }

            // calculate lexical similarity cost. This is slow for now.
            // convert to ascii and remove vowels and repetitions.
            String word = vocabulary.getWord(wordIndex);
            String reducedSource = reduceWord(word);
            String asciiSource = TurkishAlphabet.INSTANCE.toAscii(word);

            for (String s : scores.keySet()) {
              String reducedTarget = reduceWord(s);
              String asciiTarget = TurkishAlphabet.INSTANCE.toAscii(s);
              float editDistance =
                  (float) distanceCalculator.distance(reducedSource, reducedTarget) + 1;
              float asciiEditDistance =
                  (float) distanceCalculator.distance(asciiSource, asciiTarget) + 1;

              // longest commons substring ratio
              float lcsr = longestCommonSubstring(asciiSource, asciiTarget, true).length() * 1f /
                  Math.max(s.length(), word.length());

              WalkScore score = scores.get(s);
              float l1 = lcsr / editDistance;
              float l2 = lcsr / asciiEditDistance;

              score.lexicalSimilarity = Math.max(l1, l2);
            }
            result.allCandidates.putAll(word, scores.values());
          }
          try {
            lock.lock();
            globalResult.allCandidates.putAll(result.allCandidates);
            Log.info("%d words processed.", globalResult.allCandidates.keySet().size());
          } finally {
            lock.unlock();
          }
        });
      }
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.DAYS);

      return globalResult;
    }

    /**
     * From a node, selects a connected node randomly. Randomness is not uniform, it is proportional
     * to the occurrence counts attached to the edges.
     */
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

    Multimap<String, WalkScore> allCandidates = HashMultimap.create();
  }

  static class WalkScore {

    String candidate;
    int hitCount;
    int hopCount;
    float contextualSimilarity;
    float lexicalSimilarity;

    void update(int hopeCount) {
      this.hitCount++;
      this.hopCount += hopeCount;
    }

    float getAverageHittingTime() {
      return hopCount * 1f / hitCount;
    }

    float getScore() {
      return contextualSimilarity + lexicalSimilarity;
    }

    float getScore(float lambda1, float lambda2) {
      return lambda1 * contextualSimilarity + lambda2 * lexicalSimilarity;
    }

    WalkScore(String candidate) {
      this.candidate = candidate;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      WalkScore walkScore = (WalkScore) o;

      return candidate.equals(walkScore.candidate);
    }

    @Override
    public int hashCode() {
      return candidate.hashCode();
    }
  }

  private static class Work {

    int[] wordIndexes;

    Work(int[] wordIndexes) {
      this.wordIndexes = wordIndexes;
    }
  }

  private static Map<String, String> reducedWords = new ConcurrentHashMap<>(100_000);

  private static String reduceWord(String input) {

    String cached = reducedWords.get(input);
    if (cached != null) {
      return cached;
    }
    String s = TurkishAlphabet.INSTANCE.toAscii(input);
    if (input.length() < 3) {
      return s;
    }
    StringBuilder sb = new StringBuilder(input.length() - 2);
    char previous = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (TurkishAlphabet.INSTANCE.isVowel(c) || c == 'ğ') {
        previous = 0;
        continue;
      }
      if (previous == c) {
        continue;
      }
      sb.append(c);
      previous = c;
    }
    String reduced = sb.toString();

    if (reduced.length() == 0) {
      return input;
    }

    reducedWords.put(input, reduced);
    return reduced;
  }

  static class NormalizationVocabulary {

    List<String> words;
    UIntValueMap<String> indexes = new UIntValueMap<>();
    int noisyWordStart;
    int maybeIncorrectWordStart;

    NormalizationVocabulary(
        Path correct,
        Path incorrect,
        Path maybeIncorrect,
        int correctMinCount,
        int incorrectMinCount,
        int maybeIncorrectMinCount) throws IOException {
      Histogram<String> correctWords = Histogram.loadFromUtf8File(correct, ' ');
      Histogram<String> noisyWords = Histogram.loadFromUtf8File(incorrect, ' ');
      Histogram<String> maybeIncorrectWords = new Histogram<>();
      if (maybeIncorrect != null) {
        maybeIncorrectWords = Histogram.loadFromUtf8File(maybeIncorrect, ' ');
      }
      correctWords.removeSmaller(correctMinCount);
      noisyWords.removeSmaller(incorrectMinCount);
      maybeIncorrectWords.removeSmaller(maybeIncorrectMinCount);
      this.noisyWordStart = correctWords.size();

      this.words = new ArrayList<>(correctWords.getSortedList());
      words.addAll(noisyWords.getSortedList());

      this.maybeIncorrectWordStart = words.size();
      words.addAll(maybeIncorrectWords.getSortedList());

      int i = 0;
      for (String word : words) {
        indexes.put(word, i);
        i++;
      }
    }

    int totalSize() {
      return words.size();
    }

    boolean isCorrect(int id) {
      return id >= 0 && id < noisyWordStart;
    }

    boolean isCorrect(String id) {
      return isCorrect(getIndex(id));
    }

    boolean isMaybeIncorrect(String id) {
      return isMaybeIncorrect(getIndex(id));
    }

    boolean isMaybeIncorrect(int id) {
      return id >= maybeIncorrectWordStart;
    }

    boolean isIncorrect(String id) {
      return isIncorrect(getIndex(id));
    }

    boolean isIncorrect(int id) {
      return id >= noisyWordStart && id < maybeIncorrectWordStart;
    }

    int getIndex(String word) {
      return indexes.get(word);
    }

    String getWord(int id) {
      return words.get(id);
    }
  }

  /**
   * Generates and serializes a bipartite graph that represents contextual similarity.
   */
  ContextualSimilarityGraph buildGraph(
      BlockTextLoader corpora,
      int contextSize) throws Exception {
    ContextualSimilarityGraph graph = new ContextualSimilarityGraph(vocabulary, contextSize);
    graph.build(corpora, threadCount);
    Log.info("Context hash count before pruning (no singletons) = " + graph.contextHashCount());
    graph.pruneContextNodes();
    Log.info("Context hash count after pruning  = " + graph.contextHashCount());
    Log.info("Edge count = %d", graph.edgeCount());
    Log.info("Creating Words -> Context counts.");
    return graph;
  }

  private static final String SENTENCE_START = "<s>";
  private static final String SENTENCE_END = "</s>";

  static class ContextualSimilarityGraph {

    // this holds context hashes as keys.
    // values are words and their counts for context hash keys.
    UIntMap<IntIntMap> contextHashToWordCounts = new UIntMap<>(5_000_000);

    // This is for memory optimization. It holds <hash, wordIndex> values.
    // Context occurs only once and with count 1 stays in this.
    // This may be discarded during pruning.
    IntIntMap singletons = new IntIntMap(5_000_000);

    NormalizationVocabulary vocabulary;
    ReentrantLock lock = new ReentrantLock();

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

    void build(
        BlockTextLoader corpora,
        int threadCount) throws Exception {

      ExecutorService executorService = new BlockingExecutor(threadCount);

      for (TextChunk chunk : corpora) {

        executorService.submit(() -> {
          Log.info("Processing %s", chunk);
          UIntMap<IntIntMap> localContextCounts = new UIntMap<>(100_000);
          IntIntMap localSingletons = new IntIntMap(100_000);

          List<String> sentences = TextCleaner.cleanAndExtractSentences(chunk.getData());
          for (String sentence : sentences) {
            List<String> tokens = getTokens(sentence);

            // context array will be reused.
            String[] context = new String[contextSize * 2];

            for (int i = contextSize; i < tokens.size() - contextSize; i++) {

              int wordIndex = vocabulary.getIndex(tokens.get(i));

              // if current word is out of vocabulary (neither valid nor noisy) , continue.
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

              //first check singletons.
              if (localSingletons.containsKey(hash)) {
                int val = localSingletons.get(hash);
                localSingletons.remove(hash);
                IntIntMap m = new IntIntMap(2);
                m.increment(val, 1);
                m.increment(wordIndex, 1);
                localContextCounts.put(hash, m);
              } else {
                // update context -> word counts
                IntIntMap wordCounts = localContextCounts.get(hash);
                if (wordCounts != null) {
                  wordCounts = new IntIntMap(1);
                  localContextCounts.put(hash, wordCounts);
                  wordCounts.increment(wordIndex, 1);
                } else {
                  localSingletons.put(hash, wordIndex);
                }
              }
            }
          }

          try {
            lock.lock();
            for (int key : localContextCounts.getKeys()) {
              IntIntMap localMap = localContextCounts.get(key);
              IntIntMap globalMap = contextHashToWordCounts.get(key);
              if (globalMap == null) {
                // remove it from global singletons if exist.
                if (singletons.containsKey(key)) {
                  int wordIndex = singletons.get(key);
                  singletons.remove(key);
                  localMap.increment(wordIndex, 1);
                }
                contextHashToWordCounts.put(key, localMap);
              } else {
                for (int word : localMap.getKeys()) {
                  int localCount = localMap.get(word);
                  globalMap.increment(word, localCount);
                }
              }
            }
            // now put singletons.
            for (int key : localSingletons.getKeys()) {
              int wordIndex = localSingletons.get(key);
              IntIntMap mm = contextHashToWordCounts.get(key);
              if (mm == null) {
                if (singletons.containsKey(key)) {
                  int w = singletons.get(key);
                  singletons.remove(key);
                  mm = new IntIntMap(1);
                  mm.increment(w, 1);
                  mm.increment(wordIndex, 1);
                  contextHashToWordCounts.put(key, mm);
                } else {
                  singletons.put(key, wordIndex);
                }
              } else {
                mm.increment(wordIndex, 1);
              }
            }
            long contextCount = contextHashToWordCounts.size() + singletons.size();
            Log.info("Context count = %d, Singleton context count = ",
                contextCount, singletons.size());
          } finally {
            lock.unlock();
          }
        });
      }
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.DAYS);
    }

    private List<String> getTokens(String sentence) {
      List<String> tokens = new ArrayList<>();

      for (int i = 0; i < contextSize; i++) {
        tokens.add(SENTENCE_START);
      }

      sentence = sentence.toLowerCase(Turkish.LOCALE);
      List<Token> raw = TurkishTokenizer.DEFAULT.tokenize(sentence);

      // use substitute values for numbers, urls etc.
      for (Token token : raw) {
        if (token.getType() == Type.Punctuation) {
          continue;
        }
        String text = token.getText();
        switch (token.getType()) {
          case Time:
          case PercentNumeral:
          case Number:
          case Date:
            text = text.replaceAll("\\d+", "_d");
            break;
          case URL:
            text = "<url>";
            break;
          case HashTag:
            text = "<hashtag>";
            break;
          case Email:
            text = "<email>";
            break;
        }
        tokens.add(text);
      }

      for (int i = 0; i < contextSize; i++) {
        tokens.add(SENTENCE_END);
      }
      return tokens;
    }

    void pruneContextNodes() {
      // remove all singletons.
      singletons = new IntIntMap();

      UIntSet keysToPrune = new UIntSet();
      for (int contextHash : contextHashToWordCounts.getKeys()) {
        IntIntMap m = contextHashToWordCounts.get(contextHash);

        // prune if a context only points to a single word.
        if (m.size() <= 1) {
          keysToPrune.add(contextHash);
          continue;
        }

        // prune if a context is only connected to noisy words. For speed we only check nodes with
        // at most five connections.
        if (m.size() < 5) {
          int noisyCount = 0;
          for (int wordIndex : m.getKeys()) {
            if (vocabulary.isIncorrect(wordIndex)) {
              noisyCount++;
            } else {
              break;
            }
          }
          if (noisyCount == m.size()) {
            keysToPrune.add(contextHash);
          }
        }
      }
      for (int keyToRemove : keysToPrune.getKeys()) {
        contextHashToWordCounts.remove(keyToRemove);
      }
    }

    void serializeForRandomWalk(Path p) throws IOException {

      UIntMap<IntIntMap> wordToContexts = new UIntMap<>();
      for (int contextHash : contextHashToWordCounts.getKeys()) {
        IntIntMap m = contextHashToWordCounts.get(contextHash);
        for (int worIndex : m.getKeys()) {
          int count = m.get(worIndex);
          IntIntMap contextCounts = wordToContexts.get(worIndex);
          if (contextCounts == null) {
            contextCounts = new IntIntMap(1);
            wordToContexts.put(worIndex, contextCounts);
          }
          contextCounts.put(contextHash, count);
        }
      }

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

  /**
   * calculates a non negative 31 bit hash value of a String array.
   */
  static int hash(String... context) {
    int d = 0x811C9DC5;
    for (String s : context) {
      for (int i = 0; i < s.length(); i++) {
        d = (d ^ s.charAt(i)) * 16777619;
      }
    }
    return d & 0x7fffffff;
  }

  /**
   * Finds the longest common substring of two strings.
   */
  private static String longestCommonSubstring(String a, String b, boolean asciiTolerant) {
    int[][] lengths = new int[a.length() + 1][b.length() + 1];

    // row 0 and column 0 are initialized to 0 already

    for (int i = 0; i < a.length(); i++) {
      for (int j = 0; j < b.length(); j++) {
        boolean b1 = asciiTolerant ?
            TurkishAlphabet.INSTANCE.isAsciiEqual(a.charAt(i), b.charAt(j)) :
            a.charAt(i) == b.charAt(j);
        if (b1) {
          lengths[i + 1][j + 1] = lengths[i][j] + 1;
        } else {
          lengths[i + 1][j + 1] =
              Math.max(lengths[i + 1][j], lengths[i][j + 1]);
        }
      }
    }

    // read the substring out from the matrix
    StringBuilder sb = new StringBuilder();
    for (int x = a.length(), y = b.length();
        x != 0 && y != 0; ) {
      if (lengths[x][y] == lengths[x - 1][y]) {
        x--;
      } else if (lengths[x][y] == lengths[x][y - 1]) {
        y--;
      } else {
        assert a.charAt(x - 1) == b.charAt(y - 1);
        sb.append(a.charAt(x - 1));
        x--;
        y--;
      }
    }

    return sb.reverse().toString();
  }

}
