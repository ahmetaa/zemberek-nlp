package zemberek.morphology.generator;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.morphology.analysis.AnalysisDebugData;
import zemberek.morphology.analysis.AnalysisDebugData.RejectedTransition;
import zemberek.morphology.analysis.AttributesHelper;
import zemberek.morphology.analysis.SearchPath;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.StemTransitions;
import zemberek.morphology.analysis.SurfaceTransition;
import zemberek.morphology.analysis.SurfaceTransition.SuffixTemplateToken;
import zemberek.morphology.analysis.SurfaceTransition.TemplateTokenType;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.CombinedCondition;
import zemberek.morphology.morphotactics.Condition;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * This class is used for generating words from lexical information. For example, a word can be
 * constructed with only stem and morpheme id's
 * <p>
 * Generation algorithm automatically searches through empty morphemes if the are not provided by
 * the user. For example, for
 * <pre>
 * Stem: elma
 * Morphemes:[Dat]
 * </pre>
 * word "elmaya" is generated with elma:Noun+A3sg+ya:Dat analysis.
 * <p>
 * This class is not thread-safe if instantiated with forDebug() factory constructor method.
 */
public class WordGenerator {

  private StemTransitions stemTransitions;
  private TurkishMorphotactics morphotactics;
  private boolean debugMode = false;
  private AnalysisDebugData debugData;

  public WordGenerator(TurkishMorphotactics morphotactics) {
    this.morphotactics = morphotactics;
    this.stemTransitions = morphotactics.getStemTransitions();
  }

  /**
   * Method returns a WordGenerator instance. But when this factory constructor is used, an
   * AnalysisDebugData object is generated after each call to generation methods. That object cen be
   * retrieved with getDebugData method.
   */
  public static WordGenerator forDebug(TurkishMorphotactics morphotactics) {
    WordGenerator generator = new WordGenerator(morphotactics);
    generator.debugMode = true;
    return generator;
  }

  public AnalysisDebugData getDebugData() {
    return debugData;
  }

  public List<Result> generate(String stem, List<String> morphemeIds) {
    List<Morpheme> morphemes = new ArrayList<>();
    for (String morphemeId : morphemeIds) {
      Morpheme morpheme = TurkishMorphotactics.getMorpheme(morphemeId);
      morphemes.add(morpheme);
    }
    List<StemTransition> candidates = stemTransitions.getPrefixMatches(stem, false);
    return generate(stem, candidates, morphemes);
  }

  public List<Result> generate(DictionaryItem item, List<Morpheme> morphemes) {
    List<StemTransition> candidates = stemTransitions.getTransitions(item);
    return generate(item.id, candidates, morphemes);
  }

  public List<Result> generate(DictionaryItem item, Morpheme... morphemes) {
    List<StemTransition> candidates = stemTransitions.getTransitions(item);
    return generate(item.id, candidates, Arrays.asList(morphemes));
  }

  public List<Result> generate(DictionaryItem item, String... morphemeIds) {
    List<StemTransition> candidates = stemTransitions.getTransitions(item);
    return generate(item.id, candidates, morphotactics.getMorphemes(morphemeIds));
  }

