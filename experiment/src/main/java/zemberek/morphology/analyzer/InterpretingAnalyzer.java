package zemberek.morphology.analyzer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateToken;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.TemplateTokenType;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.MorphemeTransition;
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

  // TODO: this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems = ArrayListMultimap.create(1000, 2);
  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();

  public InterpretingAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    morphotactics = new TurkishMorphotactics(lexicon);
    generator = new StemTransitionGenerator(morphotactics);
    generateStemTransitions();
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

  public List<AnalysisResult> analyze(String input) {
    // get stem candidates.
    List<StemTransition> candidates = Lists.newArrayListWithCapacity(3);
    for (int i = 1; i <= input.length(); i++) {
      String stem = input.substring(0, i);
      candidates.addAll(getMatchingStemTransitions(stem));
    }

    List<SearchPath> paths = new ArrayList<>();

    for (StemTransition candidate : candidates) {
      int length = candidate.surface.length();
      String head = input.substring(0, length);
      String tail = input.substring(length);
      paths.add(new SearchPath(candidate, head, tail));
    }

    List<AnalysisResult> results = new ArrayList<>(3);
    search(paths, results);
    return results;
  }


  private void search(List<SearchPath> current, List<AnalysisResult> completed) {

    List<SearchPath> allNewPaths = Lists.newArrayList();
    for (SearchPath path : current) {
      if (path.tail.length() == 0 && path.terminal) {
        AnalysisResult analysis = new AnalysisResult(
            path.stemTransition.item,
            path.stemTransition.surface,
            path.history);
        completed.add(analysis);
        continue;
      }
      List<SearchPath> newPaths = advance(path);
      allNewPaths.addAll(newPaths);
    }

    if (!allNewPaths.isEmpty()) {
      search(allNewPaths, completed);
    }
  }

  private List<SearchPath> advance(SearchPath path) {

    // for all transitions generate new Paths for matching transitions.
    List<SearchPath> newPaths = new ArrayList<>(2);

    // for all outgoing transitions.

    for (MorphemeTransition transition : path.currentState.getOutgoing()) {

      SuffixTransition suffixTransition = (SuffixTransition) transition;

      List<SuffixTemplateToken> tokenList = suffixTransition.getTokenList();

      // check rules.
      if (!suffixTransition.canPass(path)) {
        continue;
      }

      // epsilon transition. Add and continue. Use existing attributes.
      if (tokenList.size() == 0) {
        newPaths.add(path.getCopy(
            new MorphemeSurfaceForm("", suffixTransition),
            path.phoneticAttributes,
            path.phoneticExpectations));
        continue;
      }

      // if tail is empty, since this transitions surface is not empty now, no need to check further.
      if (path.tail.isEmpty()) {
        continue;
      }

      // TODO: early return is possible
      TurkishLetterSequence seq = MorphemeSurfaceForm.generate(tokenList, path.phoneticAttributes);

      String surface = seq.toString();

      // no need to go further if generated surface for is not a prefix of tail.
      if (!path.tail.startsWith(surface)) {
        continue;
      }

      //TODO: if tail is equal to surface, no need to calculate attributes.

      MorphemeSurfaceForm surfaceTransition = new MorphemeSurfaceForm(surface, suffixTransition);
      SuffixTemplateToken lastToken = tokenList.get(tokenList.size() - 1);
      EnumSet<PhoneticExpectation> phoneticExpectations = EnumSet.noneOf(PhoneticExpectation.class);
      if (lastToken.type == TemplateTokenType.LAST_VOICED) {
        phoneticExpectations = EnumSet.of(PhoneticExpectation.ConsonantStart);
      } else if (lastToken.type == TemplateTokenType.LAST_NOT_VOICED) {
        phoneticExpectations = EnumSet.of(PhoneticExpectation.VowelStart);
      }
      SearchPath p = path.getCopy(
          surfaceTransition,
          MorphemeSurfaceForm.defineMorphemicAttributes(seq, path.phoneticAttributes),
          phoneticExpectations);
      newPaths.add(p);
    }
    return newPaths;
  }

}
