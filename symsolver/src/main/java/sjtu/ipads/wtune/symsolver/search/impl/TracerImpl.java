package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Summary;
import sjtu.ipads.wtune.symsolver.search.Tracer;
import sjtu.ipads.wtune.symsolver.utils.DisjointSet;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.symsolver.utils.Indexed.isCanonicalIndexed;

public class TracerImpl implements Tracer {
  private final TableSym[] tables;
  private final PickSym[] picks;

  private final List<Constraint> constraints;

  private final DisjointSet<TableSym> eqTables;
  private final DisjointSet<PickSym> eqPicks;
  private final Map<PickSym, PickSym> refs;
  private final TableSym[][] srcs;

  private final boolean useFastTableIndex;
  private final boolean useFastPickIndex;

  private int modCount;
  private boolean srcInferred; // indicate if `inferSrc` has been called since last `reset`
  private boolean isConflict;
  private boolean isIncomplete;
  private Summary summary;

  private TracerImpl(TableSym[] tables, PickSym[] picks) {
    tables = Arrays.copyOf(tables, tables.length);
    picks = Arrays.copyOf(picks, picks.length);

    this.tables = tables;
    this.picks = picks;

    constraints = new ArrayList<>();
    eqTables = DisjointSet.fromBoundedMembers(tables);
    eqPicks = DisjointSet.fromBoundedMembers(picks);
    refs = new HashMap<>();
    srcs = new TableSym[picks.length][];

    useFastTableIndex = isCanonicalIndexed(tables);
    useFastPickIndex = isCanonicalIndexed(picks);
  }

  public static Tracer build(TableSym[] tables, PickSym[] picks) {
    if (tables == null || picks == null) throw new IllegalArgumentException();
    return new TracerImpl(tables, picks);
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    addConstraint(constraint);
    eqTables.connect(tx, ty);
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    addConstraint(constraint);
    eqPicks.connect(px, py);
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, TableSym... src) {
    if (!isConsistent(src)) throw new IllegalArgumentException();

    // sort the array for the convenience of following process
    // according to doc, if `ts` is sorted beforehand, the overhead is only |ts| comparisons.
    Arrays.sort(src, Indexed::compareTo);

    // Check if there is an existing but different assignment
    final int pIdx = indexOf(p);
    final TableSym[] existing = srcs[pIdx];
    if (existing != null) isConflict = isConflict || !isMatched(existing, src);
    else {
      srcs[pIdx] = src;
      addConstraint(constraint);
    }
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    pickFrom(Constraint.pickFrom(px, tx), px, tx);
    pickFrom(Constraint.pickFrom(py, ty), py, ty);
    refs.put(px, py);
    addConstraint(constraint);
  }

  @Override
  public void decide(Decision[] decisions) {
    reset();
    for (Decision decision : decisions) decision.decide(this);
  }

  /**
   * Check if conflict condition is satisfied: <br>
   * &nbsp;&nbsp;{@code (exists px,py. isEq(px,py) && isMismatch(px.src,py.src)) ||} <br>
   * &nbsp;&nbsp;{@code (exists p. forAll ts in p.viableSources. isMismatch(p.src,ts))} <br>
   *
   * <p>Note that (px === py) => isEq(px,py). So the first branch also rules out the situation that
   * two mismatched sources are assigned to a single pick.
   *
   * <p>Note: This method MUST be invoked after all constraints being added.
   *
   * @see #pickFrom(Constraint, PickSym, TableSym...)
   * @see #isMatched(TableSym[], TableSym[])
   */
  @Override
  public boolean isConflict() {
    inferSrc();
    return isConflict;
  }

  /**
   * Check if incomplete condition is satisfied: <br>
   * &nbsp;&nbsp;{@code exists p, t. t in p.src && forAll t' in p.visibleTables. !isEq(t,t')}.
   *
   * <p>This situation happens when exists two picks px,py, they are assigned to be eq, but no
   * constraint are forced (or inferred) on their sources. e.g. <br>
   * &nbsp&nbsp;{@code q0: SELECT p0 FROM t0 JOIN t1} <br>
   * &nbsp&nbsp;{@code q1: SELECT p1 FROM t2} <br>
   * Assume we have constraint PickEq(p0,p1) but NO TableEq(t0,t2) and NO TableEq(t1,t2), then
   * PickEq(p0,p1) is actually meaningless.
   *
   * <p>Note: This method MUST be invoked after all constraints being added.
   */
  @Override
  public boolean isIncomplete() {
    inferSrc();
    return isIncomplete;
  }

  @Override
  public Summary summary() {
    if (summary != null) return summary;

    inferSrc();

    final List<Constraint> constraints = new ArrayList<>(this.constraints.size() << 1);
    for (int i = 0, bound = tables.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final TableSym tx = tables[i], ty = tables[j];
        if (isEq(tx, ty)) constraints.add(Constraint.tableEq(tx, ty));
      }

    for (int i = 0, bound = picks.length; i < bound; i++) {
      final PickSym px = picks[i];
      for (int j = i + 1; j < bound; j++) {
        final PickSym py = picks[j];
        if (isEq(px, py)) constraints.add(Constraint.pickEq(px, py));
      }

      final TableSym[] srcX = srcs[i];
      if (srcX != null) constraints.add(Constraint.pickFrom(px, srcX));

      final Constraint refConstraint = refConstraintOf(px);
      if (refConstraint != null) constraints.add(refConstraint);
    }

    return new SummaryImpl(tables, picks, constraints.toArray(Constraint[]::new), this);
  }

  int modCount() {
    return modCount;
  }

