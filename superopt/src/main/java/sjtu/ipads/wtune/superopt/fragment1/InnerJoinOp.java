package sjtu.ipads.wtune.superopt.fragment1;

class InnerJoinOp extends JoinOp implements InnerJoin {
  InnerJoinOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterInnerJoin(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveInnerJoin(this);
  }
}
