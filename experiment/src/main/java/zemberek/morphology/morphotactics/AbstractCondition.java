package zemberek.morphology.morphotactics;

import zemberek.morphology.analyzer.SearchPath;

abstract class AbstractCondition implements Condition {

  public abstract boolean check(SearchPath path);

  @Override
  public Condition and(Condition other) {
    return DSL.and(this, other);
  }

  @Override
  public Condition andNot(Condition other) {
    return and(other.not());
  }

  @Override
  public Condition or(Condition other) {
    return DSL.or(this, other);
  }

  @Override
  public Condition orNot(Condition other) {
    return or(other.not());
  }

  @Override
  public Condition not() {
    return new Conditions.NotCondition(this);
  }

}