  void inflateSummary(SummaryImpl summary) {
    final int[] tGrouping = eqTables.grouping();
    final int[] pGrouping = eqPicks.grouping();

    summary.tableGroups = group(tables, tGrouping);
    summary.pickGroups = group(picks, pGrouping);

    final TableSym[][] pivotedSrcs = new TableSym[srcs.length][];
    for (int i = 0; i < srcs.length; i++)
      if (srcs[i] != null)
        pivotedSrcs[i] = arrayMap(t -> tables[tGrouping[indexOf(t)]], TableSym.class, srcs[i]);
    summary.pivotedSources = pivotedSrcs;

    summary.references = refs;
  }

  private void reset() {
    ++modCount;

    constraints.clear();
    eqTables.reset();
    eqPicks.reset();
    refs.clear();
    Arrays.fill(srcs, null);

    srcInferred = false;
    isConflict = false;
    isIncomplete = false;
    summary = null;
  }

  private boolean isEq(PickSym px, PickSym py) {
    return eqPicks.isConnected(px, py);
  }

  private boolean isEq(TableSym tx, TableSym ty) {
    return eqTables.isConnected(tx, ty);
  }

  private int indexOf(PickSym p) {
    return useFastPickIndex ? p.index() : Arrays.binarySearch(picks, p, Indexed::compareTo);
  }

  private int indexOf(TableSym t) {
    return useFastTableIndex ? t.index() : Arrays.binarySearch(tables, t, Indexed::compareTo);
  }

  private void inferSrc() {
    // infer a pick's src by the equal ones
    // forAll pick px, py
    //  if is_eq(px, py) && px.src == undefined && py.src == ts
    //   set px.src = ts
    // meanwhile, detect incomplete and conflict property
    if (srcInferred) return;

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final PickSym px = picks[i], py = picks[j];
        if (!isEq(px, py)) continue;

        TableSym[] srcX = srcs[indexOf(px)], srcY = srcs[indexOf(py)];

        if (srcX != null && srcY != null) isConflict = isConflict || !isMatched(srcX, srcY);
        else if (srcX == null && srcY == null) isIncomplete = true;
        else if (srcX != null /* && srcY == null */) tryAssignSource(py, srcX);
        else /* if (srcX == null && srcY != null) */ tryAssignSource(px, srcY);
      }

    srcInferred = true;
  }

  private void tryAssignSource(PickSym p, TableSym[] assigned) {
    final TableSym[] src = find(v -> isMatched(v, assigned), p.viableSources());

    isConflict = isConflict || src == null;
    isIncomplete = isIncomplete || (src != null && !isSufficient(p.visibleSources(), src));

    if (!isConflict && !isIncomplete) pickFrom(Constraint.pickFrom(p, src), p, src);
  }

  private void addConstraint(Constraint constraint) {
    if (!constraint.ignorable()
        && (constraints.isEmpty() || constraints.get(constraints.size() - 1) != constraint))
      constraints.add(constraint);
  }

  private boolean isConsistent(TableSym... source) {
    // tables in source must come from the same query
    if (source.length <= 1) return true;

    final Object pivot = source[0].scope();
    for (int i = 1, bound = source.length; i < bound; i++)
      if (source[i].scope() != pivot) return false;
    return true;
  }

  private boolean isMatched(TableSym[] xs, TableSym[] ys) {
    // Check if two sources are mismatched under current constraints.
    // Matched sources produce the same set of tuples, namely "matching property"
    //
    // Two sources are matched iff:
    //  1. both are of the same length
    //  2. if come from same scope (i.e. from same query), then they must be identical
    //     else, then the following condition must be satisfied:
    //      forAll x in xs, exists y in ys. isEq(x, y) &&
    //      forAll y in ys, exists x in xs. isEq(y, x)
    // Memo: Why we care about the scope?
    //  1. For the same scope, "table sources" are never equivalent, even if the tables are equal.
    //     Put it differently, they always have different "identity". e.g.
    //       q: SELECT * FROM t AS t0 JOIN t AS t1 ON t0.id = t1.child_id
    //    `t0` and `t1` are both `t`. But for q, they play different roles.
    //    Thus, in the case, sources are required to be identical to enforce matching property.
    //  2. For different scope, identity doesn't matter.
    //     Surjection is enough to enforce matching property. (Re-think: is injection necessary?)
    if (xs == ys) return true;
    if (xs.length != ys.length) return false;
    if (xs.length == 0) return true;

    return xs[0].scope() == ys[0].scope()
        ? Arrays.equals(xs, ys) // xs, ys must be sorted, see `pickFrom`.
        : (stream(xs).allMatch(tx -> stream(ys).anyMatch(ty -> isEq(tx, ty)))
            && stream(ys).allMatch(ty -> stream(xs).anyMatch(tx -> isEq(tx, ty))));
  }

  private boolean isSufficient(TableSym[] visible, TableSym[] source) {
    return stream(source).allMatch(t -> stream(visible).anyMatch(v -> isEq(v, t)));
  }

  private Constraint refConstraintOf(PickSym px) {
    final TableSym[] srcX = srcs[indexOf(px)];
    if (srcX == null || srcX.length != 1) return null;
    final PickSym py = refs.get(px);
    return py == null ? null : Constraint.reference(srcX[0], px, srcs[indexOf(py)][0], py);
  }

  private static <T> Collection<Collection<T>> group(T[] xs, int[] grouping) {
    final List<T>[] groups = new List[xs.length];
    for (int i = 0; i < grouping.length; i++) {
      final int groupId = grouping[i];

      List<T> group = groups[groupId];
      if (group == null) group = groups[groupId] = new ArrayList<>();
      group.add(xs[i]);
    }

    return stream(groups).filter(Objects::nonNull).collect(toList());
  }
}
