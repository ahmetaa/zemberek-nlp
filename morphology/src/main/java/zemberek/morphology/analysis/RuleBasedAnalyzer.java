package zemberek.morphology.analysis;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import zemberek.core.collections.IntValueMap;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.Turkish;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.AnalysisDebugData.RejectedTransition;
import zemberek.morphology.analysis.SurfaceTransition.SuffixTemplateToken;
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

  private RootLexicon lexicon;
  private StemTransitions stemTransitions;
  private boolean debugMode = false;
  private AnalysisDebugData debugData;
  private boolean asciiTolerant = false;
  private TurkishMorphotactics morphotactics;

  private RuleBasedAnalyzer(TurkishMorphotactics morphotactics) {
    this.lexicon = morphotactics.getRootLexicon();
    this.stemTransitions = morphotactics.getStemTransitions();
    this.morphotactics = morphotactics;
  }

  public TurkishMorphotactics getMorphotactics() {
    return morphotactics;
  }

  public static RuleBasedAnalyzer instance(TurkishMorphotactics morphotactics) {
    return new RuleBasedAnalyzer(morphotactics);
  }

  /**
   * Generates a RuleBasedAnalyzer instance that ignores the diacritic marks from the input. As a
   * result, for input `siraci` or `şıraçi`  it generates both analyses "sıracı, şıracı"
   */
  public static RuleBasedAnalyzer ignoreDiacriticsInstance(
      TurkishMorphotactics morphotactics) {
    RuleBasedAnalyzer analyzer = RuleBasedAnalyzer.instance(morphotactics);
    analyzer.asciiTolerant = true;
    return analyzer;
  }

  /**
   * Method returns an RuleBasedAnalyzer instance. But when this factory constructor is used, an
   * AnalysisDebugData object is generated after each call to generation methods. That object cen be
   * retrieved with getDebugData method.
   */
  public static RuleBasedAnalyzer forDebug(TurkishMorphotactics morphotactics) {
    RuleBasedAnalyzer analyzer = RuleBasedAnalyzer.instance(morphotactics);
    analyzer.debugMode = true;
    return analyzer;
  }

  public static RuleBasedAnalyzer forDebug(
      TurkishMorphotactics morphotactics,
      boolean asciiTolerant) {
    RuleBasedAnalyzer analyzer = RuleBasedAnalyzer
        .instance(morphotactics);
    analyzer.debugMode = true;
    analyzer.asciiTolerant = asciiTolerant;
    return analyzer;
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

    if (debugMode) {
      debugData.input = input;
      debugData.candidateStemTransitions.addAll(candidates);
    }

    // generate initial search paths.
    List<SearchPath> paths = new ArrayList<>(candidates.size());
    for (StemTransition candidate : candidates) {
      int length = candidate.surface.length();
      String tail = searchInput.substring(length);
      paths.add(SearchPath.initialPath(candidate, tail));
    }

    // search graph.
    List<SearchPath> resultPaths = search(paths);

    // generate results from successful paths.
    List<SingleAnalysis> result = new ArrayList<>(resultPaths.size());
    for (SearchPath path : resultPaths) {
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

    if (currentPaths.size() > 30) {
      currentPaths = pruneCyclicPaths(currentPaths);
    }

    List<SearchPath> result = new ArrayList<>(3);
    // new Paths are generated with matching transitions.
    while (currentPaths.size() > 0) {

      List<SearchPath> allNewPaths = Lists.newArrayList();

      for (SearchPath path : currentPaths) {

        // if there are no more letters to consume and path can be terminated, we accept this
        // path as a correct result.
        if (path.tail.length() == 0) {
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
          }
        }

        // Creates new paths with outgoing and matching transitions.
        // In non-debug mode, append directly to allNewPaths to avoid per-path list allocations.
        if (debugMode) {
          List<SearchPath> newPaths = new ArrayList<>(2);
          advance(path, newPaths);
          allNewPaths.addAll(newPaths);
          if (newPaths.isEmpty()) {
            debugData.failedPaths.put(path, "No Transition");
          }
          debugData.paths.addAll(newPaths);
        } else {
          advance(path, allNewPaths);
        }
      }
      currentPaths = allNewPaths;
    }

    if (debugMode) {
      debugData.resultPaths.addAll(result);
    }

    return result;
  }

  // for all allowed matching outgoing transitions, new paths are generated.
  // Transition `conditions` are used for checking if a `search path`
  // is allowed to pass a transition.
  private void advance(SearchPath path, List<SearchPath> newPaths) {

    // for all outgoing transitions.
    for (MorphemeTransition transition : path.currentState.getOutgoing()) {

      SuffixTransition suffixTransition = (SuffixTransition) transition;

      // Fast-path epsilon (empty) transitions: skip surface generation and prefix matching.
      if (!suffixTransition.hasSurfaceForm()) {
        if (!debugMode ? !suffixTransition.canPass(path) : !checkDebugCondition(path, suffixTransition)) {
          continue;
        }
        newPaths.add(path.getCopy(
            new SurfaceTransition("", suffixTransition),
            path.phoneticAttributes));
        continue;
      }

      // if tail is empty and this transitions surface is not empty, no need to check.
      if (path.tail.isEmpty()) {
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
      // Check first char before calling startsWith to avoid ArraysSupport.mismatch overhead.
      boolean tailStartsWith =
          asciiTolerant ?
              TurkishAlphabet.INSTANCE.startsWithIgnoreDiacritics(path.tail, surface) :
              (surface.isEmpty()
                  || (path.tail.charAt(0) == surface.charAt(0) && path.tail.startsWith(surface)));
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

      SurfaceTransition surfaceTransition = new SurfaceTransition(surface, suffixTransition);

      //if tail is equal to surface, no need to calculate phonetic attributes.
      // Since tailStartsWith is already true, equality holds iff lengths match.
      boolean tailEqualsSurface = path.tail.length() == surface.length();
      AttributeSet<PhoneticAttribute> attributes = tailEqualsSurface ?
          path.phoneticAttributes.copy() :
          AttributesHelper.getMorphemicAttributes(surface, path.phoneticAttributes);

      // This is required for suffixes like `cik` and `ciğ`
      // an extra attribute is added if "cik" or "ciğ" is generated and matches the tail.
      // if "cik" is generated, ExpectsConsonant attribute is added, so only a consonant starting
      // suffix can follow. Likewise, if "ciğ" is produced, a vowel starting suffix is allowed.
      attributes.remove(PhoneticAttribute.CannotTerminate);
      SuffixTemplateToken lastToken = suffixTransition.getLastTemplateToken();
      if (lastToken.type == TemplateTokenType.LAST_VOICED) {
        attributes.add(PhoneticAttribute.ExpectsConsonant);
      } else if (lastToken.type == TemplateTokenType.LAST_NOT_VOICED) {
        attributes.add(PhoneticAttribute.ExpectsVowel);
        attributes.add(PhoneticAttribute.CannotTerminate);
      }

      SearchPath p = path.getCopy(
          surfaceTransition,
          attributes);
      newPaths.add(p);
    }
  }

  // if transition condition fails, add it to debug data.
  private boolean checkDebugCondition(SearchPath path, SuffixTransition suffixTransition) {
    if (suffixTransition.getCondition() != null) {
      Condition condition = suffixTransition.getCondition();
      Condition failed;
      if (condition instanceof CombinedCondition) {
        failed = ((CombinedCondition) condition).getFailingCondition(path);
      } else {
        failed = condition.accept(path) ? null : condition;
      }
      if (failed != null) {
        debugData.rejectedTransitions.put(
            path,
            new RejectedTransition(suffixTransition, "Condition → " + failed.toString()));
      }
    }
    return suffixTransition.canPass(path);
  }

  // for preventing excessive branching during search, we remove paths that has more than
  // MAX_REPEATING_SUFFIX_TYPE_COUNT morpheme-state types.
  private List<SearchPath> pruneCyclicPaths(List<SearchPath> tokens) {
    List<SearchPath> result = new ArrayList<>();
    for (SearchPath token : tokens) {
      boolean remove = false;
      IntValueMap<String> typeCounts = new IntValueMap<>(10);
      for (SurfaceTransition node : token.transitions) {
        if (typeCounts.addOrIncrement(node.getState().id) > MAX_REPEATING_SUFFIX_TYPE_COUNT) {
          remove = true;
          break;
        }
      }
      if (!remove) {
        result.add(token);
      }
    }
    return result;
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
