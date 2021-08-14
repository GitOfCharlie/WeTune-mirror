package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;

class SetOpNodeImpl extends PlanNodeBase implements SetOpNode {
  private final boolean distinct;
  private final SetOperation operation;

  SetOpNodeImpl(SetOperation operation, boolean distinct) {
    this.distinct = distinct;
    this.operation = operation;
  }

  @Override
  public SetOperation operation() {
    return operation;
  }

  @Override
  public ValueBag values() {
    return predecessors[0].values();
  }

  @Override
  public RefBag refs() {
    return RefBag.empty();
  }

  @Override
  public boolean distinct() {
    return distinct;
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    final SetOpNode copy = new SetOpNodeImpl(operation, distinct);
    copy.setContext(ctx);
    return copy;
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append(kind().text());
    if (distinct) builder.append("{distinct}");
    stringifyChildren(builder);
    return builder;
  }
}
