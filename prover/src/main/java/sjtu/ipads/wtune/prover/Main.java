package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.sqlparser.plan1.PlanBuilder;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class Main {
  public static void main(String[] args) {
    final String latch = "";
    boolean start = latch.isEmpty();

    for (Statement rewritten : Statement.findAllRewritten()) {
      if (latch.equals(rewritten.toString())) start = true;
      if (!start) continue;

      final Statement original = rewritten.original();
      System.out.println(original);

      final Schema schema = original.app().schema("base");
      try {
        final PlanNode plan0 = PlanBuilder.buildPlan(original.parsed(), schema);
        final PlanNode plan1 = PlanBuilder.buildPlan(rewritten.parsed(), schema);
        PlanNode.resolveRefs(plan0);
        PlanNode.resolveRefs(plan1);
//        System.out.println(plan0);
//        System.out.println(plan1);
      } catch (Throwable ex) {
        System.out.println(original);
        throw ex;
      }
    }
  }
}
