package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

interface ExistsFilter extends FilterNode {

  // Expression is the subquery expression.
  // e.g., exists (select 1 from x), RHS expr is (select 1 from x)
  // Since the subquery may depends on outer query's attributes (dependent subquery),
  // the expression will be set during ref-binding.
  // The refs will be updated accordingly.
  // After that, this field is immutable.
  void setExpr(Expr expr);

  @Override
  default OperatorType type() {
    return OperatorType.ExistsFilter;
  }
}
