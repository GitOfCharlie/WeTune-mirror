package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.Input;
import sjtu.ipads.wtune.superopt.fragment1.Join;
import sjtu.ipads.wtune.superopt.fragment1.Op;

/**
 * Rule that matches a fragment with only Join operators.
 *
 * <p>The fragment with only single Join will not be matched, because we wanna the substitution
 * "InnerJoin(x,y) <=> LeftJoin(x,y)"
 */
public class AllJoin extends BaseMatchingRule {
  @Override
  public boolean enter(Op op) {
    if (!(op instanceof Join) && !(op instanceof Input)) {
      matched = false;
      return false;
    }
    return true;
  }

  @Override
  public boolean match(Fragment g) {
    final Op head = g.root();

    if (head.type().isJoin()
        && head.predecessors()[0] instanceof Input
        && head.predecessors()[1] instanceof Input) return false;

    matched = true;
    g.acceptVisitor(this);
    return matched;
  }
}
