package zemberek.morphology.analysis;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Assert;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class AnalyzerTestBase {

  public static final boolean PRINT_RESULTS_TO_SCREEN = false;

  public static TurkishMorphotactics getMorphotactics(String... dictionaryLines) {
    RootLexicon lexicon = TurkishDictionaryLoader.load(dictionaryLines);
    return new TurkishMorphotactics(lexicon);
  }

  static RuleBasedAnalyzer getAnalyzer(String... dictionaryLines) {
    return RuleBasedAnalyzer.forDebug(getMorphotactics(dictionaryLines));
  }

  static RuleBasedAnalyzer getAnalyzer(TurkishMorphotactics morphotactics) {
    return RuleBasedAnalyzer.forDebug(morphotactics);
  }

  static AnalysisTester getTester(String... dictionaryLines) {
    return new AnalysisTester(RuleBasedAnalyzer.forDebug(getMorphotactics(dictionaryLines)));
  }

  static AnalysisTester getTester(TurkishMorphotactics morphotactics) {
    return new AnalysisTester(RuleBasedAnalyzer.forDebug(morphotactics));
  }

  boolean containsMorpheme(SingleAnalysis result, String morphemeName) {
    for (MorphemeData forms : result.getMorphemeDataList()) {
      if (forms.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return true;
      }
    }
    return false;
  }

  boolean lastMorphemeIs(SingleAnalysis result, String morphemeName) {
    List<MorphemeData> morphemes = result.getMorphemeDataList();
    if (morphemes.size() == 0) {
      return false;
    }
    MorphemeData last = morphemes.get(morphemes.size() - 1);
    return last.morpheme.id.equalsIgnoreCase(morphemeName);
  }

  public boolean notContains(SingleAnalysis result, String morphemeName) {
    for (MorphemeData forms : result.getMorphemeDataList()) {
      if (forms.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return false;
      }
    }
    return true;
  }

  static void printAndSort(String input, List<SingleAnalysis> results) {
    results.sort(Comparator.comparing(SingleAnalysis::toString));
    if (!PRINT_RESULTS_TO_SCREEN) {
      return;
    }
    for (SingleAnalysis result : results) {
      System.out.println(input + " = " + result + " = " + formatSurfaceAndLexical(result));
    }
  }

  static void expectFail(RuleBasedAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<SingleAnalysis> results = analyzer.analyze(word);
      if (results.size() != 0) {
        printDebug(analyzer, word);
        Assert.fail("[" + word + "] is expected to fail but passed.");
      }
    }
  }

  static void expectSuccess(RuleBasedAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<SingleAnalysis> results = analyzer.analyze(word);
      if (results.size() == 0) {
        printDebug(analyzer, word);
        Assert.fail("[" + word + "] is expected to pass but failed.");
      } else {
        printAndSort(word, results);
      }
    }
  }

  static void expectSuccess(RuleBasedAnalyzer analyzer, int solutionCount, String... words) {
    for (String word : words) {
      List<SingleAnalysis> results = analyzer.analyze(word);
      if (results.size() != solutionCount) {
        printDebug(analyzer, word);
        Assert.fail("[" + word + "] is expected to pass with solution count " + solutionCount +
            " but failed with solution count " + results.size());
      } else {
        printAndSort(word, results);
      }
    }
  }

  static SingleAnalysis getSingleAnalysis(RuleBasedAnalyzer analyzer, String input) {
    List<SingleAnalysis> results = analyzer.analyze(input);
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

  static List<SingleAnalysis> getMultipleAnalysis(
      RuleBasedAnalyzer analyzer, int count, String input) {
    List<SingleAnalysis> results = analyzer.analyze(input);
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

  static List<SingleAnalysis> getMultipleAnalysis(RuleBasedAnalyzer analyzer, String input) {
    List<SingleAnalysis> results = analyzer.analyze(input);
    if (results.size() == 0) {
      printDebug(analyzer, input);
      Assert.fail(input + " cannot be analyzed");
    }
    printAndSort(input, results);
    return results;
  }


  private static void printDebug(
      RuleBasedAnalyzer analyzer,
      String input) {
    analyzer.analyze(input);
    AnalysisDebugData debugData = analyzer.getDebugData();
    debugData.dumpToConsole();
  }

  static class AnalysisTester {

    RuleBasedAnalyzer analyzer;

    public AnalysisTester(RuleBasedAnalyzer analyzer) {
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

    void expectSingle(String input, Predicate<SingleAnalysis> predicate) {
      SingleAnalysis result = getSingleAnalysis(analyzer, input);
      if (!predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]");
      }
    }

    void expectSingle(String input, AnalysisMatcher matcher) {
      SingleAnalysis result = getSingleAnalysis(analyzer, input);
      if (!matcher.predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]. Predicate Input = " + matcher.expected);
      }
    }

    void expectAny(String input, AnalysisMatcher matcher) {
      List<SingleAnalysis> result = getMultipleAnalysis(analyzer, input);
      for (SingleAnalysis analysisResult : result) {
        if (matcher.predicate.test(analysisResult)) {
          return;
        }
      }
      printDebug(analyzer, input);
      Assert.fail("Anaysis Failed for [" + input + "]. Predicate Input = " + matcher.expected);
    }

    void expectFalse(String input, Predicate<SingleAnalysis> predicate) {
      SingleAnalysis result = getSingleAnalysis(analyzer, input);
      if (predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]");
      }
    }

    void expectFalse(String input, AnalysisMatcher matcher) {
      List<SingleAnalysis> results = getMultipleAnalysis(analyzer, input);
      for (SingleAnalysis result : results) {
        if (matcher.predicate.test(result)) {
          printDebug(analyzer, input);
          Assert.fail("Anaysis Failed for [" + input + "]");
        }
      }
    }
  }

  public static String formatSurfaceAndLexical(SingleAnalysis analysis) {
    return AnalysisFormatters.SURFACE_AND_LEXICAL_SEQUENCE.format(analysis);
  }

  public static Predicate<SingleAnalysis> matchesShortForm(String shortForm) {
    return p -> formatSurfaceAndLexical(p).equalsIgnoreCase(shortForm);
  }

  public static Predicate<SingleAnalysis> matchesShortFormTail(String shortFormTail) {
    return p -> formatSurfaceAndLexical(p).endsWith(shortFormTail);
  }


  public static String formatLexicalSequence(SingleAnalysis s) {
    return AnalysisFormatters.LEXICAL_SEQUENCE.format(s);
  }

  public static AnalysisMatcher matchesTailLex(String tail) {
    return new AnalysisMatcher(p -> formatLexicalSequence(p).endsWith(tail), tail);
  }

  static class AnalysisMatcher {

    String expected;
    Predicate<SingleAnalysis> predicate;

    AnalysisMatcher(Predicate<SingleAnalysis> predicate, String expected) {
      this.predicate = predicate;
      this.expected = expected;
    }

  }
}
