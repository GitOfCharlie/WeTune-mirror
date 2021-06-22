package sjtu.ipads.wtune.sqlparser.plan1;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Agg;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Input;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Limit;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Sort;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SubqueryFilter;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Union;
import static sjtu.ipads.wtune.sqlparser.plan1.ExprImpl.buildColumnRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

class RefResolver {
  private final PlanNode plan;
  private final PlanContext ctx;
  private StackedLookup lookup;

  private RefResolver(PlanNode plan, PlanContext ctx) {
    this.plan = plan;
    this.ctx = ctx;
  }

  static void resolve(PlanNode plan) {
    final PlanContext ctx = PlanContext.build();
    new RefResolver(plan, ctx).onNode(plan);
    PlanContext.installContext(ctx, plan);
  }

  private void onNode(PlanNode node) {
    final boolean needNewLookup = needNewLookup(node);
    final boolean needStackLookup = needStackLookup(node);
    final boolean needMergeLookup = needMergeLookup(needNewLookup, node);

    assert !needStackLookup || !needNewLookup;
    assert !needStackLookup || !needMergeLookup;
    assert !needMergeLookup || needNewLookup;

    final StackedLookup currentLookup = lookup;
    if (needNewLookup) lookup = new StackedLookup(null);
    if (needStackLookup) lookup = new StackedLookup(currentLookup);

    switch (node.type()) {
      case Input:
        onInput((InputNode) node);
        break;
      case InnerJoin:
      case LeftJoin:
        onJoin((JoinNode) node);
        break;
      case PlainFilter:
      case SubqueryFilter:
        onFilter((FilterNode) node);
        break;
      case Proj:
        onProj((ProjNode) node);
        break;
      case Agg:
        onAgg((AggNode) node);
        break;
      case Sort:
        onSort((SortNode) node);
        break;
      case Limit:
        onLimit((LimitNode) node);
        break;
      case Union:
        onUnion((SetOpNode) node);
        break;
      default:
        throw failed("unsupported operator " + node.type());
    }

    if (needMergeLookup) currentLookup.addAll(lookup.values);

    lookup = currentLookup;
  }

  private void onInput(InputNode node) {
    registerValues(node, node.values());
  }

  private void onJoin(JoinNode node) {
    onNode(node.predecessors()[0]);
    onNode(node.predecessors()[1]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, false);

    if (node.isEquiJoin()) {
      final List<Ref> lhsRefs = new ArrayList<>(refs.size() >> 1);
      final List<Ref> rhsRefs = new ArrayList<>(refs.size() >> 1);

      for (Ref ref : refs) {
        final Value value = ctx.deRef(ref);
        if (isLhs(node, ctx.ownerOf(value))) lhsRefs.add(ref);
        else rhsRefs.add(ref);
      }

      if (lhsRefs.size() != rhsRefs.size()) throw failed("ill-formed equi-join: " + node);

      node.setLhsRefs(new RefBagImpl(lhsRefs));
      node.setRhsRefs(new RefBagImpl(rhsRefs));
    }
  }

  private void onFilter(FilterNode node) {
    onNode(node.predecessors()[0]);
    if (node.type().numPredecessors() >= 2) onNode(node.predecessors()[1]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, true);
  }

  private void onProj(ProjNode node) {
    onNode(node.predecessors()[0]);

    if (node.containsWildcard()) {
      final ValueBag values = node.values();
      final List<Value> expanded = new ArrayList<>(values.size());
      for (Value value : values) {
        if (!(value instanceof WildcardValue)) expanded.add(value);
        else
          for (Value base : lookup(value.wildcardQualification())) {
            final Expr expr = buildColumnRef(base.qualification(), base.name());
            final ExprValue newValue = new ExprValue(base.name(), expr);
            newValue.setQualification(value.qualification());

            expanded.add(newValue);
          }
      }
      node.setValues(new ValueBagImpl(expanded));
    }

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, true);

