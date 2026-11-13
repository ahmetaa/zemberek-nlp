package zemberek.normalization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisSurfaceFormatter;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.StemTransition;

/**
 * Memory-compact, high-throughput Turkish spell-check candidate generator backed by Compressed
 * Sparse Row (CSR) {@link FlatTrie} arrays for stems and endings.
 *
 * <p>Architectural features:
 * <ul>
 *   <li><b>CSR Primitive Tries</b>: Stores stem and ending tries in flat primitive arrays (~8.4 MB
 *       total vs. ~103 MB for object-based {@link StemEndingGraph}).
 *   <li><b>Oflazer Length Bounds</b>: Precomputes exact {@code minRem}/{@code maxRem} bounds per
 *       node to prune impossible branches in O(1).
 *   <li><b>Zero-Allocation Tail Matching</b>: Once the 1-edit budget is exhausted, remaining
 *       characters match via direct CSR edge lookup without hypothesis object allocation.
 *   <li><b>Morphotactic Bridge Pruning</b>: Encodes 7-bit {@link PhoneticAttribute} compatibility
 *       flags on stem nodes to filter invalid stem+ending joins prior to morphological analysis.
 *   <li><b>Symmetric Diacritic &amp; Keyboard Channel Model</b>: Tracks fine-grained edit and
 *       diacritic distances on each {@link ScoredCandidate}.
 * </ul>
 */
public final class CompactStemEndingSpellChecker {

  public static final int DEFAULT_MAX_CACHE_SIZE = 16_384;

  private static final byte FLAG_CAN_TERMINATE = 1;
  private static final byte FLAG_ALLOWS_VOWEL_ENDING = 1 << 1;
  private static final byte FLAG_ALLOWS_CONSONANT_ENDING = 1 << 2;
  private static final byte FLAG_HAS_STEM = 1 << 3;
  private static final byte FLAG_FRONT_HARMONY = 1 << 4;
  private static final byte FLAG_BACK_HARMONY = 1 << 5;
  private static final byte FLAG_ALLOWS_VOICED_D = 1 << 6;

  private static final int MAX_ASCII_TABLE = 512;
  private static final char[][] DIACRITIC_MATCHES = new char[MAX_ASCII_TABLE][];
  private static final boolean[][] NEAR_OR_PHONETIC = new boolean[MAX_ASCII_TABLE][MAX_ASCII_TABLE];

  static {
    String letters = TurkishAlphabet.INSTANCE.getAllLetters() + "+.,'-";
    for (int i = 0; i < letters.length(); i++) {
      char c = letters.charAt(i);
      if (c < MAX_ASCII_TABLE) {
        DIACRITIC_MATCHES[c] = new char[] {c};
      }
    }
    // Symmetric diacritic equivalence classes covering Turkish letters and circumflex vowels.
    registerDiacriticGroup('a', 'â');
    registerDiacriticGroup('c', 'ç');
    registerDiacriticGroup('g', 'ğ');
    registerDiacriticGroup('ı', 'i', 'î');
    registerDiacriticGroup('o', 'ö');
    registerDiacriticGroup('s', 'ş');
    registerDiacriticGroup('u', 'ü', 'û');

    registerDiacriticGroup('A', 'Â');
    registerDiacriticGroup('C', 'Ç');
    registerDiacriticGroup('G', 'Ğ');
    registerDiacriticGroup('I', 'İ', 'Î');
    registerDiacriticGroup('O', 'Ö');
    registerDiacriticGroup('S', 'Ş');
    registerDiacriticGroup('U', 'Ü', 'Û');

    // Turkish Q-keyboard adjacency and common phonetic substitutions.
    registerPairs('a', "s");
    registerPairs('b', "vnp");
    registerPairs('c', "vxçj");
    registerPairs('ç', "öcş");
    registerPairs('d', "sft");
    registerPairs('e', "wri");
    registerPairs('f', "gdv");
    registerPairs('g', "fhğk");
    registerPairs('ğ', "pügy");
    registerPairs('h', "gj");
    registerPairs('ı', "oui");
    registerPairs('i', "şıe");
    registerPairs('j', "hkşc");
    registerPairs('k', "jlg");
    registerPairs('l', "kşr");
    registerPairs('m', "nö");
    registerPairs('n', "bm");
    registerPairs('o', "ıpöu");
    registerPairs('ö', "mçoü");
    registerPairs('p', "oğb");
    registerPairs('r', "etl");
    registerPairs('s', "adşz");
    registerPairs('ş', "lisçj");
    registerPairs('t', "ryd");
    registerPairs('u', "yıüo");
    registerPairs('ü', "ğuoö");
    registerPairs('v', "cbfğ");
    registerPairs('y', "tuğ");
    registerPairs('z', "xs");
  }

  private static void registerDiacriticGroup(char... chars) {
    for (char c : chars) {
      if (c < MAX_ASCII_TABLE) {
        DIACRITIC_MATCHES[c] = chars;
      }
    }
  }

