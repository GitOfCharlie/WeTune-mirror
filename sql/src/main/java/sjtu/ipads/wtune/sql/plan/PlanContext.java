package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.common.tree.UniformTreeContext;
import sjtu.ipads.wtune.sql.schema.Schema;

public interface PlanContext extends UniformTreeContext<PlanKind> {
  Schema schema();

  PlanNode nodeAt(int id);

  int nodeIdOf(PlanNode node);

  int bindNode(PlanNode node);

  ValuesRegistry valuesReg();

  InfoCache infoCache();

  PlanContext copy();

  default Values valuesOf(PlanNode node) {
    return valuesReg().valuesOf(nodeIdOf(node));
  }

  default PlanNode planRoot() {
    return nodeAt(root());
  }

  static PlanContext mk(Schema schema) {
    return new PlanContextImpl(16, schema);
  }
}