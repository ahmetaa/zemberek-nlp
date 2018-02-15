package zemberek.morphology.analysis;

import java.util.Comparator;
import java.util.List;
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

  void printAndSort(String input, List<AnalysisResult> results) {
    results.sort(Comparator.comparing(AnalysisResult::toString));
    for (AnalysisResult result : results) {
      System.out.println(input + " = " + result + " = " + result.shortForm());
    }
  }

  void shouldNotPass(InterpretingAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<AnalysisResult> results = analyzer.analyze(word);
      if (results.size() != 0) {
        printAndSort(word, results);
        AnalysisDebugData debugData = new AnalysisDebugData();
        analyzer.analyze(word, debugData);
        debugData.dumpToConsole();
        Assert.fail(word + " is expected to fail but passed.");
      }
    }
  }

  void shouldPass(InterpretingAnalyzer analyzer, String... words) {
    for (String word : words) {
      List<AnalysisResult> results = analyzer.analyze(word);
      if (results.size() == 0) {
        printAndSort(word, results);
        AnalysisDebugData debugData = new AnalysisDebugData();
        analyzer.analyze(word, debugData);
        debugData.dumpToConsole();
        Assert.fail(word + " is expected to pass but failed.");
      }
    }
  }

  void shouldPass(InterpretingAnalyzer analyzer, int solutionCount, String... words) {
    for (String word : words) {
      List<AnalysisResult> results = analyzer.analyze(word);
      if (results.size() != solutionCount) {
        printAndSort(word, results);
        AnalysisDebugData debugData = new AnalysisDebugData();
        analyzer.analyze(word, debugData);
        debugData.dumpToConsole();
        Assert.fail(word + " is expected to pass with solution count " + solutionCount +
            " but failed with solution count " + results.size());
      }
    }
  }


}