  private static void registerPairs(char c, String neighbors) {
    for (int i = 0; i < neighbors.length(); i++) {
      char n = neighbors.charAt(i);
      if (c < MAX_ASCII_TABLE && n < MAX_ASCII_TABLE) {
        NEAR_OR_PHONETIC[c][n] = true;
        NEAR_OR_PHONETIC[n][c] = true;
      }
    }
  }

  /**
   * Spell-check candidate together with its weighted edit penalty and diacritic difference count.
   */
  public static final class ScoredCandidate {

    public final String surface;
    public final float editCost;
    public final int diacriticDiffs;

    public ScoredCandidate(String surface, float editCost, int diacriticDiffs) {
      this.surface = surface;
      this.editCost = editCost;
      this.diacriticDiffs = diacriticDiffs;
    }

    public float totalDistanceCost(boolean asciiContext) {
      return editCost + (asciiContext ? 0.08f : 0.28f) * diacriticDiffs;
    }
  }

  private static final class TempNode {

    char[] childChars = new char[0];
    TempNode[] childNodes = new TempNode[0];
    int terminalId = -1;
    byte flags = 0;

    TempNode getOrAdd(char c) {
      int idx = Arrays.binarySearch(childChars, c);
      if (idx >= 0) {
        return childNodes[idx];
      }
      int ins = -idx - 1;
      int len = childChars.length;
      char[] nextChars = new char[len + 1];
      TempNode[] nextNodes = new TempNode[len + 1];
      System.arraycopy(childChars, 0, nextChars, 0, ins);
      System.arraycopy(childNodes, 0, nextNodes, 0, ins);
      TempNode created = new TempNode();
      nextChars[ins] = c;
      nextNodes[ins] = created;
      System.arraycopy(childChars, ins, nextChars, ins + 1, len - ins);
      System.arraycopy(childNodes, ins, nextNodes, ins + 1, len - ins);
      childChars = nextChars;
      childNodes = nextNodes;
      return created;
    }
  }

  static final class FlatTrie {

    final int[] edgeStart;
    final short[] edgeCount;
    final char[] edgeChars;
    final int[] edgeTargets;
    final int[] terminalId;
    final byte[] flags;
    final byte[] minRem;
    final byte[] maxRem;
    final String[] terminals;

    FlatTrie(
        int[] edgeStart,
        short[] edgeCount,
        char[] edgeChars,
        int[] edgeTargets,
        int[] terminalId,
        byte[] flags,
        byte[] minRem,
        byte[] maxRem,
        String[] terminals) {
      this.edgeStart = edgeStart;
      this.edgeCount = edgeCount;
      this.edgeChars = edgeChars;
      this.edgeTargets = edgeTargets;
      this.terminalId = terminalId;
      this.flags = flags;
      this.minRem = minRem;
      this.maxRem = maxRem;
      this.terminals = terminals;
    }

    int nodeCount() {
      return edgeStart.length;
    }

    int edgeTotal() {
      return edgeChars.length;
    }

    long estimatedBytes() {
      return (long) edgeStart.length * (4 + 2 + 4 + 1 + 1 + 1)
          + (long) edgeChars.length * (2 + 4)
          + (long) terminals.length * 40L;
    }

