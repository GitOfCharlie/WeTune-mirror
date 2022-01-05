package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.sql.ast.constants.JoinKind;

class JoinNodeImpl implements JoinNode {
  private final JoinKind joinKind;
  private final Expression joinCond;

  JoinNodeImpl(JoinKind joinKind, Expression joinCond) {
    this.joinKind = joinKind;
    this.joinCond = joinCond;
  }

  @Override
  public JoinKind joinKind() {
    return joinKind;
  }

  @Override
  public Expression joinCond() {
    return joinCond;
  }
}
