package zemberek.morphology.analyzer;

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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateToken;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.TemplateTokenType;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.Rule;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * This is a primitive analyzer.
 */
public class InterpretingAnalyzer {

  RootLexicon lexicon;

  StemTransitionGenerator generator;

  TurkishMorphotactics morphotactics;

  // TODO: Move this to somewhere else. Also this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems =
      ArrayListMultimap.create(1000, 2);
  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();

  public InterpretingAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    morphotactics = new TurkishMorphotactics(lexicon);
    generator = new StemTransitionGenerator(morphotactics);
    generateStemTransitions();
  }


  public List<AnalysisResult> analyze(String input, AnalysisDebugData debugData) {

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
    return search(paths, debugData);
  }

  public List<AnalysisResult> analyze(String input) {
    return analyze(input, null);
  }


  // searches through morphotactics graph recursively.
  private List<AnalysisResult> search(List<SearchPath> current, AnalysisDebugData debugData) {

    List<AnalysisResult> result = new ArrayList<>(3);
    // new Paths are generated with matching transitions.
    while (current.size() > 0) {

      List<SearchPath> allNewPaths = Lists.newArrayList();

      for (SearchPath path : current) {

        // if there is no more letters to consume and path can be terminated, we accept this
        // path as a correct result.
        if (path.tail.length() == 0) {
          if (path.isTerminal()) {
            AnalysisResult analysis = new AnalysisResult(
                path.stemTransition.item,
                path.stemTransition.surface,
                path.suffixes);
            result.add(analysis);
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
      current = allNewPaths;
    }

    if (debugData != null) {
      debugData.results.addAll(result);
    }

    return result;
  }

  // for all allowed outgoing transitions generates new Paths.
  // Rules are used for checking if a transition is allowed.
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
              new RejectedTransition("Empty surface expected.", suffixTransition));
        }
        continue;
      }

      if (debugData != null) {
        for (Rule rule : suffixTransition.getRules()) {
          if (!rule.canPass(path)) {
            debugData.rejectedTransitions.put(
                path,
                new RejectedTransition("Rule → " + rule.toString(), suffixTransition));
            break;
          }
        }
      }

      // check rules.
      if (!suffixTransition.canPass(path)) {
        continue;
      }

      // epsilon transition. Add and continue. Use existing attributes.
      if (!suffixTransition.hasSurfaceForm()) {
        newPaths.add(path.getCopy(
            new MorphemeSurfaceForm("", suffixTransition),
            path.phoneticAttributes));
        continue;
      }

      // TODO: early return is possible
      TurkishLetterSequence seq = MorphemeSurfaceForm.generate(
          suffixTransition,
          path.phoneticAttributes);

      String surface = seq.toString();

      // no need to go further if generated surface for is not a prefix of tail.
      if (!path.tail.startsWith(surface)) {
        if (debugData != null) {
          debugData.rejectedTransitions.put(
              path,
              new RejectedTransition("Surface Mismatch:" + surface, suffixTransition));
        }
        continue;
      }

      //TODO: if tail is equal to surface, no need to calculate attributes.

      MorphemeSurfaceForm surfaceTransition = new MorphemeSurfaceForm(surface, suffixTransition);
      SuffixTemplateToken lastToken = suffixTransition.getLastTemplateToken();

      EnumSet<PhoneticAttribute> attributes = MorphemeSurfaceForm
          .defineMorphemicAttributes(seq, path.phoneticAttributes);

      if (lastToken.type == TemplateTokenType.LAST_VOICED) {
        attributes.add(PhoneticAttribute.ExpectsConsonant);
      } else if (lastToken.type == TemplateTokenType.LAST_NOT_VOICED) {
        attributes.add(PhoneticAttribute.ExpectsVowel);
      }

      SearchPath p = path.getCopy(
          surfaceTransition,
          attributes);
      newPaths.add(p);
    }
    return newPaths;
  }

  private void generateStemTransitions() {
    for (DictionaryItem item : lexicon) {
      List<StemTransition> transitions = generator.generate(item);
      for (StemTransition transition : transitions) {
        addStemTransition(transition);
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

    String reason;
    SuffixTransition transition;

    public RejectedTransition(String reason,
        SuffixTransition transition) {
      this.reason = reason;
      this.transition = transition;
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
    List<AnalysisResult> results = new ArrayList<>();

    public List<String> detailedInfo() {
      List<String> l = new ArrayList<>();
      l.add("Input = " + input);
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
      l.add("Results:");
      for (AnalysisResult result : results) {
        l.add("  " + result);
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
