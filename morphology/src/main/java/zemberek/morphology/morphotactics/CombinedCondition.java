package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import zemberek.morphology.analysis.SearchPath;

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

  public Condition getFailingCondition(SearchPath path) {
    if (conditions.size() == 0) {
      return null;
    }
    if (conditions.size() == 1) {
      Condition condition = conditions.get(0);
      return condition.accept(path) ? null : condition;
    }
    if (operator == Operator.AND) {
      for (Condition condition : conditions) {
        if (!condition.accept(path)) {
          return condition;
        }
      }
      return null;
    } else {
      boolean pass = false;
      for (Condition condition : conditions) {
        if (condition.accept(path)) {
          pass = true;
        }
      }
      // for OR, we do not have specific failing condition.
      // So we return this as failing condition.
      return pass ? null : this;
    }
  }

  public String toString() {
    if (conditions.size() == 0) {
      return "[No-Condition]";
    }
    if (conditions.size() == 1) {
      return conditions.get(0).toString();
    }
    if (operator == Operator.AND) {
      StringBuilder sb = new StringBuilder();
      int i = 0;
      for (Condition condition : conditions) {
        sb.append(condition.toString());
        if (i++ < conditions.size() - 1) {
          sb.append(" AND ");
        }
      }
      return sb.toString();
    } else {
      int i = 0;
      StringBuilder sb = new StringBuilder();
      for (Condition condition : conditions) {
        sb.append(condition.toString());
        if (i++ < conditions.size() - 1) {
          sb.append(" OR ");
        }
      }
      return sb.toString();
    }
  }

  // counts the number of conditions.
  public int count() {
    if (conditions.size() == 0) {
      return 0;
    }
    if (conditions.size() == 1) {
      Condition first = conditions.get(0);
      if (first instanceof CombinedCondition) {
        return ((CombinedCondition) first).count();
      } else {
        return 1;
      }
    }
    int cnt = 0;
    for (Condition condition : conditions) {
      if (condition instanceof CombinedCondition) {
        cnt += ((CombinedCondition) condition).count();
      } else {
        cnt++;
      }
    }
    return cnt;
  }

}