  private List<Result> generate(String input, List<StemTransition> candidates,
      List<Morpheme> morphemes) {
    // get stem candidates.

    if (debugMode) {
      debugData = new AnalysisDebugData();
      debugData.input = input;
      debugData.candidateStemTransitions.addAll(candidates);
    }

    // generate initial search paths.
    List<GenerationPath> paths = new ArrayList<>();
    for (StemTransition candidate : candidates) {
      // we set the tail as " " because in morphotactics, some conditions look for tail's size
      // during graph walk. Because this is generation we let that condition pass always.
      SearchPath searchPath = SearchPath.initialPath(candidate, " ");
      List<Morpheme> morphemesInPath;
      // if input morpheme starts with a POS Morpheme such as Noun etc,
      // we skip it if it matches with the initial morpheme of the graph visiting SearchPath object.
      if (morphemes.size() > 0) {
        if (morphemes.get(0).equals(searchPath.getCurrentState().morpheme)) {
          morphemesInPath = morphemes.subList(1, morphemes.size());
        } else {
          morphemesInPath = new ArrayList<>(morphemes);
        }
      } else {
        morphemesInPath = new ArrayList<>(0);
      }

      paths.add(new GenerationPath(searchPath, morphemesInPath));
    }

    // search graph.
    List<GenerationPath> resultPaths = search(paths);
    // generate results from successful paths.
    List<Result> result = new ArrayList<>(resultPaths.size());
    for (GenerationPath path : resultPaths) {
      SingleAnalysis analysis = SingleAnalysis.fromSearchPath(path.path);
      result.add(new Result(analysis.surfaceForm(), analysis));
      if (debugMode) {
        debugData.results.add(analysis);
      }
    }
    return result;
  }

  // searches through morphotactics graph.
  private List<GenerationPath> search(List<GenerationPath> currentPaths) {

    List<GenerationPath> result = new ArrayList<>(3);
    // new Paths are generated with matching transitions.
    while (currentPaths.size() > 0) {

      List<GenerationPath> allNewPaths = Lists.newArrayList();

      for (GenerationPath path : currentPaths) {

        // if there are no more letters to consume and path can be terminated, we accept this
        // path as a correct result.
        if (path.morphemes.size() == 0) {
          if (path.path.isTerminal() &&
              !path.path.getPhoneticAttributes().contains(PhoneticAttribute.CannotTerminate)) {
            result.add(path);
            if (debugMode) {
              debugData.finishedPaths.add(path.path);
            }
            continue;
          }
          if (debugMode) {
            debugData.failedPaths.put(path.path, "Finished but Path not terminal");
          }
        }

        // Creates new paths with outgoing and matching transitions.
        List<GenerationPath> newPaths = advance(path);
        allNewPaths.addAll(newPaths);

        if (debugMode) {
          if (newPaths.isEmpty()) {
            debugData.failedPaths.put(path.path, "No Transition");
          }
          debugData.paths.addAll(
              newPaths.stream().map(s -> s.path).collect(Collectors.toList()));
        }
      }
      currentPaths = allNewPaths;
    }

    if (debugMode) {
      debugData.resultPaths.addAll(
          result.stream().map(s -> s.path).collect(Collectors.toList()));
    }

    return result;
  }

