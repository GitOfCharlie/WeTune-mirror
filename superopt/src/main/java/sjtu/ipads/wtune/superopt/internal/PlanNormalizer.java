package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

public class PlanNormalizer {
  public static boolean normalize(PlanNode node) {
    final PlanNode successor = node.successor();
    final PlanNode[] predecessors = node.predecessors();
    for (PlanNode predecessor : predecessors) if (!normalize(predecessor)) return false;

    // Join(Input,LeftJoin): non-left deep join tree, and cannot be refactor as left-deep
    if (node.type().isJoin() && node.predecessors()[1].type() == LeftJoin) return false;

    // remove unnecessary wildcard projection
    // e.g. select sub.a from (select * from x) sub => select x.a from x
    if (node.type() == Proj
        && ((ProjNode) node).isWildcard()
        && successor != null
        && successor.type().isJoin()
        && !predecessors[0].type().isFilter()) {
      successor.replacePredecessor(node, predecessors[0]);
      return true;
    }

    if (node.type().isJoin()) {
      // insert proj if a filter directly precedes a join
      if (predecessors[0].type().isFilter()) insertProj(node, predecessors[0]);
      if (predecessors[1].type().isFilter()) insertProj(node, predecessors[1]);
      // enforce left-deep join tree
      final PlanNode old = node;
      node = enforceLeftDeepJoin((JoinNode) node);
      successor.replacePredecessor(old, node);
      // add qualification
      rectifyQualification(node);
      return true;
    }

    node.resolveUsed();
    return true;
  }

  private static void insertProj(PlanNode successor, PlanNode predecessor) {
    final List<ASTNode> exprs =
        listMap(PlanNormalizer::makeSelectItem, predecessor.definedAttributes());
    final ProjNode proj = ProjNode.make(null, exprs);
    successor.replacePredecessor(predecessor, proj);
    proj.setWildcard(true);
    proj.setPredecessor(0, predecessor);
    proj.resolveUsed();
  }

  private static ASTNode makeSelectItem(AttributeDef def) {
    final ASTNode expr = def.toColumnRef();
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, expr);
    return item;
  }

  private static JoinNode enforceLeftDeepJoin(JoinNode join) {
    final PlanNode right = join.predecessors()[1];
    assert right.type() != LeftJoin;

    if (right.type() != InnerJoin) {
      join.resolveUsed();
      return join;
    }

    final JoinNode newJoin = (JoinNode) right;

    final PlanNode b = right.predecessors()[0]; // b can be another JOIN
    final PlanNode c = right.predecessors()[1]; // c must not be a JOIN
    assert !c.type().isJoin();

    if (b.definedAttributes().containsAll(join.rightAttributes())) {
      // 1. join<a.x=b.y>(a,join<b.z=c.w>(b,c)) => join<b.z=c.w>(join<a.x=b.y>(a,b),c)
      join.setPredecessor(1, b);
      newJoin.setPredecessor(0, join);
      newJoin.setPredecessor(1, c);
      join.resolveUsed();
      enforceLeftDeepJoin(join);
      return newJoin;

    } else {
      // 2. join<a.x=c.y>(a,join<b.z=c.w>(b,c)) => join<b.z=c.w>(join<a.x=c.y>(a,c),b)
      join.setPredecessor(1, c);
      newJoin.setPredecessor(0, join);
      newJoin.setPredecessor(1, b);
      newJoin.resolveUsed();
      return enforceLeftDeepJoin(newJoin);
    }
  }

  private static void rectifyQualification(PlanNode node) {
    if (!node.type().isJoin()) return;

    final PlanNode left = node.predecessors()[0], right = node.predecessors()[1];
    assert right.type() == Proj || right.type() == OperatorType.Input;

    final Map<String, PlanNode> qualified = new HashMap<>();
    final Set<PlanNode> unqualified = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AttributeDef attr : listJoin(left.definedAttributes(), right.definedAttributes())) {
      final String qualification = attr.qualification();
      final PlanNode definer = attr.definer();
      if (qualification == null
          || qualified.compute(qualification, (s, n) -> coalesce(n, definer)) != definer)
        unqualified.add(definer);
    }

    for (PlanNode n : unqualified) {
      final String qualification = makeQualification(qualified.keySet());
      setQualification(n, qualification);
      qualified.put(qualification, n);
    }
  }

  private static String makeQualification(Set<String> existing) {
    int i = 0;
    while (true) {
      final String qualification = "sub" + i;
      if (!existing.contains(qualification)) return qualification;
      ++i;
    }
  }

  private static void setQualification(PlanNode node, String qualification) {
    assert node.type() == Proj || node.type() == OperatorType.Input;
    if (node.type() == OperatorType.Input) ((InputNode) node).setAlias(qualification);
    node.definedAttributes().forEach(it -> it.setQualification(qualification));
  }
}
