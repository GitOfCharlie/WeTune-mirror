package sjtu.ipads.wtune.sql.plan;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.COW;
import sjtu.ipads.wtune.sql.schema.Column;

import java.util.*;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.IterableSupport.zip;
import static sjtu.ipads.wtune.common.utils.ListSupport.join;

class ValuesRegistryImpl implements ValuesRegistry {
  private int nextId;
  private final PlanContext ctx;
  private final COW<TIntObjectMap<Values>> nodeValues;
  private final COW<TIntObjectMap<Column>> valueColumns;
  private final COW<TIntObjectMap<Expression>> valueExprs;
  private final COW<Map<Expression, Values>> exprRefs;

  protected ValuesRegistryImpl(PlanContext ctx) {
    this.nextId = 0;
    this.ctx = ctx;
    this.nodeValues = new COW<>(new TIntObjectHashMap<>(ctx.maxNodeId()), null);
    this.valueColumns = new COW<>(new TIntObjectHashMap<>(), null);
    this.valueExprs = new COW<>(new TIntObjectHashMap<>(), null);
    this.exprRefs = new COW<>(new IdentityHashMap<>(), null);
  }

  protected ValuesRegistryImpl(ValuesRegistryImpl toCopy, PlanContext newPlan) {
    this.ctx = newPlan;
    this.nextId = toCopy.nextId;
    this.nodeValues = new COW<>(toCopy.nodeValues.forRead(), TIntObjectHashMap::new);
    this.valueExprs = new COW<>(toCopy.valueExprs.forRead(), TIntObjectHashMap::new);
    this.exprRefs = new COW<>(toCopy.exprRefs.forRead(), HashMap::new);
    this.valueColumns = new COW<>(toCopy.valueColumns.forRead(), TIntObjectHashMap::new);
  }

  @Override
  public Values valuesOf(int nodeId) {
    if (nodeId == NO_SUCH_NODE) return Values.mk(emptyList());

    Values values = nodeValues.forRead().get(nodeId);
    if (values != null) return values;

    List<Expression> exprs = null;
    switch (ctx.kindOf(nodeId)) {
      case Filter:
      case InSub:
      case Exists:
      case SetOp:
      case Limit:
      case Sort:
        return valuesOf(ctx.childOf(nodeId, 0));

      case Join:
        return Values.mk(join(valuesOf(ctx.childOf(nodeId, 0)), valuesOf(ctx.childOf(nodeId, 1))));

      case Proj:
      case Agg:
        final Pair<Values, List<Expression>> pair =
            mkValuesOfExporter((Exporter) ctx.nodeAt(nodeId));
        values = pair.getLeft();
        exprs = pair.getRight();
        break;

      case Input:
        values = mkValuesOfInput((InputNode) ctx.nodeAt(nodeId));
        break;

      default:
        throw new PlanException("unknown plan node kind: " + ctx.kindOf(nodeId));
    }

    bindValues(nodeId, values);
    if (exprs != null) zip(values, exprs, this::registerExpr);

    return values;
  }

  @Override
  public int initiatorOf(Value value) {
    final InitiatorFinder finder = new InitiatorFinder(value);
    nodeValues.forRead().forEachEntry(finder);
    return finder.initiator;
  }

  @Override
  public Column columnOf(Value value) {
    return valueColumns.forRead().get(value.id());
  }

  @Override
  public Expression exprOf(Value value) {
    return valueExprs.forRead().get(value.id());
  }

  @Override
  public void bindValueRefs(Expression expr, List<Value> valueRefs) {
    final Values vs;
    if (valueRefs instanceof Values) vs = (Values) valueRefs;
    else vs = Values.mk(valueRefs);
    exprRefs.forWrite().put(expr, vs);
  }

  @Override
  public Values valueRefsOf(Expression expr) {
    return exprRefs.forRead().get(expr);
  }

  @Override
  public void displaceRef(Value oldRef, Value newRef, Set<Expression> excludedExpression) {
    for (var pair : exprRefs.forWrite().entrySet()) {
      if (excludedExpression.contains(pair.getKey())) continue;
      final Values refs = pair.getValue();
      for (int i = 0, bound = refs.size(); i < bound; i++) {
        if (refs.get(i) == oldRef) refs.set(i, newRef);
      }
    }
  }

  @Override
  public void bindValues(int nodeId, List<Value> rawValues) {
    final Values values;
    if (rawValues instanceof Values) values = (Values) rawValues;
    else values = Values.mk(rawValues);
    nodeValues.forWrite().put(nodeId, values);
  }

  void relocateNode(int from, int to) {
    final Values values = nodeValues.forRead().get(from);
    if (values != null) nodeValues.forWrite().put(to, values);
  }

  void deleteNode(int id) {
    if (nodeValues.forRead().containsKey(id)) {
      final Values values = nodeValues.forWrite().remove(id);
      if (values != null && initiatorOf(values.get(0)) == NO_SUCH_NODE)
        for (Value value : values) removeValue(value);
    }
  }

  private void removeValue(Value value) {
    final int valueId = value.id();
    if (valueColumns.forRead().containsKey(valueId)) valueColumns.forWrite().remove(valueId);
    if (valueExprs.forRead().containsKey(valueId)) {
      final Expression expr = valueExprs.forWrite().remove(valueId);
      if (expr != null) exprRefs.forWrite().remove(expr);
    }
  }

  private Values mkValuesOfInput(InputNode input) {
    final Collection<Column> columns = input.table().columns();
    final String qualification = input.qualification();

    final Values values = Values.mk();
    for (Column column : columns) {
      final Value value = Value.mk(++nextId, qualification, column.name());
      values.add(value);
      valueColumns.forWrite().put(value.id(), column);
    }

    return values;
  }

  private Pair<Values, List<Expression>> mkValuesOfExporter(Exporter exporter) {
    final String qualification = exporter.qualification();
    final List<String> attrNames = exporter.attrNames();

    final Values values = Values.mk();
    for (String attrName : attrNames) values.add(Value.mk(++nextId, qualification, attrName));

    return Pair.of(values, exporter.attrExprs());
  }

  private void registerExpr(Value value, Expression expr) {
    valueExprs.forWrite().put(value.id(), expr);
  }

  private static class InitiatorFinder implements TIntObjectProcedure<Values> {
    private int initiator = NO_SUCH_NODE;
    private final Value target;

    private InitiatorFinder(Value target) {
      this.target = target;
    }

    @Override
    public boolean execute(int a, Values b) {
      if (b.contains(target)) {
        initiator = a;
        return false;
      }
      return true;
    }
  }
}