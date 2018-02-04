package zemberek.morphology.analyzer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateToken;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.TemplateTokenType;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.GraphVisitor;
import zemberek.morphology.morphotactics.MorphemeState;
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

  TurkishMorphotactics morphotactics = new TurkishMorphotactics();

  // TODO: this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems = ArrayListMultimap.create(1000, 2);
  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();

  public InterpretingAnalyzer(RootLexicon lexicon) {
    this.lexicon = lexicon;
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


  List<AnalysisResult> analyze(String input) {
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

    return null;
  }


  private void traverseSuffixes(List<SearchPath> current, List<WordAnalysis> completed) {

    List<SearchPath> newTokens = Lists.newArrayList();
    for (SearchPath path : current) {
      List<MorphemeTransition> transitions = path.currentState.getOutgoing();


    }

    if (!newTokens.isEmpty()) {
      traverseSuffixes(newTokens, completed);
    }
  }

  static class SearchPath implements GraphVisitor {

    // letters that have been parsed.
    String head;

    // letters to parse.
    String tail;

    // carries the initial transition. Normally this is not necessary bur here we have it for
    // a small optimization.
    StemTransition stemTransition;

    MorphemeState currentState;

    List<MorphemeSurfaceForm> history = new ArrayList<>();

    EnumSet<PhoneticAttribute> phoneticAttributes;
    EnumSet<PhoneticExpectation> phoneticExpectations;

    boolean terminal = false;

    public SearchPath(StemTransition stemTransition, String head, String tail) {
      this.stemTransition = stemTransition;
      this.terminal = stemTransition.to.terminal;
      this.currentState = stemTransition.to;
      this.phoneticAttributes = stemTransition.getPhoneticAttributes().clone();
      this.phoneticExpectations = stemTransition.getPhoneticExpectations().clone();
      this.head = head;
      this.tail = tail;
    }

    public SearchPath(String head, String tail,
        StemTransition stemTransition, MorphemeState currentState,
        List<MorphemeSurfaceForm> history,
        EnumSet<PhoneticAttribute> phoneticAttributes,
        EnumSet<PhoneticExpectation> phoneticExpectations, boolean terminal) {
      this.head = head;
      this.tail = tail;
      this.stemTransition = stemTransition;
      this.currentState = currentState;
      this.history = history;
      this.phoneticAttributes = phoneticAttributes;
      this.phoneticExpectations = phoneticExpectations;
      this.terminal = terminal;
    }

    SearchPath getCopy(MorphemeSurfaceForm surfaceNode,
        EnumSet<PhoneticAttribute> phoneticAttributes,
        EnumSet<PhoneticExpectation> phoneticExpectations
    ) {
      boolean t = surfaceNode.lexicalForm.to.terminal;
      ArrayList<MorphemeSurfaceForm> hist = new ArrayList<>(history);
      hist.add(surfaceNode);
      String newHead = head + surfaceNode.surface;
      String newTail = tail.substring(surfaceNode.surface.length());
      return new SearchPath(newHead, newTail, stemTransition, surfaceNode.lexicalForm.to,
          hist, phoneticAttributes, phoneticExpectations, t);
    }

    @Override
    public boolean containsKey(String key) {
      return false;
    }

    @Override
    public boolean containsTailSequence(List<String> keys) {
      return false;
    }
  }

  private List<SearchPath> advance(SearchPath path) {
    // for all transitions generate new Paths for matching transitions.
    List<SearchPath> newPaths = new ArrayList<>(2);
    for (MorphemeTransition transition : path.currentState.getOutgoing()) {
      // TODO: this is very slow, notmally it is possible to see if a new path object
      // is necessary to create by checking the tail and surface.
      SuffixTransition suffixTransition = (SuffixTransition) transition;

      List<SuffixTemplateToken> tokenList = suffixTransition.getTokenList();

      // epsilon transition. Add and continue.
      if (tokenList.size() == 0) {
        newPaths.add(path.getCopy(
            new MorphemeSurfaceForm("", suffixTransition),
            path.phoneticAttributes,
            path.phoneticExpectations));
        continue;
      }

      // if tail is empty and this transition is not empty, no need to check further.
      if (path.tail.isEmpty()) {
        continue;
      }

      TurkishLetterSequence seq = MorphemeSurfaceForm.generate(tokenList, path.phoneticAttributes);

      String surface = seq.toString();

      // no need to go further if generated surface for is not a prefix of tail.
      if (!path.tail.startsWith(surface)) {
        continue;
      }

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


  public static void main(String[] args) {
    RootLexicon loader = new TurkishDictionaryLoader().load("elma");
    InterpretingAnalyzer analyzer = new InterpretingAnalyzer(loader);
    analyzer.analyze("elmalar");
  }


}