  // for all allowed matching outgoing transitions, new paths are generated.
  // Transition conditions are used for checking if a search path is allowed to pass a transition.
  private List<GenerationPath> advance(GenerationPath gPath) {

    List<GenerationPath> newPaths = new ArrayList<>(2);

    // for all outgoing transitions.
    for (MorphemeTransition transition : gPath.path.getCurrentState().getOutgoing()) {

      SuffixTransition suffixTransition = (SuffixTransition) transition;

      // if there are no morphemes and this transitions surface is not empty, no need to check.
      if (gPath.morphemes.isEmpty() && suffixTransition.hasSurfaceForm()) {
        if (debugMode) {
          debugData.rejectedTransitions.put(
              gPath.path,
              new RejectedTransition(suffixTransition, "Empty surface expected."));
        }
        continue;
      }

      // check morpheme match.
      // if transition surface is empty, here will pass.
      if (!gPath.matches(suffixTransition)) {
        if (debugMode) {
          debugData.rejectedTransitions.put(
              gPath.path,
              new RejectedTransition(suffixTransition,
                  "Morpheme mismatch." + suffixTransition.to.morpheme));
        }
        continue;
      }

      // if transition condition fails, add it to debug data.
      if (debugMode && suffixTransition.getCondition() != null) {
        Condition condition = suffixTransition.getCondition();
        Condition failed;
        if (condition instanceof CombinedCondition) {
          failed = ((CombinedCondition) condition).getFailingCondition(gPath.path);
        } else {
          failed = condition.accept(gPath.path) ? null : condition;
        }
        if (failed != null) {
          debugData.rejectedTransitions.put(
              gPath.path,
              new RejectedTransition(suffixTransition, "Condition → " + failed.toString()));
        }
      }

      // check conditions.
      if (!suffixTransition.canPass(gPath.path)) {
        continue;
      }

      // epsilon transition. Add and continue. Use existing attributes.
      if (!suffixTransition.hasSurfaceForm()) {
        SearchPath pCopy = gPath.path.getCopyForGeneration(
            new SurfaceTransition("", suffixTransition),
            gPath.path.getPhoneticAttributes());
        newPaths.add(gPath.copy(pCopy));
        continue;
      }

      String surface = SurfaceTransition.generateSurface(
          suffixTransition,
          gPath.path.getPhoneticAttributes());

      SurfaceTransition surfaceTransition = new SurfaceTransition(surface, suffixTransition);

      //if tail is equal to surface, no need to calculate phonetic attributes.
      AttributeSet<PhoneticAttribute> attributes =
          AttributesHelper.getMorphemicAttributes(surface, gPath.path.getPhoneticAttributes());

      // This is required for suffixes like `cik` and `ciğ`
      // an extra attribute is added if "cik" or "ciğ" is generated and matches the tail.
      // if "cik" is generated, ExpectsConsonant attribute is added, so only a consonant starting
      // suffix can follow. Likewise, if "ciğ" is produced, a vowel starting suffix is allowed.
      attributes.remove(PhoneticAttribute.CannotTerminate);
      SuffixTemplateToken lastToken = suffixTransition.getLastTemplateToken();
      if (lastToken.getType() == TemplateTokenType.LAST_VOICED) {
        attributes.add(PhoneticAttribute.ExpectsConsonant);
      } else if (lastToken.getType() == TemplateTokenType.LAST_NOT_VOICED) {
        attributes.add(PhoneticAttribute.ExpectsVowel);
        attributes.add(PhoneticAttribute.CannotTerminate);
      }

      SearchPath p = gPath.path.getCopyForGeneration(
          surfaceTransition,
          attributes);
      newPaths.add(gPath.copy(p));
    }
    return newPaths;
  }

  public static class Result {

    public final String surface;
    public final SingleAnalysis analysis;

    public Result(String surface, SingleAnalysis analysis) {
      this.surface = surface;
      this.analysis = analysis;
    }

    @Override
    public String toString() {
      return surface + "-" + analysis;
    }
  }

  static class GenerationPath {

    SearchPath path;
    List<Morpheme> morphemes;

    public GenerationPath(SearchPath path,
        List<Morpheme> morphemes) {
      this.path = path;
      this.morphemes = morphemes;
    }

    public GenerationPath copy(SearchPath path) {
      SurfaceTransition lastTransition = path.getLastTransition();
      Morpheme m = lastTransition.getMorpheme();

      if (lastTransition.surface.isEmpty()) {
        if (morphemes.size() == 0) {
          return new GenerationPath(path, morphemes);
        }
        if (m.equals(morphemes.get(0))) {
          return new GenerationPath(path, morphemes.subList(1, morphemes.size()));
        } else {
          return new GenerationPath(path, morphemes);
        }
      }
      if (!m.equals(morphemes.get(0))) {
        throw new IllegalStateException(
            "Cannot generate Generation copy because transition morpheme and first morpheme to consume"
                + " does not match.");
      }
      return new GenerationPath(path, morphemes.subList(1, morphemes.size()));

    }

    boolean matches(SuffixTransition transition) {
      if (!transition.hasSurfaceForm()) {
        return true;
      }
      if (morphemes.size() > 0 && transition.to.morpheme.equals(morphemes.get(0))) {
        return true;
      }
      return false;
    }

  }

}
