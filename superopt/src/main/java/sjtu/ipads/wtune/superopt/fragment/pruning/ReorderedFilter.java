package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.InSubFilter;

public class ReorderedFilter extends BaseMatchingRule {
  @Override
  public boolean enterInSubFilter(InSubFilter op) {
    if (op.predecessors()[0] != null && op.predecessors()[0].kind() == OperatorType.SIMPLE_FILTER) {
      matched = true;
      return false;
    }
    return true;
  }
}