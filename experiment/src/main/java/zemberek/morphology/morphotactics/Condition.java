package zemberek.morphology.morphotactics;

import zemberek.morphology.analyzer.SearchPath;

public interface Condition {

  boolean check(SearchPath path);

  Condition and(Condition other);

  Condition andNot(Condition other);

  Condition or(Condition other);

  Condition orNot(Condition other);

  Condition not();

}
