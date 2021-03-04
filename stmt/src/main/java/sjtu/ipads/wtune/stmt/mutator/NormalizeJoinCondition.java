package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.stmt.utils.Collector;

import java.util.*;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.FuncUtils.consumer2;
import static sjtu.ipads.wtune.sqlparser.ASTContext.LOG;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;

class NormalizeJoinCondition implements ASTVistor {
  public static ASTNode normalize(ASTNode root) {
    Collector.collect(root, QUERY_SPEC::isInstance, false).forEach(NormalizeJoinCondition::process);
    return root;
  }

  private static void process(ASTNode querySpec) {
    // step 1: move redundant ON-condition to where
    // e.g. a join b on a.id = b.ref and b.name = 'alice'
    //   => a join b on a.id = b.ref where b.name = 'alice'
    final List<ASTNode> plainConds = collectPlainCondition(querySpec);
    plainConds.forEach(NormalizeJoinCondition::removeCondition);
    plainConds.forEach(consumer2(NormalizeJoinCondition::addWhereCondition).bind0(querySpec));

    // step 2: move join condition in where to JOIN
    // e.g. a join b where a.id = b.ref and b.name = 'alice'
    //   => a join b on a.id = b.ref and b.name = 'alice'
    final List<ASTNode> joinConds = collectJoinCondition(querySpec.get(QUERY_SPEC_WHERE));
    final List<Relation> allInputs = querySpec.get(RELATION).inputs();
    for (int i = 1, bound = allInputs.size(); i < bound; i++) {
      final ListIterator<ASTNode> iter = joinConds.listIterator();
      while (iter.hasNext()) {
        final ASTNode cond = iter.next();
        final List<Relation> inputs = allInputs.subList(0, i + 1);
        if (isAttributePresent(cond.get(BINARY_LEFT).get(ATTRIBUTE), inputs)
            && isAttributePresent(cond.get(BINARY_RIGHT).get(ATTRIBUTE), inputs)) {
          addOnCondition(allInputs.get(i).node().parent(), cond);
          iter.remove();
        }
      }
    }
  }

  private static List<ASTNode> collectPlainCondition(ASTNode root) {
    final PlainConditionCollector collector = new PlainConditionCollector();
    root.accept(collector);
    return collector.plainCondition;
  }

  private static List<ASTNode> collectJoinCondition(ASTNode root) {
    if (root == null) return Collections.emptyList();

    final JoinConditionCollector collector = new JoinConditionCollector();
    root.accept(collector);
    return collector.joinConditions;
  }

  private static void removeCondition(ASTNode node) {
    final ASTNode parent = node.parent();

    if (QUERY_SPEC.isInstance(parent)) parent.unset(QUERY_SPEC_WHERE);
    if (!BINARY.isInstance(parent)) return;

    final ASTNode left = parent.get(BINARY_LEFT), right = parent.get(BINARY_RIGHT);
    if (left == node) parent.update(right);
    else if (right == node) parent.update(left);
    else assert false;
  }

  private static void addWhereCondition(ASTNode querySpec, ASTNode expr) {
    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    if (where == null) querySpec.set(QUERY_SPEC_WHERE, expr);
    else querySpec.set(QUERY_SPEC_WHERE, makeConjunction(where, expr));
  }

  private static void addOnCondition(ASTNode join, ASTNode expr) {
    final ASTNode on = join.get(JOINED_ON);
    if (on == null) join.set(JOINED_ON, expr);
    else join.set(JOINED_ON, makeConjunction(on, expr));
  }

  private static ASTNode makeConjunction(ASTNode left, ASTNode right) {
    final ASTNode and = ASTNode.expr(BINARY);
    and.set(BINARY_OP, BinaryOp.AND);
    and.set(BINARY_LEFT, left);
    and.set(BINARY_RIGHT, right);
    return and;
  }

  private static boolean isAttributePresent(Attribute attr, List<Relation> relations) {
    for (Relation relation : relations) if (relation.attributes().contains(attr)) return true;
    return false;
  }

  private static class PlainConditionCollector implements ASTVistor {
    private final List<ASTNode> plainCondition = new ArrayList<>();

    @Override
    public boolean enterQuery(ASTNode query) {
      return false;
    }

    @Override
    public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
      if (key != QUERY_SPEC_FROM && key != JOINED_LEFT && key != JOINED_RIGHT && key != JOINED_ON)
        return false;
      if (key != JOINED_ON) return true;
      if (!parent.get(JOINED_TYPE).isInner()) return false;
      if (child == null) return false;
      collectExprsToMove(child);
      return false;
    }

    private void collectExprsToMove(ASTNode expr) {
      if (!BINARY.isInstance(expr)) return;

      final BinaryOp op = expr.get(BINARY_OP);
      final ASTNode left = expr.get(BINARY_LEFT);
      final ASTNode right = expr.get(BINARY_RIGHT);

      if (op.isLogic())
        if (op == BinaryOp.AND) {
          collectExprsToMove(left);
          collectExprsToMove(right);
          return;
        } else return;

      if (!COLUMN_REF.isInstance(left) || !COLUMN_REF.isInstance(right)) {
        plainCondition.add(expr);

        if (!BINARY.isInstance(expr.parent()))
          LOG.log(WARNING, "Wierd join condition: {0}", expr.parent());
      }
    }
  }

  private static class JoinConditionCollector implements ASTVistor {
    private final List<ASTNode> joinConditions = new LinkedList<>();

    @Override
    public boolean enterQuery(ASTNode query) {
      return false;
    }

    @Override
    public boolean enterBinary(ASTNode binary) {
      final BinaryOp op = binary.get(BINARY_OP);
      if (op == BinaryOp.AND) return true;
      if (op != BinaryOp.EQUAL) return false;
      if (binary.get(BINARY_LEFT).get(ATTRIBUTE) != null
          && binary.get(BINARY_RIGHT).get(ATTRIBUTE) != null) joinConditions.add(binary);
      return false;
    }
  }
}