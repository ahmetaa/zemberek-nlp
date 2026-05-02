package zemberek.morphology.morphotactics;

abstract class AbstractCondition implements Condition {

  @Override
  public Condition and(Condition other) {
    return Conditions.and(this, other);
  }

  @Override
  public Condition andNot(Condition other) {
    return and(other.not());
  }

  @Override
  public Condition or(Condition other) {
    return Conditions.or(this, other);
  }

  public Condition orNot(Condition other) {
    return or(other.not());
  }

  @Override
  public Condition not() {
    return new Conditions.NotCondition(this);
  }

}
