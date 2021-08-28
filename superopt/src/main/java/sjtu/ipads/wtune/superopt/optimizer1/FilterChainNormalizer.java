package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.CombinedFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

class FilterChainNormalizer {
  static FilterNode normalize(FilterNode node) {
    return (FilterNode) normalize0(node, true);
  }

  private static PlanNode normalize0(PlanNode node, boolean isRoot) {
    if (node.kind().isFilter()
        && (node.successor() == null || !node.successor().kind().isFilter())) {
      final FilterNode chainHead = (FilterNode) node;
      if (containsCombinedFilterNode(chainHead))
        node = FilterChain.mk(chainHead, true).buildChain();
    }

    for (int i = 0, bound = node.kind().numPredecessors(); i < bound; i++)
      node = normalize0(node.predecessors()[i], false);

    return isRoot ? node : node.successor();
  }

  private static boolean containsCombinedFilterNode(FilterNode chainHead) {
    PlanNode path = chainHead;
    while (path.kind().isFilter()) {
      if (path instanceof CombinedFilterNode) return true;
      path = path.predecessors()[0];
    }
    return false;
  }
}
