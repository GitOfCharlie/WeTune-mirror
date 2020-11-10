package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

/**
 * Replace predicate by a peer.
 *
 * <p>Requirements:
 *
 * <ol>
 *   <li>the original one and the peer should be both primitive predicate
 *   <li>they should contain columns of the same number
 *   <li>they shouldn't involves a subquery
 * </ol>
 */
public class ReplacePredicate implements Operator {
  private final SQLNode target;
  private final SQLNode replacement;

  private ReplacePredicate(SQLNode target, SQLNode replacement) {
    this.target = target;
    this.replacement = replacement;
  }

  public static ReplacePredicate build(SQLNode target, SQLNode replacement) {
    // we use assertion here because it should be checked in Mutation class
    assert target != null && replacement != null;
    final BoolExpr boolExpr0 = target.get(BOOL_EXPR);
    final BoolExpr boolExpr1 = replacement.get(BOOL_EXPR);
    assert boolExpr0 != null && boolExpr1 != null;
    assert boolExpr0.isPrimitive() && boolExpr1.isPrimitive();

    return new ReplacePredicate(target, replacement);
  }

  @Override
  public SQLNode apply(SQLNode root) {
    // inherit param index
    final List<SQLNode> targetParams = collectParams(target);
    final List<SQLNode> repParams = collectParams(replacement);
    for (int i = 0, bound = Math.min(repParams.size(), targetParams.size()); i < bound; i++)
      repParams.get(i).put(PARAM_INDEX, targetParams.get(i).get(PARAM_INDEX));

    Operator.replaceNode(target, replacement);

    return root;
  }

  private static List<SQLNode> collectParams(SQLNode node) {
    final CollectParam collector = new CollectParam();
    node.accept(collector);
    return collector.params;
  }

  private static class CollectParam implements SQLVisitor {
    private final List<SQLNode> params = new ArrayList<>();

    @Override
    public boolean enterLiteral(SQLNode literal) {
      if (literal.get(PARAM_INDEX) != null) params.add(literal);
      return false;
    }

    @Override
    public boolean enterParamMarker(SQLNode paramMarker) {
      if (paramMarker.get(PARAM_INDEX) != null) params.add(paramMarker);
      return false;
    }
  }
}