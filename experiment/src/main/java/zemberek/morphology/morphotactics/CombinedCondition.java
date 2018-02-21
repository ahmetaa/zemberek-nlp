package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import zemberek.morphology.analyzer.SearchPath;

// TODO: add tests!!
public class CombinedCondition extends AbstractCondition {

  Operator operator;

  List<Condition> conditions;

  static Condition of(Operator operator, Condition left, Condition right) {
    return new CombinedCondition(operator, left, right);
  }

  private CombinedCondition(Operator operator, Condition left, Condition right) {
    this(operator, 2);

    add(operator, left);
    add(operator, right);
  }

  private CombinedCondition(Operator operator, int size) {
    if (operator == null) {
      throw new IllegalArgumentException("The argument 'operator' must not be null");
    }
    this.operator = operator;
    this.conditions = new ArrayList<>(size);
  }

  private CombinedCondition add(Operator op, Condition condition) {
    if (condition instanceof CombinedCondition) {
      CombinedCondition combinedCondition = (CombinedCondition) condition;

      if (combinedCondition.operator == op) {
        this.conditions.addAll(combinedCondition.conditions);
      } else {
        this.conditions.add(condition);
      }
    } else if (condition == null) {
      throw new IllegalArgumentException("The argument 'conditions' must not contain null");
    } else {
      this.conditions.add(condition);
    }

    return this;
  }

  static Condition of(Operator operator, Collection<? extends Condition> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      throw new IllegalArgumentException("conditions must not be null or empty.");
    }

    CombinedCondition result = null;
    Condition first = null;

    for (Condition condition : conditions) {
      if (first == null) {
        first = condition;
      } else if (result == null) {
        (result = new CombinedCondition(operator, conditions.size()))
            .add(operator, first)
            .add(operator, condition);
      } else {
        result.add(operator, condition);
      }
    }

    if (result != null) {
      return result;
    } else {
      return first;
    }
  }

  @Override
  public boolean accept(SearchPath path) {
    if (conditions.size() == 0) {
      return true;
    }
    if (conditions.size() == 1) {
      return conditions.get(0).accept(path);
    }
    if (operator == Operator.AND) {
      for (Condition condition : conditions) {
        if (!condition.accept(path)) {
          return false;
        }
      }
      return true;
    } else {
      for (Condition condition : conditions) {
        if (condition.accept(path)) {
          return true;
        }
      }
      return false;
    }
  }
}
