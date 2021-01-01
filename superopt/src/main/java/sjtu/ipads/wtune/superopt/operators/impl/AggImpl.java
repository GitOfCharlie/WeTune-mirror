package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.AggFuncs;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public class AggImpl extends BaseOperator implements Agg {
  private Abstraction<GroupKeys> groupKeys;
  private Abstraction<AggFuncs> aggFuncs;

  private AggImpl() {}

  public static AggImpl create() {
    return new AggImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterAgg(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveAgg(this);
  }

  @Override
  public Abstraction<GroupKeys> groupKeys() {
    if (groupKeys == null) groupKeys = Abstraction.create(this, "");
    return groupKeys;
  }

  @Override
  public Abstraction<AggFuncs> aggFuncs() {
    if (aggFuncs == null) aggFuncs = Abstraction.create(this, "");
    return aggFuncs;
  }

  @Override
  protected RelationSchema createOutSchema() {
    return RelationSchema.create(this);
  }

  @Override
  public String toString() {
    return "Agg" + id();
  }
}