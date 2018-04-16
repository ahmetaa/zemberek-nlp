package zemberek.morphology.morphotactics;

import zemberek.morphology.analysis.SearchPath;

public interface Condition {

  boolean accept(SearchPath path);

  Condition and(Condition other);

  Condition andNot(Condition other);

  Condition or(Condition other);

  Condition orNot(Condition other);

  Condition not();

}
