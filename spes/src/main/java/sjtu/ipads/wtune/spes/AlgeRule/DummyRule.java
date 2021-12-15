package sjtu.ipads.wtune.spes.AlgeRule;

import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;

public class DummyRule extends AlgeRuleBase {
  public DummyRule(AlgeNode node) {
    this.input = node;
  }
  ;

  @Override
  public boolean preCondition() {
    return false;
  }

  @Override
  public AlgeNode transformation() {
    return this.input;
  }
}
