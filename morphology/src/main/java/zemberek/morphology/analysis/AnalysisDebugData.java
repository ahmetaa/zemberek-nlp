package zemberek.morphology.analysis;

import static java.lang.String.format;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.SuffixTransition;

public class AnalysisDebugData {

  public String input;
  public List<StemTransition> candidateStemTransitions = new ArrayList<>();
  public List<SearchPath> paths = new ArrayList<>();
  public Map<SearchPath, String> failedPaths = new HashMap<>();
  public Set<SearchPath> finishedPaths = new LinkedHashSet<>();
  public Multimap<SearchPath, RejectedTransition> rejectedTransitions = ArrayListMultimap.create();
  public List<SingleAnalysis> results = new ArrayList<>();
  public List<SearchPath> resultPaths = new ArrayList<>();

  List<String> detailedInfo() {
    List<String> l = new ArrayList<>();
    l.add("----------------------");
    l.add("Debug data for input = " + input);
    if (candidateStemTransitions.size() == 0) {
      l.add("No Stem Candidates. Analysis Failed.");
    }
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
    l.add("Paths    [" + input + "] (Surface + Morpheme State):");
    for (SearchPath result : resultPaths) {
      l.add("  " + result.toString());
    }
    l.add("Analyses [" + input + "] (Surface + Morpheme):");
    for (SingleAnalysis result : results) {
      l.add("  " + AnalysisFormatters.surfaceSequenceFormatter().format(result));
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

  public static class RejectedTransition {

    SuffixTransition transition;
    String reason;

    public RejectedTransition(SuffixTransition transition, String reason) {
      this.transition = transition;
      this.reason = reason;
    }

    @Override
    public String toString() {
      return transition.toString() + " " + reason;
    }
  }

}
