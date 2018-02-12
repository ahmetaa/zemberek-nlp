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

public class InterpretingAnalyzerTestBase {


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
      System.out.println(input + " = " + result);
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


}
