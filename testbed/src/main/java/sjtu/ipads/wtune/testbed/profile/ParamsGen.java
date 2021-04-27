package sjtu.ipads.wtune.testbed.profile;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.func;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.resolver.JoinGraph;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.Params;
import sjtu.ipads.wtune.testbed.population.Generators;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;

public interface ParamsGen {
  Object NOT_NULL = new Object();
  Object IS_NULL = new Object();

  Params params();

  Generators generators();

  /** Generated values of parameters by last call of `generateAll`. */
  Map<ParamDesc, Object> values();

  List<Relation> pivotRelations();

  /** Designate pivot tables, i.e., the tables that the pivot seed is applied to. */
  void setPivotTables(List<Relation> pivotRelations);

  /**
   * Set the pivot seed and updates the seed of every tables accordingly.
   *
   * <p>Returns false if the seed of any table cannot be derived.
   *
   * <p>Example: {@code a JOIN b ON a.id = b.ref}.
   *
   * <p>Assume `a`'s seed is set to 10, i.e., `a.id` is 10. However, no row in `b` has `ref` equals
   * to 10. In this case, this method returns false.
   */
  boolean setPivotSeed(int seed);

  /**
   * Generate values for all parameters, which can be retrieved by calling {@link #values()}.
   *
   * <p>Returns false if any parameter cannot be generated.
   */
  boolean generateAll();

  /**
   * Get the current seed of `relation`.
   *
   * <p>The seed is updated upon `setPivotSeed` being called. Thus, calling this method before any
   * `setPivotSeed` is meaningless, and returns -1.
   */
  int seedOf(Relation relation);

  static ParamsGen make(Params params, Generators generators) {
    return new ParamsGenImpl(params, generators);
  }

  static int setEligibleSeed(ParamsGen gen, int seed0) {
    if (seed0 < 0) throw new IllegalArgumentException("seed should be greater than 0");

    final int limit = seedLimitOf(gen);
    seed0 = seed0 % limit;

    int i = seed0;
    do {
      if (gen.setPivotSeed(i)) return i;
      ++i;
      if (i >= limit) i = i % limit;
    } while (i != seed0);

    return -1;
  }

  /**
   * Align tables between two parameters generator.
   *
   * <p>This is achieved by finding and setting aligned pivots tables. Thus, reset the pivot tables
   * after invoking this method will break the alignment.
   */
  static void alignTables(ParamsGen gen0, ParamsGen gen1) {
    final Params params0 = gen0.params(), params1 = gen1.params();
    final var pivotTables = ParamsGen.alignTables(params0, params1);
    gen0.setPivotTables(pivotTables.getLeft());
    gen1.setPivotTables(pivotTables.getRight());
  }

  private static Pair<List<Relation>, List<Relation>> alignTables(Params params0, Params params1) {
    final JoinGraph joins0 = params0.joinGraph();
    final JoinGraph joins1 = params1.joinGraph();

    final List<Set<Relation>> scc0 = joins0.getSCC();
    final List<Set<Relation>> scc1 = joins1.getSCC();

    final List<Relation> pivot0 = new ArrayList<>(scc0.size());
    final List<Relation> pivot1 = new ArrayList<>(scc1.size());

    for (Set<Relation> component0 : scc0) {
      final Set<Relation> component1 = findMatchSCC(component0, scc1);
      if (component1 == null) continue;

      for (Relation t0 : component0) {
        final Relation t1 = findMatchRelation(t0, component1);
        if (t1 == null) continue;
        pivot0.add(t0);
        pivot1.add(t1);
        break;
      }
    }

    handleOrphanSCC(scc0, pivot0);
    handleOrphanSCC(scc1, pivot1);

    return Pair.of(pivot0, pivot1);
  }

  private static Set<Relation> findMatchSCC(Set<Relation> component0, List<Set<Relation>> scc1) {
    if (scc1.size() == 1) return scc1.get(0);

    for (Set<Relation> component1 : scc1)
      if (all(t0 -> any(t1 -> t0.table().equals(t1.table()), component0), component1)) {
        return component1;
      }

    return null;
  }

  private static Relation findMatchRelation(Relation rel0, Set<Relation> component1) {
    // actually relation is not rigorously comparable. this is sloppy
    for (Relation rel1 : component1) if (rel1.table().equals(rel0.table())) return rel1;
    return null;
  }

  private static void handleOrphanSCC(List<Set<Relation>> scc, List<Relation> pivots) {
    outer:
    for (Set<Relation> component : scc) {
      if (scc.size() == pivots.size()) return;
      for (Relation pivot : pivots) if (component.contains(pivot)) continue outer;
      pivots.add(Iterables.get(component, 0));
    }
  }

  private static int seedLimitOf(ParamsGen gen) {
    final PopulationConfig config = gen.generators().config();
    final List<Relation> pivots = gen.pivotRelations();
    if (pivots.size() == 1) return config.getUnitCount(pivots.get(0).table().name());
    else
      return pivots.stream()
          .map(func(Relation::table).andThen(Table::name))
          .mapToInt(config::getUnitCount)
          .min()
          .orElseThrow();
  }
}
