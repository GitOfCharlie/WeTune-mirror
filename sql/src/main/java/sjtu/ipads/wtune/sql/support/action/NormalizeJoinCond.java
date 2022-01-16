package sjtu.ipads.wtune.sql.support.action;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.support.resolution.Attribute;
import sjtu.ipads.wtune.sql.support.resolution.Relation;

import java.util.List;

import static sjtu.ipads.wtune.sql.SqlSupport.isColRefEq;
import static sjtu.ipads.wtune.sql.SqlSupport.linearizeConjunction;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Joined_Kind;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Joined_On;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.AND;
import static sjtu.ipads.wtune.sql.ast.constants.UnaryOpKind.NOT;
import static sjtu.ipads.wtune.sql.util.RenumberListener.watch;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.conjunctExprTo;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.detachExpr;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.clauseLocator;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.*;

class NormalizeJoinCond {
  static void normalize(SqlNode root) {
    for (SqlNode querySpec : nodeLocator().accept(QuerySpec).bottomUp().gather(root)) {
      process(querySpec);
    }
  }

  private static void process(SqlNode querySpec) {
    final TIntList plainConds = collectPlainCondition(querySpec);
    try (final var exprs = watch(querySpec.context(), plainConds)) {
      for (SqlNode expr : exprs) {
        detachExpr(expr);
        conjunctExprTo(querySpec, QuerySpec_Where, expr);
      }
    }

    final TIntList joinConds = collectJoinCondition(querySpec);
    try (final var exprs = watch(querySpec.context(), joinConds)) {
      for (SqlNode expr : exprs) {
        final SqlNode targetJoin = locateJoinSource(expr);
        if (targetJoin != null) {
          detachExpr(expr);
          conjunctExprTo(targetJoin, Joined_On, expr);
        }
      }
    }
  }

  private static TIntList collectPlainCondition(SqlNode querySpec) {
    final SqlNodes onConditions = clauseLocator().accept(Joined_On).scoped().gather(querySpec);
    if (onConditions.isEmpty()) return new TIntArrayList(0);

    final TIntList plainConditions = new TIntArrayList(onConditions.size());
    for (SqlNode onCond : onConditions) {
      if (!onCond.parent().$(Joined_Kind).isInner()) continue;

      final List<SqlNode> terms = linearizeConjunction(onCond);
      for (SqlNode term : terms) if (isPlainCondition(term)) plainConditions.add(term.nodeId());
    }

    return plainConditions;
  }

  private static TIntList collectJoinCondition(SqlNode querySpec) {
    final SqlNode whereClause = querySpec.$(QuerySpec_Where);
    if (whereClause == null) return new TIntArrayList(0);

    return nodeLocator()
        .accept(SqlSupport::isColRefEq)
        .stopIfNot(Expr)
        .stopIf(n -> AND != n.$(Binary_Op))
        .gatherer()
        .gather(whereClause);
  }

  private static boolean isPlainCondition(SqlNode expr) {
    if (Unary.isInstance(expr))
      return expr.$(Unary_Op) == NOT && isPlainCondition(expr.$(Unary_Expr));

    if (Binary.isInstance(expr))
      return !expr.$(Binary_Op).isLogic()
          && (!ColRef.isInstance(expr.$(Binary_Left)) || !ColRef.isInstance(expr.$(Binary_Right)));

    return false;
  }

  private static SqlNode locateJoinSource(SqlNode joinCond) {
    assert isColRefEq(joinCond);

    final Attribute lhs = resolveAttribute(joinCond.$(Binary_Left));
    final Attribute rhs = resolveAttribute(joinCond.$(Binary_Right));
    final List<Relation> inputs = getEnclosingRelation(joinCond).inputs();
    for (int i = 1, bound = inputs.size(); i < bound; ++i) {
      final List<Relation> visibleInputs = inputs.subList(0, i + 1);
      if (isAttributePresent(lhs, visibleInputs) && isAttributePresent(rhs, visibleInputs)) {
        return tableSourceOf(inputs.get(i)).parent();
      }
    }

    return null;
  }

  private static boolean isAttributePresent(Attribute attr, List<Relation> relations) {
    for (Relation relation : relations) if (relation.attributes().contains(attr)) return true;
    return false;
  }
}
