package zemberek.morphology.morphotactics;

import zemberek.morphology.analyzer.SearchPath;

public interface Condition {
  boolean check(SearchPath path);
}
