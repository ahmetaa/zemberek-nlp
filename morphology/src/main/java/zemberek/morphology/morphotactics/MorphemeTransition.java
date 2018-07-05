package zemberek.morphology.morphotactics;

/**
 * Represents a transition in morphotactics graph.
 */
public abstract class MorphemeTransition {

  public MorphemeState from;
  public MorphemeState to;

  // Defines the condition(s) to allow or block a graph visitor (SearchPath).
  // A condition can be a single or a group of objects that has Condition interface.
  // For example, if condition is HasPhoneticAttribute(LastLetterVowel), and SearchPath's last
  // letter is a consonant, it cannot pass this transition.
  Condition condition;

  int conditionCount;

  public MorphemeState from() {
    return from;
  }

  public MorphemeState to() {
    return to;
  }

  public Condition getCondition() {
    return condition;
  }

  public int getConditionCount() {
    return conditionCount;
  }

  abstract MorphemeTransition getCopy();
}
