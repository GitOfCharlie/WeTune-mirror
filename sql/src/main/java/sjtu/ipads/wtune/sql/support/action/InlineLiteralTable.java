package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.support.resolution.Attribute;
import sjtu.ipads.wtune.sql.support.resolution.Relation;

import static sjtu.ipads.wtune.common.tree.TreeSupport.rootOf;
import static sjtu.ipads.wtune.common.utils.IterableSupport.all;
import static sjtu.ipads.wtune.sql.ast.ExprKind.ColRef;
import static sjtu.ipads.wtune.sql.ast.ExprKind.Literal;
import static sjtu.ipads.wtune.sql.ast.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.DerivedSource;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.JoinedSource;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.*;

class InlineLiteralTable {
  static void normalize(SqlNode node) {
    final SqlNodes literalTables = nodeLocator().accept(InlineLiteralTable::canInline).gather(node);
    for (SqlNode literalTable : literalTables) inlineLiteralTable(literalTable);
  }

  private static boolean canInline(SqlNode node) {
    return DerivedSource.isInstance(node)
        && JoinedSource.isInstance(node.parent())
        && isLiteralTable(node);
  }

  private static boolean isLiteralTable(SqlNode derived) {
    final SqlNode subquery = derived.$(Derived_Subquery);
    final SqlNode body = subquery.$(Query_Body);
    return !SetOp.isInstance(body)
        && body.$(QuerySpec_From) == null
        && all(body.$(QuerySpec_SelectItems), it -> Literal.isInstance(it.$(SelectItem_Expr)));
  }

  private static void inlineLiteralTable(SqlNode table) {
    final SqlContext ctx = table.context();
    inlineExprs(SqlNode.mk(ctx, rootOf(ctx, table.nodeId())), table);
    reduceTable(table);
  }

  private static void inlineExprs(SqlNode rootQuery, SqlNode tableSource) {
    assert DerivedSource.isInstance(tableSource);
    final Relation targetRelation = getEnclosingRelation(tableSource.$(Derived_Subquery));
    final SqlNodes colRefs = nodeLocator().accept(ColRef).gather(rootQuery);
    final SqlContext ctx = rootQuery.context();

    for (SqlNode colRef : colRefs) {
      final Attribute attr = resolveAttribute(colRef);
      if (attr == null) continue;

      final Attribute baseRef = traceRef(attr);
      if (baseRef == null || baseRef.owner() != targetRelation) continue;
      assert Literal.isInstance(baseRef.expr());

      // If the expr is an ORDER BY item then just remove it.
      // Consider "SELECT .. FROM (SELECT 1 AS o) t ORDER BY t.o"
      // "t.o" shouldn't be replaced as "1" because "ORDER BY 1"
      // means "order by the 1st output column".
      // It can be just removed since constant value won't affect
      // the ordering
      final SqlNode parent = colRef.parent();

      if (OrderItem.isInstance(parent)) {
        final SqlNode q = parent.parent();
        ctx.detachNode(parent.nodeId());
        if (Query.isInstance(q) && q.$(Query_OrderBy).isEmpty()) q.remove(Query_OrderBy);

      } else {
        final SqlNode copied = SqlSupport.copyAst(baseRef.expr()).go();
        ctx.displaceNode(colRef.nodeId(), copied.nodeId());
      }
    }
  }

  private static void reduceTable(SqlNode deriveTableSource) {
    final SqlContext ctx = deriveTableSource.context();
    final SqlNode body = getEnclosingRelation(deriveTableSource).rootNode().$(Query_Body);
    final SqlNode joinNode = deriveTableSource.parent();
    final SqlNode lhs = joinNode.$(Joined_Left);
    final SqlNode rhs = joinNode.$(Joined_Right);
    final SqlNode cond = joinNode.$(Joined_On);
    if (lhs == deriveTableSource) ctx.displaceNode(joinNode.nodeId(), rhs.nodeId());
    else ctx.displaceNode(joinNode.nodeId(), lhs.nodeId());
    assert QuerySpec.isInstance(body);
    NormalizationSupport.conjunctExprTo(body, QuerySpec_Where, cond);
  }
}