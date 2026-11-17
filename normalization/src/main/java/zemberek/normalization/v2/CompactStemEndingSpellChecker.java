package zemberek.normalization.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysisSurfaceFormatter;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.StemTransition;

/**
 * Low-memory Turkish stem+ending spell checker backed by flat CSR primitive tries, Oflazer
 * remaining-length bounds, stem-ending harmony pruning, and weighted keyboard/diacritic edit costs.
 */
public final class CompactStemEndingSpellChecker {

  private static final byte FLAG_CAN_TERMINATE = 1;
  private static final byte FLAG_ALLOWS_VOWEL_ENDING = 1 << 1;
  private static final byte FLAG_ALLOWS_CONSONANT_ENDING = 1 << 2;
  private static final byte FLAG_HAS_STEM = 1 << 3;
  private static final byte FLAG_FRONT_HARMONY = 1 << 4;
  private static final byte FLAG_BACK_HARMONY = 1 << 5;
  private static final byte FLAG_ALLOWS_VOICED_D = 1 << 6;

  private static final String[] INVARIANT_HARMONY_PREFIXES = {
    "yor", "ken", "yken", "ki", "leyin", "gil", "mtırak", "daş", "taş"
  };

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
    DIACRITIC_MATCHES['a'] = new char[] {'a', 'â'};
    DIACRITIC_MATCHES['c'] = new char[] {'c', 'ç'};
    DIACRITIC_MATCHES['g'] = new char[] {'g', 'ğ'};
    DIACRITIC_MATCHES['ı'] = new char[] {'ı', 'i'};
    DIACRITIC_MATCHES['i'] = new char[] {'i', 'ı', 'î'};
    DIACRITIC_MATCHES['o'] = new char[] {'o', 'ö'};
    DIACRITIC_MATCHES['s'] = new char[] {'s', 'ş'};
    DIACRITIC_MATCHES['u'] = new char[] {'u', 'ü', 'û'};

    DIACRITIC_MATCHES['A'] = new char[] {'A', 'Â'};
    DIACRITIC_MATCHES['C'] = new char[] {'C', 'Ç'};
    DIACRITIC_MATCHES['G'] = new char[] {'G', 'Ğ'};
    DIACRITIC_MATCHES['I'] = new char[] {'I', 'İ'};
    DIACRITIC_MATCHES['İ'] = new char[] {'İ', 'I', 'Î'};
    DIACRITIC_MATCHES['O'] = new char[] {'O', 'Ö'};
    DIACRITIC_MATCHES['S'] = new char[] {'S', 'Ş'};
    DIACRITIC_MATCHES['U'] = new char[] {'U', 'Ü', 'Û'};

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

  private static void registerPairs(char c, String neighbors) {
    for (int i = 0; i < neighbors.length(); i++) {
      char n = neighbors.charAt(i);
      if (c < MAX_ASCII_TABLE && n < MAX_ASCII_TABLE) {
        NEAR_OR_PHONETIC[c][n] = true;
        NEAR_OR_PHONETIC[n][c] = true;
      }
    }
  }

  public record ScoredCandidate(String surface, float editCost, int diacriticDiffs) {
    float rawRankScore() {
      return editCost + 0.20f * diacriticDiffs;
    }

