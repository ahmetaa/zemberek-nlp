package zemberek.morphology.analysis;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import zemberek.core.collections.IntValueMap;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.AnalysisDebugData.RejectedTransition;
import zemberek.morphology.analysis.SurfaceTransition.SuffixTemplateToken;
import zemberek.morphology.analysis.SurfaceTransition.TemplateTokenType;
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
    if (debugMode) {
      debugData = new AnalysisDebugData();
    }
    // get stem candidates.
    List<StemTransition> candidates = stemTransitions.getPrefixMatches(input, asciiTolerant);

    if (debugMode) {
      debugData.input = input;
      debugData.candidateStemTransitions.addAll(candidates);
    }

    // generate initial search paths.
    List<SearchPath> paths = new ArrayList<>();
    for (StemTransition candidate : candidates) {
      int length = candidate.surface.length();
      String tail = input.substring(length);
      paths.add(SearchPath.initialPath(candidate, tail));
    }

    // search graph.
    List<SearchPath> resultPaths = search(paths);

    // generate results from successful paths.
    List<SingleAnalysis> result = new ArrayList<>(resultPaths.size());
    for (SearchPath path : resultPaths) {
      SingleAnalysis analysis = SingleAnalysis.fromSearchPath(path);
      result.add(analysis);
      if (debugMode) {
        debugData.results.add(analysis);
      }
    }
    return result;
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
        List<SearchPath> newPaths = advance(path);
        allNewPaths.addAll(newPaths);

        if (debugMode) {
          if (newPaths.isEmpty()) {
            debugData.failedPaths.put(path, "No Transition");
          }
          debugData.paths.addAll(newPaths);
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
  private List<SearchPath> advance(SearchPath path) {

    List<SearchPath> newPaths = new ArrayList<>(2);

    // for all outgoing transitions.
    for (MorphemeTransition transition : path.currentState.getOutgoing()) {

      SuffixTransition suffixTransition = (SuffixTransition) transition;

      // if tail is empty and this transitions surface is not empty, no need to check.
      if (path.tail.isEmpty() && suffixTransition.hasSurfaceForm()) {
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
      boolean tailStartsWith =
          asciiTolerant ?
              TurkishAlphabet.INSTANCE.startsWithIgnoreDiacritics(path.tail, surface) :
              path.tail.startsWith(surface);
      if (!tailStartsWith) {
        if (debugMode) {
          debugData.rejectedTransitions.put(
              path,
              new RejectedTransition(suffixTransition, "Surface Mismatch:" + surface));
        }
        continue;
      }

      // if transition condition fails, add it to debug data.
      if (debugMode && suffixTransition.getCondition() != null) {
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

      // check conditions.
      if (!suffixTransition.canPass(path)) {
        continue;
      }

      // epsilon (empty) transition. Add and continue. Use existing attributes.
      if (!suffixTransition.hasSurfaceForm()) {
        newPaths.add(path.getCopy(
            new SurfaceTransition("", suffixTransition),
            path.phoneticAttributes));
        continue;
      }

      SurfaceTransition surfaceTransition = new SurfaceTransition(surface, suffixTransition);

      //if tail is equal to surface, no need to calculate phonetic attributes.
      boolean tailEqualsSurface = asciiTolerant ?
          TurkishAlphabet.INSTANCE.equalsIgnoreDiacritics(path.tail, surface)
          : path.tail.equals(surface);
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
    return newPaths;
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
}
