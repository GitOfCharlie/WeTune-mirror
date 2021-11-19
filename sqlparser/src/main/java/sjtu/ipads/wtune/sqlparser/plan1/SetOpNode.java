package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.constants.SetOpKind;

public interface SetOpNode extends PlanNode {
  boolean deduplicated();

  SetOpKind opKind();

  @Override
  default PlanKind kind() {
    return PlanKind.SetOp;
  }

  static SetOpNode mk(boolean deduplicated, SetOpKind opKind) {
    return new SetOpNodeImpl(deduplicated, opKind);
  }
}