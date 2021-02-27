package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;

public class ToASTTranslator {
  private final Deque<Query> stack;

  private ToASTTranslator() {
    this.stack = new LinkedList<>();
  }

  public static ASTNode translate(PlanNode head) {
    final ToASTTranslator translator = new ToASTTranslator();
    translator.translate0(head);
    assert translator.stack.size() == 1;
    return translator.stack.peek().assembleAsQuery();
  }

  private void translate0(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) translate0(predecessor);

    if (node instanceof InputNode) translateInput((InputNode) node);
    else if (node instanceof JoinNode) translateJoin((JoinNode) node);
    else if (node instanceof ProjNode) translateProj((ProjNode) node);
    else if (node instanceof PlainFilterNode) translateFilter((PlainFilterNode) node);
    else if (node instanceof SubqueryFilterNode) translateFilter((SubqueryFilterNode) node);
    else if (node instanceof AggNode) translateAgg((AggNode) node);
    else if (node instanceof SortNode) translateSort((SortNode) node);
    else if (node instanceof LimitNode) translateLimit((LimitNode) node);
    else assert false;
  }

  private void translateInput(InputNode input) {
    stack.push(Query.from(input.toTableSource()));
  }

  private void translateProj(ProjNode op) {
    assert !stack.isEmpty();
    stack.peek().setProjection(op.selectItems());
  }

  private void translateFilter(FilterNode op) {
    assert !stack.isEmpty();
    stack.peek().appendFilter(op.expr(), true);
  }

  private void translateFilter(SubqueryFilterNode op) {
    final ASTNode subquery = stack.pop().assembleAsQuery();
    final ASTNode column = op.expr().get(BINARY_LEFT);

    final ASTNode queryExpr = expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, subquery);

    final ASTNode binary = expr(BINARY);
    binary.set(BINARY_OP, IN_SUBQUERY);
    binary.set(BINARY_LEFT, column);
    binary.set(BINARY_RIGHT, queryExpr);

    assert !stack.isEmpty();
    stack.peek().appendFilter(binary, true);
  }

  private void translateJoin(JoinNode op) {
    final ASTNode rightSource = stack.pop().assembleAsSource();
    final ASTNode leftSource = stack.pop().assembleAsSource();

    final ASTNode join = tableSource(JOINED_SOURCE);
    join.set(JOINED_LEFT, leftSource);
    join.set(JOINED_RIGHT, rightSource);
    join.set(JOINED_ON, op.onCondition());
    join.set(JOINED_TYPE, op instanceof InnerJoinNode ? INNER_JOIN : LEFT_JOIN);

    stack.push(Query.from(join));
  }

  private void translateAgg(AggNode agg) {
    assert !stack.isEmpty();
    final Query q = stack.peek();
    q.setGroupKeys(agg.groupKeys());
    q.setAggregation(agg.aggregations());
  }

  private void translateSort(SortNode sort) {
    assert !stack.isEmpty();
    stack.peek().setOrderKeys(sort.orderKeys());
  }

  private void translateLimit(LimitNode limit) {
    assert !stack.isEmpty();
    final Query q = stack.peek();
    q.setLimit(limit.limit());
    q.setOffset(limit.offset());
  }

  private static class Query {
    private List<ASTNode> projection;
    private ASTNode filter;
    private ASTNode source;
    private List<ASTNode> groupKeys;
    private List<ASTNode> orderKeys;
    private ASTNode limit;
    private ASTNode offset;

    private static Query from(ASTNode source) {
      final Query rel = new Query();
      rel.source = source;
      return rel;
    }

    private void appendFilter(ASTNode filterNode, boolean conjunctive) {
      if (projection == null)
        if (filter == null) filter = filterNode;
        else {
          final ASTNode newFilter = expr(ExprKind.BINARY);
          newFilter.set(BINARY_LEFT, filterNode);
          newFilter.set(BINARY_RIGHT, filter);
          newFilter.set(BINARY_OP, conjunctive ? AND : OR);
          filter = newFilter;
        }
      else {
        source = this.assembleAsSource();
        projection = null;
        filter = filterNode;
      }
    }

    private void setProjection(List<ASTNode> projection) {
      if (this.projection == null) this.projection = projection;
      else {
        this.source = this.assembleAsSource();
        this.projection = projection;
        this.filter = null;
      }
    }

    private void setAggregation(List<ASTNode> aggregation) {
      this.projection = aggregation;
    }

    private void setGroupKeys(List<ASTNode> groupKeys) {
      assert projection != null;
      this.groupKeys = groupKeys;
    }

    private void setOrderKeys(List<ASTNode> orderKeys) {
      this.orderKeys = orderKeys;
    }

    private void setLimit(ASTNode limit) {
      this.limit = limit;
    }

    public void setOffset(ASTNode offset) {
      this.offset = offset;
    }

    private ASTNode assembleAsQuery() {
      if (projection == null && filter == null && QUERY.isInstance(source)) return source;

      final ASTNode querySpec = node(QUERY_SPEC);
      querySpec.set(QUERY_SPEC_SELECT_ITEMS, selectItems());
      querySpec.set(QUERY_SPEC_FROM, source);
      if (filter != null) querySpec.set(QUERY_SPEC_WHERE, filter);
      if (!isEmpty(groupKeys)) querySpec.set(QUERY_SPEC_GROUP_BY, groupKeys);

      final ASTNode query = node(QUERY);
      query.set(QUERY_BODY, querySpec);

      if (!isEmpty(orderKeys)) query.set(QUERY_ORDER_BY, orderKeys);
      if (limit != null) query.set(QUERY_LIMIT, limit);
      if (offset != null) query.set(QUERY_OFFSET, offset);

      return query;
    }

    private ASTNode assembleAsSource() {
      if (projection == null && filter == null) return source;

      final ASTNode source = tableSource(DERIVED_SOURCE);
      source.set(DERIVED_SUBQUERY, assembleAsQuery());

      return source;
    }

    private List<ASTNode> selectItems() {
      if (projection != null) return projection;
      else {
        final ASTNode wildcard = expr(WILDCARD);

        final ASTNode item = node(SELECT_ITEM);
        item.set(SELECT_ITEM_EXPR, wildcard);

        return singletonList(item);
      }
    }
  }
}