    float totalDistanceCost() {
      return editCost + 0.28f * diacriticDiffs;
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

  record FlatTrie(
      int[] edgeStart,
      short[] edgeCount,
      char[] edgeChars,
      int[] edgeTargets,
      int[] terminalId,
      byte[] flags,
      byte[] minRem,
      byte[] maxRem,
      String[] terminals) {

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
  private final Map<String, List<ScoredCandidate>> suggestionCache;

  public CompactStemEndingSpellChecker(TurkishMorphology morphology) throws IOException {
    this(morphology, TextIO.loadLinesFromResource("endings"), 16_384);
  }

  public CompactStemEndingSpellChecker(
      TurkishMorphology morphology, List<String> endings, int maxCacheSize) {
    this.morphology = morphology;
    this.endingTrie = buildEndingTrie(endings);
    this.stemTrie = buildStemTrie(morphology, this.endingTrie.maxRem()[0]);
    this.suggestionCache =
        maxCacheSize > 0
            ? Collections.synchronizedMap(
                new LinkedHashMap<>(1024, 0.75f, true) {
                  @Override
                  protected boolean removeEldestEntry(
                      Map.Entry<String, List<ScoredCandidate>> eldest) {
                    return size() > maxCacheSize;
                  }
                })
            : null;
  }

  // =========================================================================
  // CSR Trie Construction
  // =========================================================================

  private static FlatTrie buildEndingTrie(List<String> endings) {
    TempNode root = new TempNode();
    List<String> termList = new ArrayList<>(endings.size());
    for (String ending : endings) {
      if (ending.isEmpty()) {
        continue;
      }
      TempNode cur = insertPath(root, ending);
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
      TempNode cur = insertPath(root, tr.surface);
      if (cur.terminalId < 0) {
        cur.terminalId = termList.size();
        termList.add(tr.surface);
      }
      cur.flags |= stemFlagsForTransition(tr.getPhoneticAttributes());
    }
    return freeze(root, termList, maxEndingLength);
  }

  private static TempNode insertPath(TempNode root, String surface) {
    TempNode cur = root;
    for (int i = 0; i < surface.length(); i++) {
      cur = cur.getOrAdd(surface.charAt(i));
    }
    return cur;
  }

  private static byte stemFlagsForTransition(AttributeSet<PhoneticAttribute> attrs) {
    byte flags = FLAG_HAS_STEM;
    if (!attrs.contains(PhoneticAttribute.CannotTerminate)) {
      flags |= FLAG_CAN_TERMINATE;
    }
    if (!attrs.contains(PhoneticAttribute.ExpectsConsonant)
        && !attrs.contains(PhoneticAttribute.LastLetterVowel)) {
      flags |= FLAG_ALLOWS_VOWEL_ENDING;
    }
    if (!attrs.contains(PhoneticAttribute.ExpectsVowel)) {
      flags |= FLAG_ALLOWS_CONSONANT_ENDING;
    }
    if (attrs.contains(PhoneticAttribute.LastVowelFrontal)
        || attrs.contains(PhoneticAttribute.HasNoVowel)) {
      flags |= FLAG_FRONT_HARMONY;
    }
    if (attrs.contains(PhoneticAttribute.LastVowelBack)
        || attrs.contains(PhoneticAttribute.HasNoVowel)) {
      flags |= FLAG_BACK_HARMONY;
    }
    if (!attrs.contains(PhoneticAttribute.LastLetterVoiceless)) {
      flags |= FLAG_ALLOWS_VOICED_D;
    }
    return flags;
  }

  private static FlatTrie freeze(TempNode root, List<String> termList, int maxEndingLength) {
    List<TempNode> order = new ArrayList<>(64_000);
    collectPreOrder(root, order);
    int n = order.size();
    int totalEdges = 0;
    Map<TempNode, Integer> nodeToIdx = new IdentityHashMap<>(n);
    for (int i = 0; i < n; i++) {
      TempNode node = order.get(i);
      nodeToIdx.put(node, i);
      totalEdges += node.childChars.length;
    }

    int[] edgeStart = new int[n];
    short[] edgeCount = new short[n];
    char[] edgeChars = new char[totalEdges];
    int[] edgeTargets = new int[totalEdges];
    int[] terminalId = new int[n];
    byte[] flags = new byte[n];

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

    byte[][] remBounds =
        computeRemainingBounds(n, edgeStart, edgeCount, edgeTargets, flags, maxEndingLength);
    return new FlatTrie(
        edgeStart,
        edgeCount,
        edgeChars,
        edgeTargets,
        terminalId,
        flags,
        remBounds[0],
        remBounds[1],
        termList.toArray(new String[0]));
  }

  private static byte[][] computeRemainingBounds(
      int n,
      int[] edgeStart,
      short[] edgeCount,
      int[] edgeTargets,
      byte[] flags,
      int maxEndingLength) {
    int[] minRemInt = new int[n];
    int[] maxRemInt = new int[n];
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
    return new byte[][] {minRem, maxRem};
  }

  private static void collectPreOrder(TempNode node, List<TempNode> out) {
    out.add(node);
    for (TempNode child : node.childNodes) {
      collectPreOrder(child, out);
    }
  }

  // =========================================================================
  // Bounded Edit-Distance Stem + Ending Search
  // =========================================================================

  /**
   * Returns morphologically valid, case-formatted candidate words together with their edit and
   * diacritic distances.
   */
  public List<ScoredCandidate> suggestScored(String word) {
    if (suggestionCache != null) {
      List<ScoredCandidate> cached = suggestionCache.get(word);
      if (cached != null) {
        return cached;
      }
    }

    String normalized = TurkishAlphabet.INSTANCE.normalize(stripStandardApostrophes(word));
    if (normalized.isEmpty()) {
      return Collections.emptyList();
    }

    RawCandidateCollector rawCollector = new RawCandidateCollector();
    searchStem(0, normalized, 0, 0, 0f, 0, rawCollector);
    List<ScoredCandidate> result = formatAndFilterCandidates(word, rawCollector.toSortedList());

    if (suggestionCache != null) {
      suggestionCache.put(word, result);
    }
    return result;
  }

  private List<ScoredCandidate> formatAndFilterCandidates(
      String word, List<ScoredCandidate> rawList) {
    WordAnalysisSurfaceFormatter.CaseType caseType = formatter.guessCase(word);
    if (caseType == WordAnalysisSurfaceFormatter.CaseType.MIXED_CASE
        || caseType == WordAnalysisSurfaceFormatter.CaseType.LOWER_CASE) {
      caseType = WordAnalysisSurfaceFormatter.CaseType.DEFAULT_CASE;
    }
    String apostrophe = getApostrophe(word);

    int limit = Math.min(rawList.size(), 120);
    Map<String, ScoredCandidate> validMap = new LinkedHashMap<>(16);
    float bestValidScore = Float.POSITIVE_INFINITY;
    for (int i = 0; i < limit; i++) {
      ScoredCandidate rc = rawList.get(i);
      float rcScore = rc.rawRankScore();
      if (validMap.size() >= 18 && rcScore > 0.85f && rcScore > bestValidScore + 0.50f) {
        break;
      }
      for (SingleAnalysis analysis : morphology.analyze(rc.surface())) {
        if (analysis.isUnknown()) {
          continue;
        }
        String formatted = formatter.formatToCase(analysis, caseType, apostrophe);
        if (rcScore < bestValidScore) {
          bestValidScore = rcScore;
        }
        ScoredCandidate existing = validMap.get(formatted);
        if (existing == null || rc.totalDistanceCost() < existing.totalDistanceCost()) {
          validMap.put(formatted, new ScoredCandidate(formatted, rc.editCost(), rc.diacriticDiffs()));
        }
      }
    }
    return Collections.unmodifiableList(new ArrayList<>(validMap.values()));
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
      searchEnding(f, stemTermId, 0, input, pos, edits, cost, diacriticDiffs, out);
    }

    if (edits >= 1) {
      if (pos < len) {
        char ch = input.charAt(pos);
        for (char mc : matchOrSelf(ch)) {
          int child = stemTrie.findChild(node, mc);
          if (child >= 0) {
            searchStem(child, input, pos + 1, 1, cost, diacriticDiffs + (mc == ch ? 0 : 1), out);
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
          searchStem(child, input, pos + 1, 1, cost + substitutionCost(ch, ec), diacriticDiffs, out);
        }
      }
      searchStem(node, input, pos + 1, 1, cost + deletionFromInputCost(input, pos), diacriticDiffs, out);
      if (len > 2 && pos + 1 < len && ch != input.charAt(pos + 1)) {
        exploreTranspositionInStem(node, ch, input.charAt(pos + 1), input, pos, cost, diacriticDiffs, out);
      }
    }

    for (int e = start; e < end; e++) {
      searchStem(
          stemTrie.edgeTargets[e],
          input,
          pos,
          1,
          cost + omittedCharCost(stemTrie.edgeChars[e]),
          diacriticDiffs,
          out);
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
    char[] a1 = matchOrSelf(c1);
    char[] a0 = matchOrSelf(c0);
    for (char x1 : a1) {
      int d1 = x1 == c1 ? 0 : 1;
      int mid = stemTrie.findChild(node, x1);
      if (mid >= 0) {
        for (char x0 : a0) {
          int d0 = x0 == c0 ? 0 : 1;
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
    if (node == 0
        && (stemFlags & (FLAG_ALLOWS_VOWEL_ENDING | FLAG_ALLOWS_CONSONANT_ENDING)) == 0) {
      return;
    }
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
        for (char mc : matchOrSelf(ch)) {
          stepEndingExact(
              stemFlags,
              stemTermId,
              node,
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
      return;
    }

    int start = endingTrie.edgeStart[node];
    int end = start + endingTrie.edgeCount[node];
    if (pos < len) {
      char ch = input.charAt(pos);
      for (int e = start; e < end; e++) {
        char ec = endingTrie.edgeChars[e];
        if (node == 0 && !isFirstEndingCharCompatible(ec, stemFlags)) {
          continue;
        }
        int child = endingTrie.edgeTargets[e];
        if (ec == ch) {
          searchEnding(stemFlags, stemTermId, child, input, pos + 1, 0, cost, diacriticDiffs, out);
        } else if (isDiacriticMatch(ch, ec)) {
          searchEnding(
              stemFlags, stemTermId, child, input, pos + 1, 0, cost, diacriticDiffs + 1, out);
        } else {
          searchEnding(
              stemFlags,
              stemTermId,
              child,
              input,
              pos + 1,
              1,
              cost + substitutionCost(ch, ec),
              diacriticDiffs,
              out);
        }
      }
      if (node != 0) {
        searchEnding(
            stemFlags,
            stemTermId,
            node,
            input,
            pos + 1,
            1,
            cost + deletionFromInputCost(input, pos),
            diacriticDiffs,
            out);
      }
      if (len > 2 && pos + 1 < len && ch != input.charAt(pos + 1)) {
        exploreTranspositionInEnding(
            node,
            node == 0,
            stemFlags,
            stemTermId,
            ch,
            input.charAt(pos + 1),
            input,
            pos,
            cost,
            diacriticDiffs,
            out);
      }
    }

    for (int e = start; e < end; e++) {
      char ec = endingTrie.edgeChars[e];
      if (node == 0 && !isFirstEndingCharCompatible(ec, stemFlags)) {
        continue;
      }
      searchEnding(
          stemFlags,
          stemTermId,
          endingTrie.edgeTargets[e],
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
    char[] a1 = matchOrSelf(c1);
    char[] a0 = matchOrSelf(c0);
    for (char x1 : a1) {
      if (atRoot && !isFirstEndingCharCompatible(x1, stemFlags)) {
        continue;
      }
      int d1 = x1 == c1 ? 0 : 1;
      int mid = endingTrie.findChild(node, x1);
      if (mid >= 0) {
        for (char x0 : a0) {
          int d0 = x0 == c0 ? 0 : 1;
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

  // =========================================================================
  // Morphotactic Harmony & Edit-Cost Helpers
  // =========================================================================

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
    return (stemFlags & FLAG_ALLOWS_CONSONANT_ENDING) != 0
        && (ec != 'd' || (stemFlags & FLAG_ALLOWS_VOICED_D) != 0);
  }

  private static boolean isEndingHarmoniousWithStem(byte stemFlags, String ending) {
    for (String prefix : INVARIANT_HARMONY_PREFIXES) {
      if (ending.startsWith(prefix)) {
        return true;
      }
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

  private static char[] matchOrSelf(char c) {
    char[] matches = c < MAX_ASCII_TABLE ? DIACRITIC_MATCHES[c] : null;
    return matches != null ? matches : new char[] {c};
  }

  private static boolean isDiacriticMatch(char inputChar, char edgeChar) {
    if (inputChar >= MAX_ASCII_TABLE) {
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
    return inputChar < MAX_ASCII_TABLE
            && targetChar < MAX_ASCII_TABLE
            && NEAR_OR_PHONETIC[inputChar][targetChar]
        ? 0.55f
        : 1.0f;
  }

  private static float omittedCharCost(char omittedTargetChar) {
    return TurkishAlphabet.INSTANCE.isVowel(omittedTargetChar)
            || omittedTargetChar == 'ğ'
            || omittedTargetChar == 'y'
            || omittedTargetChar == 'h'
        ? 0.75f
        : 1.0f;
  }

  private static float deletionFromInputCost(String input, int pos) {
    char c = input.charAt(pos);
    return (pos > 0 && input.charAt(pos - 1) == c)
            || (pos + 1 < input.length() && input.charAt(pos + 1) == c)
        ? 0.65f
        : 1.0f;
  }

  private static String stripStandardApostrophes(String s) {
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
    }
    return input.indexOf('\'') > 0 ? "'" : null;
  }

  private static final class RawCandidateCollector {
    private final Map<String, ScoredCandidate> items = new HashMap<>(16);

    void record(String word, float editCost, int diacriticDiffs) {
      ScoredCandidate cand = new ScoredCandidate(word, editCost, diacriticDiffs);
      ScoredCandidate existing = items.get(word);
      if (existing == null || cand.rawRankScore() < existing.rawRankScore()) {
        items.put(word, cand);
      }
    }

    List<ScoredCandidate> toSortedList() {
      List<ScoredCandidate> list = new ArrayList<>(items.values());
      list.sort(
          (a, b) -> {
            int cmp = Float.compare(a.rawRankScore(), b.rawRankScore());
            return cmp != 0 ? cmp : a.surface().compareTo(b.surface());
          });
      return list;
    }
  }
}
