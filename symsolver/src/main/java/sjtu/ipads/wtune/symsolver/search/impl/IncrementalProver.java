package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

import java.util.HashMap;
import java.util.Map;

public class IncrementalProver extends BaseProver {
  private final Map<Decision, Proposition> trackers;

  public IncrementalProver(SmtCtx ctx, Query q0, Query q1) {
    super(ctx, q0, q1);
    this.trackers = new HashMap<>();
  }

  public static Prover build(SmtCtx ctx, Query q0, Query q1) {
    return new IncrementalProver(ctx, q0, q1);
  }

  private static Proposition[] toAssumptions(
      Decision[] decisions, Map<Decision, Proposition> trackers) {
    final Proposition[] enabledTrackers = new Proposition[decisions.length];
    for (int i = 0, bound = decisions.length; i < bound; i++)
      enabledTrackers[i] = trackers.get(decisions[i]);
    return enabledTrackers;
  }

  @Override
  protected void addAssertion(Constraint constraint, Proposition assertion) {
    super.addAssertion(constraint, assertion);
    trackers.computeIfAbsent(constraint, k -> ctx.makeTracker(k.toString()));
  }

  @Override
  public void prepare(Decision[] choices) {
    super.prepare(choices);
    smtSolver.add(targetProperties);
    for (Decision choice : choices) {
      final Proposition tracker = trackers.get(choice);
      for (Proposition assertion : assertions.get(choice))
        smtSolver.add(tracker.implies(assertion));
    }
  }

  @Override
  public Result prove() {
    final Proposition[] assumptions = toAssumptions(decisions, trackers);
    return smtSolver.checkAssumption(assumptions);
  }
}