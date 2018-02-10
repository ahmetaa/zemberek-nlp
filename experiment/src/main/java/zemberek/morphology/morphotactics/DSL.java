package zemberek.morphology.morphotactics;

import static java.util.Arrays.asList;
import static zemberek.morphology.morphotactics.Operator.AND;
import static zemberek.morphology.morphotactics.Operator.OR;

import java.util.Collection;

class DSL {

  public static Condition and(Condition left, Condition right) {
    return condition(AND, left, right);
  }

  public static Condition and(Collection<? extends Condition> conditions) {
    return condition(AND, conditions);
  }

  public static Condition and(Condition... conditions) {
    return condition(AND, conditions);
  }


  public static Condition condition(Operator operator, Condition left, Condition right) {
    return CombinedCondition.of(operator, left, right);
  }

  public static Condition condition(Operator operator, Condition... conditions) {
    return condition(operator, asList(conditions));
  }

  public static Condition condition(Operator operator, Collection<? extends Condition> conditions) {
    return CombinedCondition.of(operator, conditions);
  }

  public static Condition or(Condition left, Condition right) {
    return condition(OR, left, right);
  }

  public static Condition or(Condition... conditions) {
    return condition(OR, conditions);
  }

  public static Condition or(Collection<? extends Condition> conditions) {
    return condition(OR, conditions);
  }

  public static Condition not(Condition condition) {
    return condition.not();
  }

}
