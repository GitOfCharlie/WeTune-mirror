package sjtu.ipads.wtune.superopt.fragment1;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.bindValues;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Ref;
import sjtu.ipads.wtune.sqlparser.plan1.RefBag;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

public interface Join extends Op {
  Symbol lhsAttrs();

  Symbol rhsAttrs();

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.type() != this.type()) return false;

    final JoinNode join = (JoinNode) node;
    if (!join.isEquiJoin()) return false;

    final PlanContext ctx = join.context();

    return m.assign(lhsAttrs(), ctx.deRef(join.lhsRefs()))
        && m.assign(rhsAttrs(), ctx.deRef(join.rhsRefs()));
  }

  @Override
  default PlanNode instantiate(PlanContext ctx, Model m) {
    final PlanNode predecessor0 = predecessors()[0].instantiate(ctx, m);
    final PlanNode predecessor1 = predecessors()[1].instantiate(ctx, m);

    final List<Value> lhsValues = bindValues(m.interpretAttrs(lhsAttrs()), predecessor0);
    final List<Value> rhsValues = bindValues(m.interpretAttrs(rhsAttrs()), predecessor1);
    final List<Ref> lhsRefs = listMap(lhsValues, Value::selfish);
    final List<Ref> rhsRefs = listMap(rhsValues, Value::selfish);
    final JoinNode join = JoinNode.mk(type(), RefBag.mk(lhsRefs), RefBag.mk(rhsRefs));

    join.setContext(ctx);
    join.setPredecessor(0, predecessor0);
    join.setPredecessor(1, predecessor1);

    ctx.registerRefs(join, join.lhsRefs());
    ctx.registerRefs(join, join.rhsRefs());
    zipForEach(lhsRefs, lhsValues, ctx::setRef);
    zipForEach(rhsRefs, rhsValues, ctx::setRef);

    return join;
  }
}
