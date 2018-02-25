package zemberek.morphology.morphotactics;

public abstract class MorphemeTransition {

  public MorphemeState from;
  public MorphemeState to;

  Condition condition;

  public MorphemeState from() {
    return from;
  }

  public MorphemeState to() {
    return to;
  }

  public Condition getCondition() {
    return condition;
  }
}
