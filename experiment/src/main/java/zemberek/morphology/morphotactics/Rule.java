package zemberek.morphology.morphotactics;

import zemberek.morphology.analyzer.SearchPath;

public interface Rule {

  // rule is accepted.
  boolean canPass(SearchPath visitor);
}
