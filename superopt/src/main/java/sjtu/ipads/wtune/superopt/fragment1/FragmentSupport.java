package sjtu.ipads.wtune.superopt.fragment1;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

public class FragmentSupport {
  public static PlanNode translateAsPlan(Fragment fragment, Constraints constraints) {
    return new PlanTranslator().translate(fragment, constraints);
  }

  public static Pair<PlanNode, PlanNode> translateAsPlan(
      Substitution substitution, boolean backwardCompatible) {
    return new PlanTranslator().translate(substitution, backwardCompatible);
  }

  public static Pair<PlanNode, PlanNode> translateAsPlan(Substitution substitution) {
    return new PlanTranslator().translate(substitution);
  }
}
