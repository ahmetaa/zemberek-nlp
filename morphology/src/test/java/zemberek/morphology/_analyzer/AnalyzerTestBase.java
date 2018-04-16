package zemberek.morphology._analyzer;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Assert;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeSurface;
import zemberek.morphology._morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class AnalyzerTestBase {

  static TurkishMorphotactics getMorphotactics(String... dictionaryLines) {
    RootLexicon lexicon = new TurkishDictionaryLoader().load(dictionaryLines);
    return new TurkishMorphotactics(lexicon);
  }

  static InterpretingAnalyzer getAnalyzer(String... dictionaryLines) {
    return new InterpretingAnalyzer(getMorphotactics(dictionaryLines));
  }

  static AnalysisTester getTester(String... dictionaryLines) {
    return new AnalysisTester(new InterpretingAnalyzer(getMorphotactics(dictionaryLines)));
  }

  boolean containsMorpheme(_SingleAnalysis result, String morphemeName) {
    for (MorphemeSurface forms : result.getMorphemesSurfaces()) {
      if (forms.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return true;
      }
    }
    return false;
  }

  boolean lastMorphemeIs(_SingleAnalysis result, String morphemeName) {
    List<MorphemeSurface> morphemes = result.getMorphemesSurfaces();
    if (morphemes.size() == 0) {
      return false;
    }
    MorphemeSurface last = morphemes.get(morphemes.size() - 1);
    return last.morpheme.id.equalsIgnoreCase(morphemeName);
  }

  public boolean notContains(_SingleAnalysis result, String morphemeName) {
    for (_SingleAnalysis.MorphemeSurface forms : result.getMorphemesSurfaces()) {
      if (forms.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return false;
      }
    }
    return true;
  }

  static void printAndSort(String input, List<_SingleAnalysis> results) {
    results.sort(Comparator.comparing(_SingleAnalysis::toString));
    for (_SingleAnalysis result : results) {
      System.out.println(input + " = " + result + " = " + result.formatSurfaceSequence());
    }
  }

  static void expectFail(InterpretingAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<_SingleAnalysis> results = analyzer.analyze(word);
      if (results.size() != 0) {
        printDebug(analyzer, word);
        Assert.fail("[" + word + "] is expected to fail but passed.");
      }
    }
  }

  static void expectSuccess(InterpretingAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<_SingleAnalysis> results = analyzer.analyze(word);
      if (results.size() == 0) {
        printDebug(analyzer, word);
        Assert.fail("[" + word + "] is expected to pass but failed.");
      } else {
        printAndSort(word, results);
      }
    }
  }

  static void expectSuccess(InterpretingAnalyzer analyzer, int solutionCount, String... words) {
    for (String word : words) {
      List<_SingleAnalysis> results = analyzer.analyze(word);
      if (results.size() != solutionCount) {
        printDebug(analyzer, word);
        Assert.fail("[" + word + "] is expected to pass with solution count " + solutionCount +
            " but failed with solution count " + results.size());
      } else {
        printAndSort(word, results);
      }
    }
  }

  static _SingleAnalysis getSingleAnalysis(InterpretingAnalyzer analyzer, String input) {
    List<_SingleAnalysis> results = analyzer.analyze(input);
    if (results.size() != 1) {
      printDebug(analyzer, input);
      if (results.size() == 0) {
        Assert.fail("[" + input + "] cannot be analyzed");
      } else {
        Assert.fail("[" + input + "] is expected to have single solution but " +
            " it has " + results.size() + " solutions");
      }
    }
    printAndSort(input, results);
    return results.get(0);
  }

  static List<_SingleAnalysis> getMultipleAnalysis(
      InterpretingAnalyzer analyzer, int count, String input) {
    List<_SingleAnalysis> results = analyzer.analyze(input);
    if (results.size() != count) {
      printDebug(analyzer, input);
      if (results.size() == 0) {
        Assert.fail(input + " cannot be analyzed");
      } else {
        Assert.fail("[" + input + "] is expected to have single solution but " +
            " it has " + results.size() + " solutions");
      }
    }
    printAndSort(input, results);
    return results;
  }

  static List<_SingleAnalysis> getMultipleAnalysis(InterpretingAnalyzer analyzer, String input) {
    List<_SingleAnalysis> results = analyzer.analyze(input);
    if (results.size() == 0) {
      printDebug(analyzer, input);
      Assert.fail(input + " cannot be analyzed");
    }
    printAndSort(input, results);
    return results;
  }


  private static void printDebug(
      InterpretingAnalyzer analyzer,
      String input) {
    AnalysisDebugData debugData = new AnalysisDebugData();
    analyzer.analyze(input, debugData);
    debugData.dumpToConsole();
  }

  static class AnalysisTester {

    InterpretingAnalyzer analyzer;

    public AnalysisTester(InterpretingAnalyzer analyzer) {
      this.analyzer = analyzer;
    }

    void expectFail(String... words) {
      AnalyzerTestBase.expectFail(analyzer, words);
    }

    void expectSuccess(String... words) {
      AnalyzerTestBase.expectSuccess(analyzer, words);
    }

    void expectSuccess(int solutionCount, String... words) {
      AnalyzerTestBase.expectSuccess(analyzer, solutionCount, words);
    }

    void expectSingle(String input, Predicate<_SingleAnalysis> predicate) {
      _SingleAnalysis result = getSingleAnalysis(analyzer, input);
      if (!predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]");
      }
    }

    void expectSingle(String input, AnalysisMatcher matcher) {
      _SingleAnalysis result = getSingleAnalysis(analyzer, input);
      if (!matcher.predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]. Predicate Input = " + matcher.expected);
      }
    }

    void expectAny(String input, AnalysisMatcher matcher) {
      List<_SingleAnalysis> result = getMultipleAnalysis(analyzer, input);
      for (_SingleAnalysis analysisResult : result) {
        if (matcher.predicate.test(analysisResult)) {
          return;
        }
      }
      printDebug(analyzer, input);
      Assert.fail("Anaysis Failed for [" + input + "]. Predicate Input = " + matcher.expected);
    }

    void expectFalse(String input, Predicate<_SingleAnalysis> predicate) {
      _SingleAnalysis result = getSingleAnalysis(analyzer, input);
      if (predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]");
      }
    }

    void expectFalse(String input, AnalysisMatcher matcher) {
      List<_SingleAnalysis> results = getMultipleAnalysis(analyzer, input);
      for (_SingleAnalysis result : results) {
        if (matcher.predicate.test(result)) {
          printDebug(analyzer, input);
          Assert.fail("Anaysis Failed for [" + input + "]");
        }
      }
    }
  }

  public static Predicate<_SingleAnalysis> matchesShortForm(String shortForm) {
    return p -> p.formatSurfaceSequence().equalsIgnoreCase(shortForm);
  }

  public static Predicate<_SingleAnalysis> matchesShortFormTail(String shortFormTail) {
    return p -> p.formatSurfaceSequence().endsWith(shortFormTail);
  }

  public static AnalysisMatcher matchesTailLex(String tail) {
    return new AnalysisMatcher(p -> p.formatLexicalSequence().endsWith(tail), tail);
  }

  static class AnalysisMatcher {

    String expected;
    Predicate<_SingleAnalysis> predicate;

    AnalysisMatcher(Predicate<_SingleAnalysis> predicate, String expected) {
      this.predicate = predicate;
      this.expected = expected;
    }

  }
}
