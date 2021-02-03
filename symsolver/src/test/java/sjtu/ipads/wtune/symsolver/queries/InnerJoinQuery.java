package sjtu.ipads.wtune.symsolver.queries;

import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.supplier;

public class InnerJoinQuery extends BaseQueryBuilder {
  @Override
  protected Function<Value, Proposition> semantic() {
    final TableSym[] tables = supplier(this::makeTable).repeat(2).toArray(TableSym[]::new);
    final PickSym[] picks = supplier(this::makePick).repeat(3).toArray(PickSym[]::new);

    final TableSym t0 = tables[0];
    final TableSym t1 = tables[1];
    final PickSym p0 = picks[0];
    final PickSym p1 = picks[1];
    final PickSym p2 = picks[2];

    p0.setVisibleSources(asArray(t0, t1));
    p1.setVisibleSources(asArray(t0));
    p2.setVisibleSources(asArray(t1));
    p0.setViableSources(powerSet(Set.of(tables)));
    p1.setViableSources(singleton(singleton(t0)));
    p2.setViableSources(singleton(singleton(t1)));

    p1.setJoined(p2);

    final Value a = makeTuple(), b = makeTuple();
    final Proposition from = ctx().tupleFrom(a, t0).and(ctx().tupleFrom(b, t1));
    final Proposition join = p1.apply(a).equalsTo(p2.apply(b));

    return x -> ctx().makeExists(asArray(a, b), x.equalsTo(p0.apply(a, b)).and(from).and(join));
  }
}
