package zemberek.morphology._analyzer;

import static java.lang.String.format;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology._analyzer.SurfaceTransition.SuffixTemplateToken;
import zemberek.morphology._analyzer.SurfaceTransition.TemplateTokenType;
import zemberek.morphology._morphotactics.AttributeSet;
import zemberek.morphology._morphotactics.CombinedCondition;
import zemberek.morphology._morphotactics.Condition;
import zemberek.morphology._morphotactics.MorphemeTransition;
import zemberek.morphology._morphotactics.StemTransition;
import zemberek.morphology._morphotactics.SuffixTransition;
import zemberek.morphology._morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * This is a primitive _analyzer.
 */
public class InterpretingAnalyzer {

  private RootLexicon lexicon;

  private TurkishMorphotactics morphotactics;

  // TODO: Move this to somewhere else. Also this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems =
      ArrayListMultimap.create(1000, 2);
  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();

  public InterpretingAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    morphotactics = new TurkishMorphotactics(lexicon);
    generateStemTransitions(morphotactics);
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  public List<_SingleAnalysis> analyze(String input, AnalysisDebugData debugData) {

    // get stem candidates.
    List<StemTransition> candidates = Lists.newArrayListWithCapacity(3);
    for (int i = 1; i <= input.length(); i++) {
      String stem = input.substring(0, i);
      candidates.addAll(getMatchingStemTransitions(stem));
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

  public List<_SingleAnalysis> analyze(String input) {
    return analyze(input, null);
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

  private void generateStemTransitions(TurkishMorphotactics morphotactics) {
    StemTransitionGenerator generator = new StemTransitionGenerator(morphotactics);
    for (DictionaryItem item : lexicon) {
      try {
        List<StemTransition> transitions = generator.generate(item);
        for (StemTransition transition : transitions) {
          addStemTransition(transition);
        }
      } catch (Exception e) {
        Log.warn("Cannot generate stem transition for %s with reason %s", item, e.getMessage());
      }
    }
  }

  private synchronized void addStemTransition(StemTransition stemTransition) {
    final String surfaceForm = stemTransition.surface;
    if (multiStems.containsKey(surfaceForm)) {
      multiStems.put(surfaceForm, stemTransition);
    } else if (singleStems.containsKey(surfaceForm)) {
      multiStems.put(surfaceForm, singleStems.get(surfaceForm));
      singleStems.remove(surfaceForm);
      multiStems.put(surfaceForm, stemTransition);
    } else {
      singleStems.put(surfaceForm, stemTransition);
    }
  }

  public List<StemTransition> getMatchingStemTransitions(String stem) {
    if (singleStems.containsKey(stem)) {
      return Lists.newArrayList(singleStems.get(stem));
    } else if (multiStems.containsKey(stem)) {
      return Lists.newArrayList(multiStems.get(stem));
    } else {
      return Collections.emptyList();
    }
  }

  static class RejectedTransition {

    SuffixTransition transition;
    String reason;

    RejectedTransition(SuffixTransition transition, String reason) {
      this.transition = transition;
      this.reason = reason;
    }

    @Override
    public String toString() {
      return transition.toString() + " " + reason;
    }
  }

  public static class AnalysisDebugData {

    String input;
    List<StemTransition> candidateStemTransitions = new ArrayList<>();
    List<SearchPath> paths = new ArrayList<>();
    Map<SearchPath, String> failedPaths = new HashMap<>();
    Set<SearchPath> finishedPaths = new LinkedHashSet<>();
    Multimap<SearchPath, RejectedTransition> rejectedTransitions = ArrayListMultimap.create();
    List<_SingleAnalysis> results = new ArrayList<>();
    List<SearchPath> resultPaths = new ArrayList<>();

    List<String> detailedInfo() {
      List<String> l = new ArrayList<>();
      l.add("----------------------");
      l.add("Debug data for input = " + input);
      if (candidateStemTransitions.size() == 0) {
        l.add("No Stem Candidates. Analysis Failed.");
      }
      l.add("Stem Candidate Transitions: ");
      for (StemTransition c : candidateStemTransitions) {
        l.add("  " + c.debugForm());
      }
      l.add("All paths:");
      for (SearchPath path : paths) {
        if (failedPaths.containsKey(path)) {
          l.add(format("  %s Fail → %s", path, failedPaths.get(path)));
        } else if (finishedPaths.contains(path)) {
          l.add(format("  %s Accepted", path));
        } else {
          l.add(format("  %s", path));
        }
        if (rejectedTransitions.containsKey(path)) {
          l.add("    Failed Transitions:");
          for (RejectedTransition r : rejectedTransitions.get(path)) {
            l.add("    " + r);
          }
        }
      }
      l.add("Paths    [" + input + "] (Surface + Morpheme State):");
      for (SearchPath result : resultPaths) {
        l.add("  " + result.toString());
      }
      l.add("Analyses [" + input + "] (Surface + Morpheme):");
      for (_SingleAnalysis result : results) {
        l.add("  " + AnalysisFormatters.shortForm().format(result));
      }
      return l;
    }

    public void dumpToConsole() {
      List<String> l = detailedInfo();
      l.forEach(System.out::println);
    }

    public void dumpToFile(Path path) throws IOException {
      Files.write(path, detailedInfo(), StandardCharsets.UTF_8);
    }
  }

}
