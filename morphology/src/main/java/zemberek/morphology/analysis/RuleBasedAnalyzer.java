package zemberek.morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.AnalysisDebugData.RejectedTransition;
import zemberek.morphology.analysis.SurfaceTransition.TemplateTokenType;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.CombinedCondition;
import zemberek.morphology.morphotactics.Condition;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * This is a Morphological Analyzer implementation. Instances of this class are not thread safe if
 * instantiated with forDebug() factory constructor method.
 */
public class RuleBasedAnalyzer {

  private static final int MAX_REPEATING_SUFFIX_TYPE_COUNT = 3;

  private final RootLexicon lexicon;
  private final StemTransitions stemTransitions;
  private final boolean debugMode;
  private AnalysisDebugData debugData;
  private final boolean asciiTolerant;
  private final TurkishMorphotactics morphotactics;

  private RuleBasedAnalyzer(
      TurkishMorphotactics morphotactics,
      boolean debugMode,
      boolean asciiTolerant) {
    this.lexicon = morphotactics.getRootLexicon();
    this.stemTransitions = morphotactics.getStemTransitions();
    this.morphotactics = morphotactics;
    this.debugMode = debugMode;
    this.asciiTolerant = asciiTolerant;
  }

  public TurkishMorphotactics getMorphotactics() {
    return morphotactics;
  }

  public static RuleBasedAnalyzer instance(TurkishMorphotactics morphotactics) {
    return new RuleBasedAnalyzer(morphotactics, false, false);
  }

  /**
   * Generates a RuleBasedAnalyzer instance that ignores the diacritic marks from the input. As a
   * result, for input `siraci` or `şıraçi`  it generates both analyses "sıracı, şıracı"
   */
  public static RuleBasedAnalyzer ignoreDiacriticsInstance(
      TurkishMorphotactics morphotactics) {
    return new RuleBasedAnalyzer(morphotactics, false, true);
  }

  /**
   * Method returns an RuleBasedAnalyzer instance. But when this factory constructor is used, an
   * AnalysisDebugData object is generated after each call to generation methods. That object cen be
   * retrieved with getDebugData method.
   */
  public static RuleBasedAnalyzer forDebug(TurkishMorphotactics morphotactics) {
    return new RuleBasedAnalyzer(morphotactics, true, false);
  }

  public static RuleBasedAnalyzer forDebug(
      TurkishMorphotactics morphotactics,
      boolean asciiTolerant) {
    return new RuleBasedAnalyzer(morphotactics, true, asciiTolerant);
  }

