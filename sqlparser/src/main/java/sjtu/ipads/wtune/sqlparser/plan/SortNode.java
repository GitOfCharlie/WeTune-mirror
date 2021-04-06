package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.SortNodeImpl;

public interface SortNode extends PlanNode {
  List<ASTNode> orderKeys();

  @Override
  default OperatorType type() {
    return OperatorType.Sort;
  }

  static SortNode make(List<ASTNode> orderKeys) {
    return SortNodeImpl.build(orderKeys);
  }
}
