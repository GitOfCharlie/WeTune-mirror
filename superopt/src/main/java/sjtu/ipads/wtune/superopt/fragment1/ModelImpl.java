package sjtu.ipads.wtune.superopt.fragment1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

class ModelImpl implements Model {
  private final ModelImpl base;
  private final Map<Symbol, Object> assignments;

  ModelImpl(ModelImpl base) {
    this.base = base;
    this.assignments = new HashMap<>(8);
  }

  // TODO: conflict check

  @Override
  public boolean assign(Symbol table, PlanNode input) {
    assignments.put(table, input);
    return true;
  }

  @Override
  public boolean assign(Symbol pred, Expr predicate) {
    assignments.put(pred, predicate);
    return true;
  }

  @Override
  public boolean assign(Symbol attrs, List<Value> values) {
    assignments.put(attrs, values);
    return true;
  }

  @Override
  public PlanNode interpretTable(Symbol table) {
    return get0(table);
  }

  @Override
  public Expr interpretPred(Symbol pred) {
    return get0(pred);
  }

  @Override
  public List<Value> interpretAttrs(Symbol pred) {
    return get0(pred);
  }

  @Override
  public Model derive() {
    return new ModelImpl(this);
  }

  private <T> T get0(Symbol key) {
    final Object obj = assignments.get(key);
    if (obj != null) return (T) obj;
    else if (base != null) return base.get0(key);
    else return null;
  }
}