  public StemTransitions getStemTransitions() {
    return stemTransitions;
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  public AnalysisDebugData getDebugData() {
    return debugData;
  }

  public List<SingleAnalysis> analyze(String input) {
    var searchInput = TurkishAlphabet.INSTANCE.normalizeCircumflex(input);
    return analyze(input, searchInput, searchInput != input);
  }

  public List<SingleAnalysis> analyze(
      String input, String searchInput, boolean hasCircumflex) {
    if (debugMode) {
      debugData = new AnalysisDebugData();
      debugData.input = input;
    }

    // get stem candidates.
    List<StemTransition> candidates = stemTransitions.getPrefixMatches(searchInput, asciiTolerant);
    boolean checkCircumflex = hasCircumflex && !asciiTolerant;
    if (checkCircumflex) {
      var rawCandidates = stemTransitions.getPrefixMatches(input, false);
      if (!rawCandidates.isEmpty()) {
        candidates = new ArrayList<>(candidates);
        candidates.addAll(rawCandidates);
      }
    }

    if (candidates.isEmpty()) {
      return new ArrayList<>(0);
    }

    if (debugMode) {
      debugData.candidateStemTransitions.addAll(candidates);
    }

    // generate initial search paths.
    List<SearchPath> paths = new ArrayList<>(candidates.size());
    for (int i = 0, n = candidates.size(); i < n; i++) {
      StemTransition candidate = candidates.get(i);
      paths.add(SearchPath.initialPath(candidate, searchInput, candidate.surface.length()));
    }

    // search graph.
    List<SearchPath> resultPaths = search(paths);

    // generate results from successful paths.
    List<SingleAnalysis> result = new ArrayList<>(resultPaths.size());
    for (int i = 0, n = resultPaths.size(); i < n; i++) {
      SearchPath path = resultPaths.get(i);
      if (checkCircumflex && !matchesCircumflex(path, input)) {
        continue;
      }
      SingleAnalysis analysis = SingleAnalysis.fromSearchPath(path);
      result.add(analysis);
      if (debugMode) {
        debugData.results.add(analysis);
      }
    }
    if (result.size() > 1) {
      deduplicateAnalyses(result);
      if (debugMode && debugData.results.size() != result.size()) {
        debugData.results.clear();
        debugData.results.addAll(result);
      }
    }
    return result;
  }

  private static boolean matchesCircumflex(SearchPath path, String inputWithCircumflex) {
    int stemLen = path.getStemTransition().surface.length();
    String lemmaStem = null;
    for (int i = 0; i < inputWithCircumflex.length(); i++) {
      char c = inputWithCircumflex.charAt(i);
      if (TurkishAlphabet.INSTANCE.normalizeCircumflex(c) == c) {
        continue;
      }
      // Suffixes do not carry circumflexes; reject if input circumflex falls on a suffix character.
      if (i >= stemLen) {
        return false;
      }
      if (lemmaStem == null) {
        var item = path.getDictionaryItem();
        if (item.hasAttribute(RootAttribute.Dummy)) {
          item = item.getReferenceItem();
        }
        lemmaStem = normalizedLemmaPreservingCircumflex(item);
      }
      if (i >= lemmaStem.length() || lemmaStem.charAt(i) != c) {
        return false;
      }
    }
    return true;
  }

  private static String normalizedLemmaPreservingCircumflex(DictionaryItem item) {
    if (item.primaryPos == PrimaryPos.Punctuation) {
      return item.lemma;
    }
    Locale locale = item.hasAttribute(RootAttribute.LocaleEn) ? Locale.ENGLISH : Turkish.LOCALE;
    String w = TurkishAlphabet.INSTANCE.normalizeApostrophe(item.normalizedLemma().toLowerCase(locale));
    StringBuilder sb = new StringBuilder(w.length());
    for (int i = 0; i < w.length(); i++) {
      char ch = w.charAt(i);
      if (ch != '.' && ch != '-' && ch != '\'') {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  // searches through morphotactics graph.
  private List<SearchPath> search(List<SearchPath> currentPaths) {

    List<SearchPath> result = new ArrayList<>(3);
    List<SearchPath> nextPaths = new ArrayList<>(currentPaths.size() * 2 + 4);
    // new Paths are generated with matching transitions.
    while (!currentPaths.isEmpty()) {

      if (currentPaths.size() > 30) {
        pruneCyclicPathsInPlace(currentPaths);
      }

      nextPaths.clear();

      for (int i = 0, n = currentPaths.size(); i < n; i++) {
        SearchPath path = currentPaths.get(i);

        // if there are no more letters to consume and path can be terminated, we accept this
        // path as a correct result.
        if (path.hasEmptyTail()) {
          if (path.isTerminal() &&
              !path.containsPhoneticAttribute(PhoneticAttribute.CannotTerminate)) {
            result.add(path);
            if (debugMode) {
              debugData.finishedPaths.add(path);
            }
            continue;
          }
          if (debugMode) {
            debugData.failedPaths.put(path, "Finished but Path not terminal");
          } else {
            advanceEmpty(path, nextPaths);
            continue;
          }
        }

        // Creates new paths with outgoing and matching transitions.
        // In non-debug mode, append directly to nextPaths to avoid per-path list allocations.
        if (debugMode) {
          List<SearchPath> newPaths = new ArrayList<>(2);
          advance(path, newPaths);
          nextPaths.addAll(newPaths);
          if (newPaths.isEmpty()) {
            debugData.failedPaths.put(path, "No Transition");
          }
          debugData.paths.addAll(newPaths);
        } else {
          advance(path, nextPaths);
        }
      }
      List<SearchPath> swap = currentPaths;
      currentPaths = nextPaths;
      nextPaths = swap;
    }

    if (debugMode) {
      debugData.resultPaths.addAll(result);
    }

    return result;
  }

  // Fast-path when tail is already empty (non-debug): only epsilon transitions can match.
  private static void advanceEmpty(SearchPath path, List<SearchPath> newPaths) {
    List<SuffixTransition> emptyTransitions = path.currentState.getOutgoingEmpty();
    for (int i = 0, n = emptyTransitions.size(); i < n; i++) {
      SuffixTransition suffixTransition = emptyTransitions.get(i);
      if (suffixTransition.canPass(path)) {
        newPaths.add(path.advanceEmpty(suffixTransition, path.phoneticAttributes));
      }
    }
  }

  // for all allowed matching outgoing transitions, new paths are generated.
  // Transition `conditions` are used for checking if a `search path`
  // is allowed to pass a transition.
  private void advance(SearchPath path, List<SearchPath> newPaths) {

    // for all outgoing transitions.
    List<MorphemeTransition> outgoing = path.currentState.getOutgoing();
    String word = path.word;
    int tailIdx = path.tailIndex;
    int tailLen = word.length() - tailIdx;
    char firstTailChar = tailLen > 0 ? word.charAt(tailIdx) : 0;
    for (int i = 0, n = outgoing.size(); i < n; i++) {

      SuffixTransition suffixTransition = (SuffixTransition) outgoing.get(i);

      // Fast-path epsilon (empty) transitions: skip surface generation and prefix matching.
      if (!suffixTransition.hasSurfaceForm()) {
        if (!debugMode ? !suffixTransition.canPass(path) : !checkDebugCondition(path, suffixTransition)) {
          continue;
        }
        newPaths.add(path.advanceEmpty(suffixTransition, path.phoneticAttributes));
        continue;
      }

      // if tail is empty and this transitions surface is not empty, no need to check.
      if (tailLen == 0) {
        if (debugMode) {
          debugData.rejectedTransitions.put(
              path,
              new RejectedTransition(suffixTransition, "Empty surface expected."));
        }
        continue;
      }

      String surface = SurfaceTransition.generateSurface(
          suffixTransition,
          path.phoneticAttributes);

      // no need to go further if generated surface form is not a prefix of the paths's tail.
      // Check length and first char before calling startsWith to avoid mismatch overhead.
      int surfLen = surface.length();
      boolean tailStartsWith = asciiTolerant
          ? TurkishAlphabet.INSTANCE.startsWithIgnoreDiacritics(word, tailIdx, surface)
          : (surfLen == 0
              || (surfLen <= tailLen
                  && firstTailChar == surface.charAt(0)
                  && (surfLen == 1 || word.startsWith(surface, tailIdx))));
      if (!tailStartsWith) {
        if (debugMode) {
          debugData.rejectedTransitions.put(
              path,
              new RejectedTransition(suffixTransition, "Surface Mismatch:" + surface));
        }
        continue;
      }

      // check conditions.
      if (!debugMode ? !suffixTransition.canPass(path) : !checkDebugCondition(path, suffixTransition)) {
        continue;
      }

      // If tail equals surface, no need to recalculate phonetic attributes.
      // Reuse path.phoneticAttributes directly when no attribute flag changes.
      TemplateTokenType lastTokenType = suffixTransition.getLastTemplateToken().type;
      AttributeSet<PhoneticAttribute> attributes;
      if (tailLen == surfLen) {
        boolean needsChange = path.phoneticAttributes.contains(PhoneticAttribute.CannotTerminate)
            || lastTokenType == TemplateTokenType.LAST_VOICED
            || lastTokenType == TemplateTokenType.LAST_NOT_VOICED;
        attributes = needsChange ? path.phoneticAttributes.copy() : path.phoneticAttributes;
      } else {
        attributes = AttributesHelper.getMorphemicAttributes(surface, path.phoneticAttributes);
      }

      // This is required for suffixes like `cik` and `ciğ`
      // an extra attribute is added if "cik" or "ciğ" is generated and matches the tail.
      // if "cik" is generated, ExpectsConsonant attribute is added, so only a consonant starting
      // suffix can follow. Likewise, if "ciğ" is produced, a vowel starting suffix is allowed.
      if (attributes != path.phoneticAttributes) {
        attributes.remove(PhoneticAttribute.CannotTerminate);
        if (lastTokenType == TemplateTokenType.LAST_VOICED) {
          attributes.add(PhoneticAttribute.ExpectsConsonant);
        } else if (lastTokenType == TemplateTokenType.LAST_NOT_VOICED) {
          attributes.add(PhoneticAttribute.ExpectsVowel);
          attributes.add(PhoneticAttribute.CannotTerminate);
        }
      }

      newPaths.add(path.advance(surface, suffixTransition, attributes));
    }
  }

  // if transition condition fails, add it to debug data.
  private boolean checkDebugCondition(SearchPath path, SuffixTransition suffixTransition) {
    Condition condition = suffixTransition.getCondition();
    if (condition == null) {
      return true;
    }
    Condition failed = (condition instanceof CombinedCondition)
        ? ((CombinedCondition) condition).getFailingCondition(path)
        : (condition.accept(path) ? null : condition);
    if (failed != null) {
      debugData.rejectedTransitions.put(
          path,
          new RejectedTransition(suffixTransition, "Condition → " + failed.toString()));
      return false;
    }
    return true;
  }

  // for preventing excessive branching during search, we remove paths that have more than
  // MAX_REPEATING_SUFFIX_TYPE_COUNT identical morpheme states.
  private static void pruneCyclicPathsInPlace(List<SearchPath> paths) {
    int write = 0;
    for (int i = 0, n = paths.size(); i < n; i++) {
      SearchPath path = paths.get(i);
      if (!hasExcessiveCycle(path)) {
        if (write != i) {
          paths.set(write, path);
        }
        write++;
      }
    }
    if (write < paths.size()) {
      paths.subList(write, paths.size()).clear();
    }
  }

  private static boolean hasExcessiveCycle(SearchPath path) {
    if (path.getLength() <= MAX_REPEATING_SUFFIX_TYPE_COUNT) {
      return false;
    }
    for (SearchPath outer = path; outer != null; outer = outer.previous) {
      String id = outer.currentState.id;
      int count = 1;
      for (SearchPath inner = outer.previous; inner != null; inner = inner.previous) {
        if (id.equals(inner.currentState.id)) {
          if (++count > MAX_REPEATING_SUFFIX_TYPE_COUNT) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static void deduplicateAnalyses(List<SingleAnalysis> analyses) {
    int size = analyses.size();
    for (int i = 1; i < size; i++) {
      var candidate = analyses.get(i);
      for (int j = 0; j < i; j++) {
        var existing = analyses.get(j);
        if (isSameAnalysis(existing, candidate)) {
          if (candidate.getDictionaryItem().index < existing.getDictionaryItem().index) {
            analyses.set(j, candidate);
          }
          analyses.remove(i);
          i--;
          size--;
          break;
        }
      }
    }
  }

  private static boolean isSameAnalysis(SingleAnalysis a, SingleAnalysis b) {
    if (a.groupCount() != b.groupCount()) {
      return false;
    }
    var ia = a.getDictionaryItem();
    var ib = b.getDictionaryItem();
    return ia.primaryPos == ib.primaryPos
        && ia.secondaryPos == ib.secondaryPos
        && (ia == ib || ia.lemma.equals(ib.lemma))
        && a.getMorphemeDataList().equals(b.getMorphemeDataList());
  }
}
