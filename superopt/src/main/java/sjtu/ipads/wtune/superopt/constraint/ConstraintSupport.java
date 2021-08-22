package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.IgnorableException;
import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.Symbols;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class ConstraintSupport {
  public static ConstraintEnumerator mkConstraintEnumerator(
      Fragment f0, Fragment f1, LogicCtx logicCtx) {
    checkOverwhelming(f0, f1);
    final ConstraintsIndex constraints = ConstraintsIndex.mk(f0, f1);
    return new ConstraintEnumeratorImpl(f0, f1, constraints, logicCtx);
  }

  public static List<Substitution> enumConstraints(Fragment f0, Fragment f1, LogicCtx logicCtx) {
    return enumConstraints(f0, f1, logicCtx, -1);
  }

  public static List<Substitution> enumConstraints(
      Fragment f0, Fragment f1, LogicCtx logicCtx, long timeout) {
    final ConstraintEnumerator enumerator = mkConstraintEnumerator(f0, f1, logicCtx);
    enumerator.setTimeout(timeout);
    final List<Substitution> ret =
        listMap(enumerator.enumerate(), it -> Substitution.mk(f0, f1, it));
    enumerator.close();
    return ret;
  }

  private static void checkOverwhelming(Fragment f0, Fragment f1) {
    final Symbols syms0 = f0.symbols(), syms1 = f1.symbols();
    if (syms0.symbolsOf(Symbol.Kind.ATTRS).size() + syms1.symbolsOf(Symbol.Kind.ATTRS).size() >= 10)
      throw new IgnorableException("too many attrs symbols", true);
  }
}
