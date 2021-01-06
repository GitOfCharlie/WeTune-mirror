package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.SmtCtx;
import sjtu.ipads.wtune.symsolver.logic.Value;

public interface Query {
  String name();

  void setName(String name);

  TableSym[] tables();

  PickSym[] picks();

  Value output(SmtCtx ctx, Value[] tuples);

  Proposition condition(SmtCtx ctx, Value[] tuples);
}
