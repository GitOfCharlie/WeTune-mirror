package sjtu.ipads.wtune.symsolver.logic;

import sjtu.ipads.wtune.symsolver.logic.impl.PropositionImpl;

public interface Proposition extends Value {
  static Proposition wrap(LogicCtx ctx, Object underlying) {
    return PropositionImpl.build(ctx, underlying);
  }

  Proposition not();

  Proposition implies(Proposition other);

  Proposition and(Proposition other);

  Proposition or(Proposition other);
}
