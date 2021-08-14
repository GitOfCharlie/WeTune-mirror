package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.internal.PlainFilterImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface PlainFilter extends Filter {
  Placeholder predicate();

  @Override
  default OperatorType kind() {
    return OperatorType.SIMPLE_FILTER;
  }

  static PlainFilter create() {
    return PlainFilterImpl.create();
  }

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    final PlanNode predecessor = predecessors()[0].instantiate(interpretations);
    final PlanNode node =
        FilterNode.makePlainFilter(
            interpretations.getPredicate(predicate()).object(),
            interpretations.getAttributes(fields()).object());
    node.setPredecessor(0, predecessor);
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (!node.kind().isFilter()) return false;
    final FilterNode filter = (FilterNode) node;
    return inter.assignAttributes(fields(), filter.usedAttributes())
        && inter.assignPredicate(predicate(), filter.predicate());
  }
}