    lookup.swap();
    registerValues(node, node.values());
  }

  private void onAgg(AggNode node) {
    onNode(node.predecessors()[0]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, true, false);

    lookup.clear();
    registerValues(node, node.values());
  }

  private void onSort(SortNode node) {
    onNode(node.predecessors()[0]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, false);
  }

  private void onLimit(LimitNode node) {
    onNode(node.predecessors()[0]);
  }

  private void onUnion(SetOpNode node) {
    onNode(node.predecessors()[0]);
    onNode(node.predecessors()[1]);
  }

  private void setRef(Ref ref, Value value) {
    ctx.setRef(ref, value);
  }

  private void registerValues(PlanNode node, ValueBag values) {
    ctx.registerValues(node, values);
    lookup.addAll(values);
  }

  private void registerRefs(PlanNode node, RefBag refs) {
    ctx.registerRefs(node, refs);
  }

  private Value lookup(Ref ref, boolean useAux, boolean recursive) {
    final Value value =
        lookup.lookup(ref.intrinsicQualification(), ref.intrinsicName(), useAux, recursive);
    if (value == null) throw failed("unknown ref " + ref);
    return value;
  }

  private List<Value> lookup(String qualification) {
    final List<Value> values = this.lookup.lookup(qualification);
    if (values == null)
      throw failed("unknown ref " + (qualification == null ? "*" : qualification + "*"));
    return values;
  }

  private void resolveRefs(Iterable<Ref> refs, boolean auxFirst, boolean recursive) {
    for (Ref ref : refs) {
      final Value value = lookup(ref, auxFirst, recursive);
      setRef(ref, value);
    }
  }

  private boolean isLhs(PlanNode root, PlanNode descent) {
    assert root.type().numPredecessors() == 2;

    final PlanNode savedDescent = descent;
    while (descent != null) {
      if (descent.successor() == root) return root.predecessors()[0] == descent;
      descent = descent.successor();
    }

    throw failed("%s not a descent of %s".formatted(savedDescent, root));
  }

  private RuntimeException failed(String reason) {
    throw new IllegalArgumentException(
        "failed to bind reference to value. [" + reason + "] " + plan);
  }

  private static boolean needNewLookup(PlanNode node) {
    final PlanNode successor = node.successor();
    if (successor == null) return true;

    final OperatorType succType = successor.type();
    final OperatorType nodeType = node.type();

    if (succType == Union) return true;
    if (succType == SubqueryFilter && successor.predecessors()[1] == node)
      return false; // needStack
    if (nodeType == Input || nodeType.isJoin()) return false;
    if (nodeType.isFilter()) return succType.isJoin();
    if (nodeType == Proj) return succType != Agg && succType != Sort && succType != Limit;
    if (nodeType == Agg) return succType != Sort && succType != Limit;
    if (nodeType == Sort) return succType != Limit;
    return nodeType == Limit || nodeType == Union;
  }

  private static boolean needStackLookup(PlanNode node) {
    final PlanNode successor = node.successor();
    return successor != null
        && successor.type() == SubqueryFilter
        && successor.predecessors()[1] == node;
  }

  private static boolean needMergeLookup(boolean needNew, PlanNode node) {
    final PlanNode successor = node.successor();
    if (successor == null) return false;
    return needNew && (successor.type() != Union || node == successor.predecessors()[0]);
  }

  private static class StackedLookup {
    private final StackedLookup previous;
    private List<Value> values = new ArrayList<>();
    private List<Value> auxValues;

    private StackedLookup(StackedLookup previous) {
      this.previous = previous;
    }

    Value lookup(String qualification, String name, boolean auxFirst, boolean recursive) {
      final Value value0 = lookup0(qualification, name, auxValues);
      final Value value1 = lookup0(qualification, name, values);

      if (value0 != null && (value1 == null || auxFirst)) return value0;
      if (value1 != null) return value1;
      if (recursive && previous != null)
        return previous.lookup(qualification, name, auxFirst, true);

      return null;
    }

    List<Value> lookup(String qualification) {
      if (qualification == null) return values;
      else return listFilter(it -> qualification.equals(it.qualification()), values);
    }

    private static Value lookup0(String qualification, String name, List<Value> values) {
      if (name == null || name.isEmpty()) throw new IllegalArgumentException();
      if (values == null) return null;

      for (Value value : values)
        if ((qualification == null || qualification.equalsIgnoreCase(value.qualification()))
            && name.equalsIgnoreCase(value.name())) return value;

      return null;
    }

    void addAll(Collection<Value> values) {
      this.values.addAll(values);
    }

    void swap() {
      assert auxValues == null;
      auxValues = values;
      values = new ArrayList<>();
    }

    void clear() {
      values.clear();
    }
  }
}
