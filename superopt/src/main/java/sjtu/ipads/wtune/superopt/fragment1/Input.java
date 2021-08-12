package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

public interface Input extends Op {
  Symbol table();

  @Override
  default OperatorType type() {
    return OperatorType.INPUT;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    return m.assign(table(), node);
  }

  @Override
  default PlanNode instantiate(PlanContext ctx, Model m) {
    return m.interpretTable(table()).copy(ctx);
  }
}
