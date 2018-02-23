package zemberek.morphology.analysis;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Assert;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;
import zemberek.morphology.analyzer.InterpretingAnalyzer.AnalysisDebugData;
import zemberek.morphology.analyzer.MorphemeSurfaceForm;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class AnalyzerTestBase {


  static InterpretingAnalyzer getAnalyzer(String... dictionaryLines) {
    RootLexicon loader = new TurkishDictionaryLoader().load(dictionaryLines);
    return new InterpretingAnalyzer(loader);
  }

  static AnalysisTester getTester(String... dictionaryLines) {
    RootLexicon loader = new TurkishDictionaryLoader().load(dictionaryLines);
    return new AnalysisTester(new InterpretingAnalyzer(loader));
  }

  boolean containsMorpheme(AnalysisResult result, String morphemeName) {
    for (MorphemeSurfaceForm forms : result.getMorphemes()) {
      if (forms.lexicalTransition.to.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return true;
      }
    }
    return false;
  }

  boolean lastMorphemeIs(AnalysisResult result, String morphemeName) {
    List<MorphemeSurfaceForm> morphemes = result.getMorphemes();
    if (morphemes.size() == 0) {
      return false;
    }
    MorphemeSurfaceForm last = morphemes.get(morphemes.size() - 1);
    return last.lexicalTransition.to.morpheme.id.equalsIgnoreCase(morphemeName);
  }

  public boolean notContains(AnalysisResult result, String morphemeName) {
    for (MorphemeSurfaceForm forms : result.getMorphemes()) {
      if (forms.lexicalTransition.to.morpheme.id.equalsIgnoreCase(morphemeName)) {
        return false;
      }
    }
    return true;
  }

  static void printAndSort(String input, List<AnalysisResult> results) {
    results.sort(Comparator.comparing(AnalysisResult::toString));
    for (AnalysisResult result : results) {
      System.out.println(input + " = " + result + " = " + result.shortForm());
    }
  }

  static void expectFail(InterpretingAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<AnalysisResult> results = analyzer.analyze(word);
      if (results.size() != 0) {
        printDebug(analyzer, word);
        Assert.fail(word + " is expected to fail but passed.");
      }
    }
  }

  static void expectSuccess(InterpretingAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<AnalysisResult> results = analyzer.analyze(word);
      if (results.size() == 0) {
        printDebug(analyzer, word);
        Assert.fail(word + " is expected to pass but failed.");
      } else {
        printAndSort(word, results);
      }
    }
  }

  static void expectSuccess(InterpretingAnalyzer analyzer, int solutionCount, String... words) {
    for (String word : words) {
      List<AnalysisResult> results = analyzer.analyze(word);
      if (results.size() != solutionCount) {
        printDebug(analyzer, word);
        Assert.fail(word + " is expected to pass with solution count " + solutionCount +
            " but failed with solution count " + results.size());
      } else {
        printAndSort(word, results);
      }
    }
  }

  static AnalysisResult getSingleAnalysis(InterpretingAnalyzer analyzer, String input) {
    List<AnalysisResult> results = analyzer.analyze(input);
    if (results.size() != 1) {
      printDebug(analyzer, input);
      if (results.size() == 0) {
        Assert.fail(input + " cannot be analyzed");
      } else {
        Assert.fail(input + " is expected to have single solution but " +
            " it has " + results.size() + " solutions");
      }
    }
    printAndSort(input, results);
    return results.get(0);
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

    void expectTrue(String input, Predicate<AnalysisResult> predicate) {
      AnalysisResult result = getSingleAnalysis(analyzer, input);
      if (!predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]");
      }
    }

    void expectFalse(String input, Predicate<AnalysisResult> predicate) {
      AnalysisResult result = getSingleAnalysis(analyzer, input);
      if (predicate.test(result)) {
        printDebug(analyzer, input);
        Assert.fail("Anaysis Failed for [" + input + "]");
      }
    }
  }

  public static Predicate<AnalysisResult> matchesShortForm(String shortForm) {
    return p -> p.shortForm().equalsIgnoreCase(shortForm);
  }

  public static Predicate<AnalysisResult> matchesShortFormTail(String shortFormTail) {
    return p -> p.shortForm().endsWith(shortFormTail);
  }

  public static Predicate<AnalysisResult> matchesLexicalTail(String tail) {
    return p -> p.lexicalForm().endsWith(tail);
  }


}
