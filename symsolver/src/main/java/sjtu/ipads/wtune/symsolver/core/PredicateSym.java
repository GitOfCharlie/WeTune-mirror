package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.PredicateSymImpl;

public interface PredicateSym extends Sym {
  static PredicateSym of(Scoped owner) {
    return PredicateSymImpl.build(owner);
  }
}