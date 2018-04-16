package zemberek.morphology.analyzer;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import zemberek.core.collections.IntValueMap;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.analyzer.AnalysisDebugData.RejectedTransition;
import zemberek.morphology.analyzer.SurfaceTransition.SuffixTemplateToken;
import zemberek.morphology.analyzer.SurfaceTransition.TemplateTokenType;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.CombinedCondition;
import zemberek.morphology.morphotactics.Condition;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * This is a Morphological Analysis implementation.
 */
public class InterpretingAnalyzer {

  private static final int MAX_REPEATING_SUFFIX_TYPE_COUNT = 3;

  private RootLexicon lexicon;
  private StemTransitions stemTransitions;
  private TurkishMorphotactics morphotactics;

  public InterpretingAnalyzer(TurkishMorphotactics morphotactics) {
    this.morphotactics = morphotactics;
    this.lexicon = morphotactics.getRootLexicon();
    this.stemTransitions = morphotactics.getStemTransitions();
  }

  public StemTransitions getStemTransitions() {
    return stemTransitions;
  }

  public TurkishMorphotactics getMorphotactics() {
    return morphotactics;
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  public List<SingleAnalysis> analyze(String input, AnalysisDebugData debugData) {

    // get stem candidates.
    List<StemTransition> candidates = Lists.newArrayListWithCapacity(3);
    for (int i = 1; i <= input.length(); i++) {
      String stem = input.substring(0, i);
      candidates.addAll(stemTransitions.getMatchingStemTransitions(stem));
    }

    if (debugData != null) {
      debugData.input = input;
      debugData.candidateStemTransitions.addAll(candidates);
    }

    // generate initial search paths.
    List<SearchPath> paths = new ArrayList<>();
    for (StemTransition candidate : candidates) {
      int length = candidate.surface.length();
      String head = input.substring(0, length);
      String tail = input.substring(length);
      paths.add(SearchPath.initialPath(candidate, head, tail));
    }

    // search graph.
    List<SearchPath> resultPaths = search(paths, debugData);
    // generate results from successful paths.
    List<SingleAnalysis> result = new ArrayList<>(resultPaths.size());
    for (SearchPath path : resultPaths) {
      SingleAnalysis analysis = SingleAnalysis.fromSearchPath(path);
      result.add(analysis);
      if (debugData != null) {
        debugData.results.add(analysis);
      }
    }
    return result;
  }

  public List<SingleAnalysis> analyze(String input) {
    return analyze(input, null);
  }

  // searches through morphotactics graph.
  private List<SearchPath> search(List<SearchPath> currentPaths, AnalysisDebugData debugData) {

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
              !path.phoneticAttributes.contains(PhoneticAttribute.CannotTerminate)) {
            result.add(path);
            if (debugData != null) {
              debugData.finishedPaths.add(path);
            }
            continue;
          }
          if (debugData != null) {
            debugData.failedPaths.put(path, "Finished but Path not terminal");
          }
        }

        // Creates new paths with outgoing and matching transitions.
        List<SearchPath> newPaths = advance(path, debugData);
        allNewPaths.addAll(newPaths);

        if (debugData != null) {
          if (newPaths.isEmpty()) {
            debugData.failedPaths.put(path, "No Transition");
          }
          debugData.paths.addAll(newPaths);
        }
      }
      currentPaths = allNewPaths;
    }

    if (debugData != null) {
      debugData.resultPaths.addAll(result);
    }

    return result;
  }

  // for all allowed matching outgoing transitions, new paths are generated.
  // Transition conditions are used for checking if a search path is allowed to pass a transition.
  private List<SearchPath> advance(SearchPath path, AnalysisDebugData debugData) {

    List<SearchPath> newPaths = new ArrayList<>(2);

    // for all outgoing transitions.
    for (MorphemeTransition transition : path.currentState.getOutgoing()) {

      SuffixTransition suffixTransition = (SuffixTransition) transition;

      // if tail is empty and this transitions surface is not empty, no need to check.
      if (path.tail.isEmpty() && suffixTransition.hasSurfaceForm()) {
        if (debugData != null) {
          debugData.rejectedTransitions.put(
              path,
              new RejectedTransition(suffixTransition, "Empty surface expected."));
        }
        continue;
      }

      String surface = SurfaceTransition.generate(
          suffixTransition,
          path.phoneticAttributes);

      // no need to go further if generated surface form is not a prefix of the paths's tail.
      if (!path.tail.startsWith(surface)) {
        if (debugData != null) {
          debugData.rejectedTransitions.put(
              path,
              new RejectedTransition(suffixTransition, "Surface Mismatch:" + surface));
        }
        continue;
      }

      // if transition condition fails, add it to debug data.
      if (debugData != null && suffixTransition.getCondition() != null) {
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

      // epsilon transition. Add and continue. Use existing attributes.
      if (!suffixTransition.hasSurfaceForm()) {
        newPaths.add(path.getCopy(
            new SurfaceTransition("", suffixTransition),
            path.phoneticAttributes));
        continue;
      }

      SurfaceTransition surfaceTransition = new SurfaceTransition(surface, suffixTransition);

      //if tail is equal to surface, no need to calculate phonetic attributes.
      AttributeSet<PhoneticAttribute> attributes = path.tail.equals(surface) ?
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
