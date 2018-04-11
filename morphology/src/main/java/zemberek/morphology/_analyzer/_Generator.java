package zemberek.morphology._analyzer;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology._analyzer.InterpretingAnalyzer.AnalysisDebugData;
import zemberek.morphology._analyzer.InterpretingAnalyzer.RejectedTransition;
import zemberek.morphology._analyzer.SurfaceTransition.SuffixTemplateToken;
import zemberek.morphology._analyzer.SurfaceTransition.TemplateTokenType;
import zemberek.morphology._morphotactics.AttributeSet;
import zemberek.morphology._morphotactics.CombinedCondition;
import zemberek.morphology._morphotactics.Condition;
import zemberek.morphology._morphotactics.Morpheme;
import zemberek.morphology._morphotactics.MorphemeTransition;
import zemberek.morphology._morphotactics.StemTransition;
import zemberek.morphology._morphotactics.SuffixTransition;

public class _Generator {


  InterpretingAnalyzer analyzer;

  public List<_SingleAnalysis> generate(
      String stem,
      List<Morpheme> morphemes,
      AnalysisDebugData debugData) {

    // get stem candidates.
    List<StemTransition> candidates = Lists.newArrayListWithCapacity(1);
    candidates.addAll(analyzer.getMatchingStemTransitions(stem));

    if (debugData != null) {
      debugData.input = stem;
      debugData.candidateStemTransitions.addAll(candidates);
    }

    // generate initial search paths.
    List<SearchPath> paths = new ArrayList<>();
    for (StemTransition candidate : candidates) {
      int length = candidate.surface.length();
      String head = stem;
      String tail = "";
      paths.add(SearchPath.initialPath(candidate, head, tail));
    }

    // search graph.
    List<SearchPath> resultPaths = search(paths, debugData);
    // generate results from successful paths.
    List<_SingleAnalysis> result = new ArrayList<>(resultPaths.size());
    for (SearchPath path : resultPaths) {
      _SingleAnalysis analysis = _SingleAnalysis.fromSearchPath(path);
      result.add(analysis);
      if (debugData != null) {
        debugData.results.add(analysis);
      }
    }
    return result;
  }

  public List<_SingleAnalysis> generate(String stem, List<Morpheme> morphemes) {
    return generate(stem, morphemes, null);
  }

  // searches through morphotactics graph.
  private List<SearchPath> search(List<SearchPath> currentPaths, AnalysisDebugData debugData) {

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


}
