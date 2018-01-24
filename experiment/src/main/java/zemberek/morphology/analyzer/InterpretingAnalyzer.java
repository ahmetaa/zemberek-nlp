package zemberek.morphology.analyzer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.GraphVisitor;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * This is a primitive analyzer.
 */
public class InterpretingAnalyzer {

  RootLexicon lexicon;

  GraphVisitor graphVisitor;

  StemTransitionGenerator generator;

  TurkishMorphotactics morphotactics = new TurkishMorphotactics();

  // TODO: this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems = ArrayListMultimap.create(1000, 2);
  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();

  public InterpretingAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
    generator = new StemTransitionGenerator(morphotactics);
    graphVisitor = new SimpleGraphVisitor();
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


  List<AnalysisResult> analyze(String input) {
    // get stem candidates.
    List<StemTransition> candidates = Lists.newArrayListWithCapacity(3);
    for (int i = 1; i <= input.length(); i++) {
      String stem = input.substring(0, i);
      candidates.addAll(getMatchingStemTransitions(stem));
    }

    return null;
  }

  static class SimpleGraphVisitor implements GraphVisitor {

    @Override
    public boolean containsKey(String key) {
      return false;
    }

    @Override
    public boolean containsTailSequence(List<String> keys) {
      return false;
    }
  }


  static class Token {

    // carries the initial transition.
    StemTransition stemTransition;

    SuffixTransition currentSuffixTransition;

    List<SuffixTransition> history;

    // remaining letters to parse.
    String rest;

  }


}