    int findChild(int node, char c) {
      int start = edgeStart[node];
      int count = edgeCount[node];
      if (count <= 6) {
        int end = start + count;
        for (int i = start; i < end; i++) {
          char ec = edgeChars[i];
          if (ec == c) {
            return edgeTargets[i];
          }
          if (ec > c) {
            return -1;
          }
        }
        return -1;
      }
      int lo = start;
      int hi = start + count - 1;
      while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        char ec = edgeChars[mid];
        if (ec < c) {
          lo = mid + 1;
        } else if (ec > c) {
          hi = mid - 1;
        } else {
          return edgeTargets[mid];
        }
      }
      return -1;
    }
  }

  private final TurkishMorphology morphology;
  private final WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();
  private final FlatTrie stemTrie;
  private final FlatTrie endingTrie;
  private final boolean matchDiacritics;
  private final Map<String, List<ScoredCandidate>> suggestionCache;
  private final AtomicLong rawGraphAnalysesCount = new AtomicLong();

  public CompactStemEndingSpellChecker(TurkishMorphology morphology, boolean matchDiacritics)
      throws IOException {
    this(
        morphology,
        TextIO.loadLinesFromResource("endings"),
        matchDiacritics,
        DEFAULT_MAX_CACHE_SIZE);
  }

  public CompactStemEndingSpellChecker(
      TurkishMorphology morphology,
      List<String> endings,
      boolean matchDiacritics,
      int maxCacheSize) {
    this.morphology = morphology;
    this.matchDiacritics = matchDiacritics;
    this.endingTrie = buildEndingTrie(endings);
    this.stemTrie = buildStemTrie(morphology, this.endingTrie.maxRem[0]);
    this.suggestionCache = createCache(maxCacheSize);
  }

  private CompactStemEndingSpellChecker(
      TurkishMorphology morphology,
      FlatTrie stemTrie,
      FlatTrie endingTrie,
      boolean matchDiacritics,
      int maxCacheSize) {
    this.morphology = morphology;
    this.stemTrie = stemTrie;
    this.endingTrie = endingTrie;
    this.matchDiacritics = matchDiacritics;
    this.suggestionCache = createCache(maxCacheSize);
  }

  private static Map<String, List<ScoredCandidate>> createCache(int maxCacheSize) {
    if (maxCacheSize <= 0) {
      return null;
    }
    return Collections.synchronizedMap(
        new LinkedHashMap<String, List<ScoredCandidate>>(1024, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, List<ScoredCandidate>> eldest) {
            return size() > maxCacheSize;
          }
        });
  }

  /**
   * Returns a view sharing the compiled stem and ending CSR tries with the requested diacritic
   * matching behavior and default cache size.
   */
  public CompactStemEndingSpellChecker withDiacriticsIgnoring(boolean matchDiacritics) {
    return withDiacriticsIgnoring(matchDiacritics, DEFAULT_MAX_CACHE_SIZE);
  }

  /**
   * Returns a view sharing the compiled stem and ending CSR tries with the requested diacritic
   * matching behavior and suggestion cache capacity.
   */
  public CompactStemEndingSpellChecker withDiacriticsIgnoring(
      boolean matchDiacritics, int maxCacheSize) {
    return new CompactStemEndingSpellChecker(
        morphology, stemTrie, endingTrie, matchDiacritics, maxCacheSize);
  }

  public boolean isMatchDiacritics() {
    return matchDiacritics;
  }

  public long estimatedGraphBytes() {
    return stemTrie.estimatedBytes() + endingTrie.estimatedBytes();
  }

  public int totalNodeCount() {
    return stemTrie.nodeCount() + endingTrie.nodeCount();
  }

  public int totalEdgeCount() {
    return stemTrie.edgeTotal() + endingTrie.edgeTotal();
  }

  public long getRawGraphAnalysesCount() {
    return rawGraphAnalysesCount.get();
  }

  public void clearCache() {
    if (suggestionCache != null) {
      suggestionCache.clear();
    }
  }

  private static FlatTrie buildEndingTrie(List<String> endings) {
    TempNode root = new TempNode();
    List<String> termList = new ArrayList<>(endings.size());
    for (String ending : endings) {
      if (ending.isEmpty()) {
        continue;
      }
      TempNode cur = root;
      for (int i = 0; i < ending.length(); i++) {
        cur = cur.getOrAdd(ending.charAt(i));
      }
      if (cur.terminalId < 0) {
        cur.terminalId = termList.size();
        termList.add(ending);
      }
      cur.flags |= FLAG_CAN_TERMINATE;
    }
    return freeze(root, termList, 0);
  }

  private static FlatTrie buildStemTrie(TurkishMorphology morphology, int maxEndingLength) {
    TempNode root = new TempNode();
    List<String> termList = new ArrayList<>(90_000);
    for (StemTransition tr : morphology.getMorphotactics().getStemTransitions().getTransitions()) {
      if (tr.surface.isEmpty() || tr.item.primaryPos == PrimaryPos.Punctuation) {
        continue;
      }
      TempNode cur = root;
      for (int i = 0; i < tr.surface.length(); i++) {
        cur = cur.getOrAdd(tr.surface.charAt(i));
      }
      if (cur.terminalId < 0) {
        cur.terminalId = termList.size();
        termList.add(tr.surface);
      }
      cur.flags |= FLAG_HAS_STEM;
      AttributeSet<PhoneticAttribute> attrs = tr.getPhoneticAttributes();
      if (!attrs.contains(PhoneticAttribute.CannotTerminate)) {
        cur.flags |= FLAG_CAN_TERMINATE;
      }
      if (!attrs.contains(PhoneticAttribute.ExpectsConsonant)
          && !attrs.contains(PhoneticAttribute.LastLetterVowel)) {
        cur.flags |= FLAG_ALLOWS_VOWEL_ENDING;
      }
      if (!attrs.contains(PhoneticAttribute.ExpectsVowel)) {
        cur.flags |= FLAG_ALLOWS_CONSONANT_ENDING;
      }
      if (attrs.contains(PhoneticAttribute.LastVowelFrontal)
          || attrs.contains(PhoneticAttribute.HasNoVowel)) {
        cur.flags |= FLAG_FRONT_HARMONY;
      }
      if (attrs.contains(PhoneticAttribute.LastVowelBack)
          || attrs.contains(PhoneticAttribute.HasNoVowel)) {
        cur.flags |= FLAG_BACK_HARMONY;
      }
      if (!attrs.contains(PhoneticAttribute.LastLetterVoiceless)) {
        cur.flags |= FLAG_ALLOWS_VOICED_D;
      }
    }
    return freeze(root, termList, maxEndingLength);
  }

  private static FlatTrie freeze(TempNode root, List<String> termList, int maxEndingLength) {
    List<TempNode> order = new ArrayList<>(64_000);
    collectPreOrder(root, order);
    int n = order.size();
    int totalEdges = 0;
    for (TempNode node : order) {
      totalEdges += node.childChars.length;
    }

    Map<TempNode, Integer> nodeToIdx = new IdentityHashMap<>(n);
    for (int i = 0; i < n; i++) {
      nodeToIdx.put(order.get(i), i);
    }

    int[] edgeStart = new int[n];
    short[] edgeCount = new short[n];
    char[] edgeChars = new char[totalEdges];
    int[] edgeTargets = new int[totalEdges];
    int[] terminalId = new int[n];
    byte[] flags = new byte[n];
    int[] minRemInt = new int[n];
    int[] maxRemInt = new int[n];

    int cursor = 0;
    for (int i = 0; i < n; i++) {
      TempNode tn = order.get(i);
      edgeStart[i] = cursor;
      int cnt = tn.childChars.length;
      edgeCount[i] = (short) cnt;
      terminalId[i] = tn.terminalId;
      flags[i] = tn.flags;
      for (int j = 0; j < cnt; j++) {
        edgeChars[cursor] = tn.childChars[j];
        edgeTargets[cursor] = nodeToIdx.get(tn.childNodes[j]);
        cursor++;
      }
    }

    // Bottom-up remaining length computation in reverse pre-order.
    for (int i = n - 1; i >= 0; i--) {
      int minR = 127;
      int maxR = 0;
      byte f = flags[i];
      if ((f & FLAG_CAN_TERMINATE) != 0) {
        minR = 0;
      }
      if ((f & FLAG_HAS_STEM) != 0 && maxEndingLength > 0) {
        minR = Math.min(minR, 1);
        maxR = Math.max(maxR, maxEndingLength);
      }
      int start = edgeStart[i];
      int end = start + edgeCount[i];
      for (int e = start; e < end; e++) {
        int child = edgeTargets[e];
        minR = Math.min(minR, 1 + minRemInt[child]);
        maxR = Math.max(maxR, 1 + maxRemInt[child]);
      }
      minRemInt[i] = Math.min(126, minR);
      maxRemInt[i] = Math.min(126, maxR);
    }

    byte[] minRem = new byte[n];
    byte[] maxRem = new byte[n];
    for (int i = 0; i < n; i++) {
      minRem[i] = (byte) minRemInt[i];
      maxRem[i] = (byte) maxRemInt[i];
    }

    String[] terminals = termList.toArray(new String[0]);
    return new FlatTrie(
        edgeStart,
        edgeCount,
        edgeChars,
        edgeTargets,
        terminalId,
        flags,
        minRem,
        maxRem,
        terminals);
  }

  private static void collectPreOrder(TempNode node, List<TempNode> out) {
    out.add(node);
    for (TempNode child : node.childNodes) {
      collectPreOrder(child, out);
    }
  }

  /**
   * Returns morphologically valid, case-formatted candidate words together with their edit and
   * diacritic distances.
   */
  public List<ScoredCandidate> suggestScored(String word) {
    return suggestScored(word, null);
  }

  /**
   * Returns morphologically valid, case-formatted candidate words filtered by {@code
   * analysisPredicate} (if non-null) together with their edit and diacritic distances.
   */
  public List<ScoredCandidate> suggestScored(
      String word, Predicate<SingleAnalysis> analysisPredicate) {
    if (analysisPredicate == null && suggestionCache != null) {
      List<ScoredCandidate> cached = suggestionCache.get(word);
      if (cached != null) {
        return cached;
      }
    }

    String stripped = stripApostrophes(word);
    String normalized = TurkishAlphabet.INSTANCE.normalize(stripped);
    if (normalized.isEmpty()) {
      return Collections.emptyList();
    }

    RawCandidateCollector rawCollector = new RawCandidateCollector();
    searchStem(0, normalized, 0, 0, 0f, 0, rawCollector);

    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(word);
    if (caseType == WordAnalysisSurfaceFormatter.CaseType.MIXED_CASE
        || caseType == WordAnalysisSurfaceFormatter.CaseType.LOWER_CASE) {
      caseType = WordAnalysisSurfaceFormatter.CaseType.DEFAULT_CASE;
    }
    String apostrophe = getApostrophe(word);

    List<ScoredCandidate> rawList = rawCollector.toSortedList();
    int limit = Math.min(rawList.size(), 160);

    Map<String, ScoredCandidate> validMap = new LinkedHashMap<>(16);
    float bestValidScore = Float.POSITIVE_INFINITY;
    for (int i = 0; i < limit; i++) {
      ScoredCandidate rc = rawList.get(i);
      float rcScore = rc.editCost + 0.20f * rc.diacriticDiffs;
      if (validMap.size() >= 32 && rcScore > 1.05f && rcScore > bestValidScore + 0.65f) {
        break;
      }
      rawGraphAnalysesCount.incrementAndGet();
      WordAnalysis analyses = morphology.analyze(rc.surface);
      for (SingleAnalysis analysis : analyses) {
        if (analysis.isUnknown()) {
          continue;
        }
        if (analysisPredicate != null && !analysisPredicate.test(analysis)) {
          continue;
        }
        String formatted = formatter.formatToCase(analysis, caseType, apostrophe);
        if (rcScore < bestValidScore) {
          bestValidScore = rcScore;
        }
        ScoredCandidate existing = validMap.get(formatted);
        if (existing == null
            || rc.totalDistanceCost(false) < existing.totalDistanceCost(false)) {
          validMap.put(formatted, new ScoredCandidate(formatted, rc.editCost, rc.diacriticDiffs));
        }
      }
    }

    List<ScoredCandidate> result = Collections.unmodifiableList(new ArrayList<>(validMap.values()));
    if (analysisPredicate == null && suggestionCache != null) {
      suggestionCache.put(word, result);
    }
    return result;
  }

  private void searchStem(
      int node,
      String input,
      int pos,
      int edits,
      float cost,
      int diacriticDiffs,
      RawCandidateCollector out) {
    int len = input.length();
    int rem = len - pos;
    int budget = 1 - edits;
    if (rem < stemTrie.minRem[node] - budget || rem > stemTrie.maxRem[node] + budget) {
      return;
    }

    byte f = stemTrie.flags[node];
    int stemTermId = stemTrie.terminalId[node];

    if (pos == len && (f & FLAG_CAN_TERMINATE) != 0 && stemTermId >= 0) {
      out.record(stemTrie.terminals[stemTermId], cost, diacriticDiffs);
    }

    if ((f & FLAG_HAS_STEM) != 0 && stemTermId >= 0) {
      searchEndingFromRoot(f, stemTermId, input, pos, edits, cost, diacriticDiffs, out);
    }

    if (edits >= 1) {
      if (pos < len) {
        char ch = input.charAt(pos);
        char[] matches = getMatches(ch);
        if (matches == null) {
          int child = stemTrie.findChild(node, ch);
          if (child >= 0) {
            searchStem(child, input, pos + 1, 1, cost, diacriticDiffs, out);
          }
        } else {
          for (char mc : matches) {
            int child = stemTrie.findChild(node, mc);
            if (child >= 0) {
              searchStem(
                  child,
                  input,
                  pos + 1,
                  1,
                  cost,
                  diacriticDiffs + (mc == ch ? 0 : 1),
                  out);
            }
          }
        }
      }
      return;
    }

    int start = stemTrie.edgeStart[node];
    int end = start + stemTrie.edgeCount[node];

    if (pos < len) {
      char ch = input.charAt(pos);
      for (int e = start; e < end; e++) {
        char ec = stemTrie.edgeChars[e];
        int child = stemTrie.edgeTargets[e];
        if (ec == ch) {
          searchStem(child, input, pos + 1, 0, cost, diacriticDiffs, out);
        } else if (isDiacriticMatch(ch, ec)) {
          searchStem(child, input, pos + 1, 0, cost, diacriticDiffs + 1, out);
        } else {
          float subCost = substitutionCost(ch, ec);
          searchStem(child, input, pos + 1, 1, cost + subCost, diacriticDiffs, out);
        }
      }

      float delCost = deletionFromInputCost(input, pos);
      searchStem(node, input, pos + 1, 1, cost + delCost, diacriticDiffs, out);

      if (len > 2 && pos + 1 < len) {
        char nextCh = input.charAt(pos + 1);
        if (ch != nextCh) {
          exploreTranspositionInStem(node, ch, nextCh, input, pos, cost, diacriticDiffs, out);
        }
      }
    }

    for (int e = start; e < end; e++) {
      char ec = stemTrie.edgeChars[e];
      int child = stemTrie.edgeTargets[e];
      searchStem(child, input, pos, 1, cost + omittedCharCost(ec), diacriticDiffs, out);
    }
  }

  private void exploreTranspositionInStem(
      int node,
      char c0,
      char c1,
      String input,
      int pos,
      float cost,
      int diacriticDiffs,
      RawCandidateCollector out) {
    char[] m1 = getMatches(c1);
    char[] m0 = getMatches(c0);
    if (m1 == null && m0 == null) {
      int mid = stemTrie.findChild(node, c1);
      if (mid >= 0) {
        int dst = stemTrie.findChild(mid, c0);
        if (dst >= 0) {
          searchStem(dst, input, pos + 2, 1, cost + 0.70f, diacriticDiffs, out);
        }
        byte mf = stemTrie.flags[mid];
        int midTerm = stemTrie.terminalId[mid];
        if ((mf & FLAG_HAS_STEM) != 0 && midTerm >= 0) {
          stepEndingExact(mf, midTerm, 0, c0, 0, input, pos + 2, 1, cost + 0.70f, diacriticDiffs, out);
        }
      }
    } else {
      char[] a1 = m1 != null ? m1 : new char[] {c1};
      char[] a0 = m0 != null ? m0 : new char[] {c0};
      for (char x1 : a1) {
        int d1 = (x1 == c1) ? 0 : 1;
        int mid = stemTrie.findChild(node, x1);
        if (mid >= 0) {
          for (char x0 : a0) {
            int d0 = (x0 == c0) ? 0 : 1;
            int dst = stemTrie.findChild(mid, x0);
            if (dst >= 0) {
              searchStem(dst, input, pos + 2, 1, cost + 0.70f, diacriticDiffs + d1 + d0, out);
            }
            byte mf = stemTrie.flags[mid];
            int midTerm = stemTrie.terminalId[mid];
            if ((mf & FLAG_HAS_STEM) != 0 && midTerm >= 0) {
              stepEndingExact(
                  mf, midTerm, 0, x0, d1 + d0, input, pos + 2, 1, cost + 0.70f, diacriticDiffs, out);
            }
          }
        }
      }
    }
  }

  private void searchEndingFromRoot(
      byte stemFlags,
      int stemTermId,
      String input,
      int pos,
      int edits,
      float cost,
      int diacriticDiffs,
      RawCandidateCollector out) {
    int len = input.length();
    int rem = len - pos;
    int budget = 1 - edits;
    if (rem < endingTrie.minRem[0] - budget || rem > endingTrie.maxRem[0] + budget) {
      return;
    }

    boolean allowVowel = (stemFlags & FLAG_ALLOWS_VOWEL_ENDING) != 0;
    boolean allowConsonant = (stemFlags & FLAG_ALLOWS_CONSONANT_ENDING) != 0;
    if (!allowVowel && !allowConsonant) {
      return;
    }

    int start = endingTrie.edgeStart[0];
    int end = start + endingTrie.edgeCount[0];

    if (edits >= 1) {
      if (pos < len) {
        char ch = input.charAt(pos);
        char[] matches = getMatches(ch);
        if (matches == null) {
          stepEndingExact(
              stemFlags, stemTermId, 0, ch, 0, input, pos + 1, 1, cost, diacriticDiffs, out);
        } else {
          for (char mc : matches) {
            stepEndingExact(
                stemFlags,
                stemTermId,
                0,
                mc,
                mc == ch ? 0 : 1,
                input,
                pos + 1,
                1,
                cost,
                diacriticDiffs,
                out);
          }
        }
      }
      return;
    }

    if (pos < len) {
      char ch = input.charAt(pos);
      for (int e = start; e < end; e++) {
        char ec = endingTrie.edgeChars[e];
        if (!isFirstEndingCharCompatible(ec, stemFlags)) {
          continue;
        }
        int child = endingTrie.edgeTargets[e];
        if (ec == ch) {
          searchEnding(stemFlags, stemTermId, child, input, pos + 1, 0, cost, diacriticDiffs, out);
        } else if (isDiacriticMatch(ch, ec)) {
          searchEnding(
              stemFlags, stemTermId, child, input, pos + 1, 0, cost, diacriticDiffs + 1, out);
        } else {
          float subCost = substitutionCost(ch, ec);
          searchEnding(
              stemFlags, stemTermId, child, input, pos + 1, 1, cost + subCost, diacriticDiffs, out);
        }
      }

      if (len > 2 && pos + 1 < len) {
        char nextCh = input.charAt(pos + 1);
        if (ch != nextCh) {
          exploreTranspositionInEnding(
              0, true, stemFlags, stemTermId, ch, nextCh, input, pos, cost, diacriticDiffs, out);
        }
      }
    }

    for (int e = start; e < end; e++) {
      char ec = endingTrie.edgeChars[e];
      if (!isFirstEndingCharCompatible(ec, stemFlags)) {
        continue;
      }
      int child = endingTrie.edgeTargets[e];
      searchEnding(
          stemFlags,
          stemTermId,
          child,
          input,
          pos,
          1,
          cost + omittedCharCost(ec),
          diacriticDiffs,
          out);
    }
  }

  private void stepEndingExact(
      byte stemFlags,
      int stemTermId,
      int endingNode,
      char ec,
      int extraDiacritic,
      String input,
      int nextPos,
      int edits,
      float cost,
      int diacriticDiffs,
      RawCandidateCollector out) {
    if (endingNode == 0 && !isFirstEndingCharCompatible(ec, stemFlags)) {
      return;
    }
    int child = endingTrie.findChild(endingNode, ec);
    if (child >= 0) {
      searchEnding(
          stemFlags,
          stemTermId,
          child,
          input,
          nextPos,
          edits,
          cost,
          diacriticDiffs + extraDiacritic,
          out);
    }
  }

  private void searchEnding(
      byte stemFlags,
      int stemTermId,
      int node,
      String input,
      int pos,
      int edits,
      float cost,
      int diacriticDiffs,
      RawCandidateCollector out) {
    int len = input.length();
    int rem = len - pos;
    int budget = 1 - edits;
    if (rem < endingTrie.minRem[node] - budget || rem > endingTrie.maxRem[node] + budget) {
      return;
    }

    int endTermId = endingTrie.terminalId[node];
    if (pos == len && endTermId >= 0) {
      String endingStr = endingTrie.terminals[endTermId];
      if (isEndingHarmoniousWithStem(stemFlags, endingStr)) {
        out.record(stemTrie.terminals[stemTermId] + endingStr, cost, diacriticDiffs);
      }
    }

    if (edits >= 1) {
      if (pos < len) {
        char ch = input.charAt(pos);
        char[] matches = getMatches(ch);
        if (matches == null) {
          int child = endingTrie.findChild(node, ch);
          if (child >= 0) {
            searchEnding(
                stemFlags, stemTermId, child, input, pos + 1, 1, cost, diacriticDiffs, out);
          }
        } else {
          for (char mc : matches) {
            int child = endingTrie.findChild(node, mc);
            if (child >= 0) {
              searchEnding(
                  stemFlags,
                  stemTermId,
                  child,
                  input,
                  pos + 1,
                  1,
                  cost,
                  diacriticDiffs + (mc == ch ? 0 : 1),
                  out);
            }
          }
        }
      }
      return;
    }

    int start = endingTrie.edgeStart[node];
    int end = start + endingTrie.edgeCount[node];

    if (pos < len) {
      char ch = input.charAt(pos);
      for (int e = start; e < end; e++) {
        char ec = endingTrie.edgeChars[e];
        int child = endingTrie.edgeTargets[e];
        if (ec == ch) {
          searchEnding(stemFlags, stemTermId, child, input, pos + 1, 0, cost, diacriticDiffs, out);
        } else if (isDiacriticMatch(ch, ec)) {
          searchEnding(
              stemFlags, stemTermId, child, input, pos + 1, 0, cost, diacriticDiffs + 1, out);
        } else {
          float subCost = substitutionCost(ch, ec);
          searchEnding(
              stemFlags, stemTermId, child, input, pos + 1, 1, cost + subCost, diacriticDiffs, out);
        }
      }

      float delCost = deletionFromInputCost(input, pos);
      searchEnding(
          stemFlags, stemTermId, node, input, pos + 1, 1, cost + delCost, diacriticDiffs, out);

      if (len > 2 && pos + 1 < len) {
        char nextCh = input.charAt(pos + 1);
        if (ch != nextCh) {
          exploreTranspositionInEnding(
              node, false, stemFlags, stemTermId, ch, nextCh, input, pos, cost, diacriticDiffs, out);
        }
      }
    }

    for (int e = start; e < end; e++) {
      char ec = endingTrie.edgeChars[e];
      int child = endingTrie.edgeTargets[e];
      searchEnding(
          stemFlags,
          stemTermId,
          child,
          input,
          pos,
          1,
          cost + omittedCharCost(ec),
          diacriticDiffs,
          out);
    }
  }

  private void exploreTranspositionInEnding(
      int node,
      boolean atRoot,
      byte stemFlags,
      int stemTermId,
      char c0,
      char c1,
      String input,
      int pos,
      float cost,
      int diacriticDiffs,
      RawCandidateCollector out) {
    char[] m1 = getMatches(c1);
    char[] m0 = getMatches(c0);
    char[] a1 = m1 != null ? m1 : new char[] {c1};
    char[] a0 = m0 != null ? m0 : new char[] {c0};
    for (char x1 : a1) {
      if (atRoot && !isFirstEndingCharCompatible(x1, stemFlags)) {
        continue;
      }
      int d1 = (x1 == c1) ? 0 : 1;
      int mid = endingTrie.findChild(node, x1);
      if (mid >= 0) {
        for (char x0 : a0) {
          int d0 = (x0 == c0) ? 0 : 1;
          int dst = endingTrie.findChild(mid, x0);
          if (dst >= 0) {
            searchEnding(
                stemFlags,
                stemTermId,
                dst,
                input,
                pos + 2,
                1,
                cost + 0.70f,
                diacriticDiffs + d1 + d0,
                out);
          }
        }
      }
    }
  }

  private static boolean isFirstEndingCharCompatible(char ec, byte stemFlags) {
    if (TurkishAlphabet.INSTANCE.isVowel(ec)) {
      if ((stemFlags & FLAG_ALLOWS_VOWEL_ENDING) == 0) {
        return false;
      }
      if (isFrontVowel(ec)) {
        return (stemFlags & FLAG_FRONT_HARMONY) != 0;
      }
      if (isBackVowel(ec)) {
        return (stemFlags & FLAG_BACK_HARMONY) != 0;
      }
      return true;
    }
    if ((stemFlags & FLAG_ALLOWS_CONSONANT_ENDING) == 0) {
      return false;
    }
    if (ec == 'd' && (stemFlags & FLAG_ALLOWS_VOICED_D) == 0) {
      return false;
    }
    return true;
  }

  private static boolean isEndingHarmoniousWithStem(byte stemFlags, String ending) {
    if (ending.startsWith("yor")
        || ending.startsWith("ken")
        || ending.startsWith("yken")
        || ending.startsWith("ki")
        || ending.startsWith("leyin")
        || ending.startsWith("gil")
        || ending.startsWith("mtırak")
        || ending.startsWith("daş")
        || ending.startsWith("taş")) {
      return true;
    }
    for (int i = 0; i < ending.length(); i++) {
      char c = ending.charAt(i);
      if (TurkishAlphabet.INSTANCE.isVowel(c)) {
        if (isFrontVowel(c)) {
          return (stemFlags & FLAG_FRONT_HARMONY) != 0;
        }
        if (isBackVowel(c)) {
          return (stemFlags & FLAG_BACK_HARMONY) != 0;
        }
        return true;
      }
    }
    return true;
  }

  private static boolean isFrontVowel(char c) {
    return c == 'e' || c == 'i' || c == 'ö' || c == 'ü' || c == 'î';
  }

  private static boolean isBackVowel(char c) {
    return c == 'a' || c == 'ı' || c == 'o' || c == 'u' || c == 'â' || c == 'û';
  }

  private char[] getMatches(char c) {
    if (!matchDiacritics) {
      return null;
    }
    return c < MAX_ASCII_TABLE ? DIACRITIC_MATCHES[c] : null;
  }

  private boolean isDiacriticMatch(char inputChar, char edgeChar) {
    if (!matchDiacritics || inputChar >= MAX_ASCII_TABLE) {
      return false;
    }
    char[] arr = DIACRITIC_MATCHES[inputChar];
    if (arr == null) {
      return false;
    }
    for (char c : arr) {
      if (c == edgeChar) {
        return true;
      }
    }
    return false;
  }

  private static float substitutionCost(char inputChar, char targetChar) {
    if (inputChar < MAX_ASCII_TABLE
        && targetChar < MAX_ASCII_TABLE
        && NEAR_OR_PHONETIC[inputChar][targetChar]) {
      return 0.55f;
    }
    return 1.0f;
  }

  private static float omittedCharCost(char omittedTargetChar) {
    if (TurkishAlphabet.INSTANCE.isVowel(omittedTargetChar)
        || omittedTargetChar == 'ğ'
        || omittedTargetChar == 'y'
        || omittedTargetChar == 'h') {
      return 0.75f;
    }
    return 1.0f;
  }

  private static float deletionFromInputCost(String input, int pos) {
    char c = input.charAt(pos);
    if ((pos > 0 && input.charAt(pos - 1) == c)
        || (pos + 1 < input.length() && input.charAt(pos + 1) == c)) {
      return 0.65f;
    }
    return 1.0f;
  }

  private static String stripApostrophes(String s) {
    if (s.indexOf('\'') < 0 && s.indexOf('’') < 0) {
      return s;
    }
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c != '\'' && c != '’') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static String getApostrophe(String input) {
    if (input.indexOf('’') > 0) {
      return "’";
    } else if (input.indexOf('\'') > 0) {
      return "'";
    }
    return null;
  }

  private static final class RawCandidateCollector {

    private final FloatValueMap<String> bestCombined = new FloatValueMap<>(16);
    private final Map<String, ScoredCandidate> items = new HashMap<>(16);

    void record(String word, float editCost, int diacriticDiffs) {
      float combined = editCost + 0.20f * diacriticDiffs;
      if (!bestCombined.contains(word) || combined < bestCombined.get(word)) {
        bestCombined.set(word, combined);
        items.put(word, new ScoredCandidate(word, editCost, diacriticDiffs));
      }
    }

    List<ScoredCandidate> toSortedList() {
      List<ScoredCandidate> list = new ArrayList<>(items.values());
      list.sort(
          (a, b) -> {
            int cmp =
                Float.compare(
                    a.editCost + 0.20f * a.diacriticDiffs,
                    b.editCost + 0.20f * b.diacriticDiffs);
            return cmp != 0 ? cmp : a.surface.compareTo(b.surface);
          });
      return list;
    }
  }
}
