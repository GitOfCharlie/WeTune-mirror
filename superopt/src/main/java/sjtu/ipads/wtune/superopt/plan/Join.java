package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;

public interface Join extends PlanNode {
  Placeholder leftFields();

  Placeholder rightFields();
}
