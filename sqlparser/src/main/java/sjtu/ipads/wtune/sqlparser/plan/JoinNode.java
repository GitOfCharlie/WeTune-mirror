package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

public interface JoinNode extends PlanNode {
  JoinKind joinKind();

  Expression joinCond();

  @Override
  default PlanKind kind() {
    return PlanKind.Join;
  }

  static JoinNode mk(JoinKind kind, Expression joinCond) {
    return new JoinNodeImpl(kind, joinCond);
  }
}
