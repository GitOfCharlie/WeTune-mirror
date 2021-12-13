package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.common.utils.PartialOrder;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.logic.LogicSupport;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.ArraySupport.compareBools;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.common.utils.PartialOrder.*;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.*;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;
import static sjtu.ipads.wtune.superopt.logic.LogicSupport.*;
import static sjtu.ipads.wtune.superopt.uexpr.UExprSupport.translateToUExpr;

class ConstraintEnumerator {
  /*
   * Please also refer the WeTune paper's section 4.2.
   *
   * + Preconditions & Instantiations
   * Constraint are separated into two parts: preconditions and instantiations.
   * - Preconditions
   *   Constraints only regarding the source side's symbols.
   *   Together with the structure of template, they are used to match a pattern in a
   *   concrete plan during optimization.
   * - Instantiations
   *   Eq-constraints that assign the target side's symbols from the source side.
   *   After preconditions are satisfied, assign concrete values to target side's symbols
   *   to construct a replacement of the matched pattern in a concrete plan.
   *
   * + Validness
   *   This section describes whether a constraint set is valid.
   *   "must ..." in the section means that "a valid constraint set are required to ..."
   * - AttrsSub
   *   For each source-side Attrs `a`, there must be one and only one `AttrsSub(a,s)`,
   *   where `s` is a viable source according to the structure of plan template.
   * - Table Instantiation
   *   Each Table symbol must be exclusively instantiated.
   *   e.g., if there is a target-side Table `t1` being instantiated from a source-side Table `t0`,
   *   then another target-side Table `t2` are not allowed to be instantiated from `t0` too.
   * - Attrs Instantiation
   *   Each Attrs must be instantiated by a symbol whose source is viable in the target-side structure.
   *   e.g. Proj<a0>(InnerJoin<k0,k1>(t0,t1)) vs. Proj<a1>(t0), AttrsSub(k1,t1). Then `a1` must not be
   *   instantiated from `k1` since there are no `t1` at the target side.
   * - Predicate Instantiation: Always valid.
   * - TableEq: Always valid.
   * - AttrsEq
   *   An AttrsEq(a1,a2) is valid only if \exists t1,t2. AttrsSub(a1,t1) /\ AttrsSub(a2,t2) /\ TableEq(t1,t2).
   * - PredicateEq: Always valid.
   * - Unique/NotNull
   *   A Unique/NotNull(t,a) is valid only if `AttrsSub(a,t)`
   * - Reference
   *   A Reference(t1,a1,t2,a2) is valid only if `AttrsSub(a1,t1) /\ AttrsSub(a2,t2)`
   *
   * + Closure
   *   Given a (valid) constraints set `C`, we denote its closure under implication by `C+`.
   *   i.e. if a constraint `c` satisfies `C` => `c`, then `c` \in `C+`.
   *   The rule of `=>` is listed as follows.
   * - AttrsSub: N/A
   * - Instantiation: N/A
   * - TableEq/AttrsEq/PredicateEq: by the transitivity of "="
   * - Unique/NotNull
   *   TableEq(t1,t2) /\ AttrsEq(a1,a2) /\ Unique/NotNull(t1,a1) /\ AttrsSub(a2,t2) => Unique/NotNull(t2,a2)
   * - Reference: similar to Unique/NotNull.
   *
   * + Stages
   * The enumeration are divided into stages. Each stage enumerates a subset of constraints
   * and recursively invokes the next stage. Validness are enforced during enumeration. The output
   * of an iteration is just the closure.
   * There are also "breaker" stages that breaks the current iteration and skips to next iteration,
   * if the determined parts must lead to non-equivalence. The stages are sequenced as follows:
   *     AttrsSub -> Instantiation -> MismatchedOutputBreaker ->
   *     Eq -> Unique -> MismatchedSummationBreaker ->
   *     NotNull -> Reference
   * - AttrsSub
   *   For each source-side Attrs symbol "a", determine the "x" in AttrsSub(a,x).
   *   We call "x" is the "source" of "a"
   * - Instantiation
   *   For each target-side symbols "s", determine it should be assigned from which source-side symbol.
   * - MismatchedOutputBreaker
   *   The output schema of the two plan templates is decided now. Fast reject if they are mismatched.
   *   e.g. Join(t1,t2) can never be equivalent to Proj(t1).
   * - Eq
   *   For each kind of source-side symbols, enumerate the equivalence class partitions.
   *   e.g., for 3 Table symbols {t1,t2,t3}, the partitions {{t1,t2,t3}}, {{t1,t2},t3}, ...
   *   are enumerated, which corresponds to t1=t2 /\ t1=t3 /\ t2=t3, t1=t2, ... respectively.
   * - Unique
   *   For each Attrs symbol, determine whether it is unique key.
   * - MismatchedSummationBreaker
   *   The summation variable of two plans' U-exprs are decided now. Fast reject if either side is not
   *   the subset of another (i.e. theorem 5.1 and 5.2 are neither applicable)
   *   e.g. Sum{x}(T(x)) vs. Sum{y}(|Sum{x}(T(x) * [y = a(x)])|)
   *     Why this breaker is here? i.e, why the summation vars are decided here? Basically, they can be
   *     decided earlier (right after instantiation stage). However, some essential tricks in U-expr
   *     translation delay the timing until determining Unique. See UExprTranslator for details.
   * - NotNull
   *   For each Attrs symbol, determine whether it is NOT NULL.
   * - Reference
   *   For each join key pair, determine whether there are foreign key between them.
   *
   * + Prune
   * Pruning is essentially summarized as two conditions:
   *   ((C => C') /\ (C => q != q')) => (C' => q != q')
   *   ((C => C') /\ (C' => q = q)) => (C => q = q')
   * Thus, the verification result and its correspondent constraint C are remembered during enumeration.
   * The following enumeration can leverage this information to avoid redundant SMT invocation.
   */
  private static final int FREE = 0, MUST_ENABLE = 1, MUST_DISABLE = 2, CONFLICT = 3;
  private static final int TIMEOUT = Integer.MAX_VALUE;
  private final ConstraintsIndex I;
  private final long timeout;
  private final boolean[] enabled;
  private final List<boolean[]> knownEqs, knownNeqs;
  private final EnumerationStage[] stages;
  private final int tweak;

  ConstraintEnumerator(ConstraintsIndex I, long timeout) {
    this(I, timeout, 0);
  }

  ConstraintEnumerator(ConstraintsIndex I, long timeout, int tweak) {
    this.I = I;
    this.timeout = timeout < 0 ? Long.MAX_VALUE : timeout;
    this.enabled = new boolean[I.size()];
    this.knownEqs = new LinkedList<>();
    this.knownNeqs = new LinkedList<>();
    this.stages = mkStages();
    this.tweak = tweak;
    Arrays.fill(enabled, false);
  }

  List<Substitution> enumerate() {
    stages[0].enumerate();
    return ListSupport.map(knownEqs, I::mkRule);
  }

  //// initialization ////

  private EnumerationStage[] mkStages() {
    final boolean disable0 = (tweak & ENUM_FLAG_DISABLE_BREAKER_0) != 0;
    final boolean disable1 = (tweak & ENUM_FLAG_DISABLE_BREAKER_1) != 0;
    final boolean dryRun = disable0 || disable1 || (tweak & ENUM_FLAG_DRY_RUN) != 0;

    final EnumerationStage sourceEnum = new AttrsSourceEnumerator();
    final EnumerationStage tableInstantiation = new InstantiationEnumerator(TABLE);
    final EnumerationStage attrsInstantiation = new InstantiationEnumerator(ATTRS);
    final EnumerationStage predInstantiation = new InstantiationEnumerator(PRED);
    final EnumerationStage mismatchedOutputBreaker = new MismatchedOutputBreaker(disable0);
    final EnumerationStage tableEnum = new PartitionEnumerator(TABLE);
    final EnumerationStage attrsEnum = new PartitionEnumerator(ATTRS);
    final EnumerationStage predEnum = new PartitionEnumerator(PRED);
    final EnumerationStage uniqueEnum = new BinaryEnumerator(Unique);
    final EnumerationStage notNullEnum = new BinaryEnumerator(NotNull);
    final EnumerationStage refEnum = new BinaryEnumerator(Reference);
    final EnumerationStage mismatchedSummationBreaker = new MismatchedSummationBreaker(disable1);
    final EnumerationStage timeout = new TimeoutBreaker(System.currentTimeMillis(), this.timeout);
    final EnumerationStage verifier = new Verifier(dryRun);

    final EnumerationStage[] stages =
        new EnumerationStage[] {
          sourceEnum,
          tableInstantiation,
          attrsInstantiation,
          predInstantiation,
          mismatchedOutputBreaker,
          tableEnum,
          attrsEnum,
          predEnum,
          uniqueEnum,
          mismatchedSummationBreaker,
          notNullEnum,
          refEnum,
          timeout,
          verifier
        };

    for (int i = 0, bound = stages.length - 1; i < bound; ++i)
      stages[i].setNextStage(stages[i + 1]);
    return stages;
  }

  //// inspection of current state ////

  private Symbol currentSourceOf(Symbol attrsSym) {
    final int begin = I.beginIndexOfKind(AttrsSub);
    final int end = I.endIndexOfKind(AttrsSub);
    for (int i = begin; i < end; ++i) {
      final Constraint c = I.get(i);
      if (currentIsEnabled(i) && c.symbols()[0] == attrsSym) return c.symbols()[1];
    }
    return null;
  }

  private Symbol currentInstantiationOf(Symbol sym) {
    assert sym.ctx() == I.targetSymbols();

    final Symbol.Kind kind = sym.kind();
    final int begin = I.beginIndexOfInstantiation(kind);
    final int end = I.endIndexOfInstantiation(kind);
    for (int i = begin; i < end; ++i) {
      final Constraint instantiation = I.get(i);
      if (currentIsEnabled(i) && instantiation.symbols()[1] == sym)
        return instantiation.symbols()[0];
    }

    return null;
  }

  private boolean currentIsEq(Symbol sym0, Symbol sym1) {
    return sym0 == sym1
        || (sym0.kind() == sym1.kind() && currentIsEnabled(I.indexOfEq(sym0, sym1)));
  }

  private boolean currentIsEnabled(int index) {
    return enabled[index];
  }

  //// verification cache ////

  private void rememberEq(boolean[] enabled) {
    knownEqs.removeIf(it -> compareBools(it, enabled) == GREATER_THAN);
    knownEqs.add(enabled);
  }

  private void rememberNeq(boolean[] enabled) {
    knownNeqs.removeIf(it -> compareBools(it, enabled) == LESS_THAN);
    knownNeqs.add(enabled);
  }

  //// rules about force-enabled/disable ////

  private int checkForced(int index) {
    final Constraint.Kind kind = I.get(index).kind();
    switch (kind) {
      case AttrsSub:
        return checkAttrsSubForced(index);
      case TableEq:
        return checkTableEqForced(index);
      case AttrsEq:
        return checkAttrsEqForced(index);
      case PredicateEq:
        return checkPredEqForced(index);
      case NotNull:
        return checkNotNullForced(index);
      case Unique:
        return checkUniqueForced(index);
      case Reference:
        return checkReferenceForced(index);
      default:
        throw new IllegalArgumentException("unknown constraint kind" + kind);
    }
  }

  private int checkAttrsSubForced(int index) {
    final Constraint attrsSub = I.get(index);
    final Symbol attrsSym = attrsSub.symbols()[0];
    if (I.viableSourcesOf(attrsSym).size() == 1) return MUST_ENABLE;

    final Symbol sourceSym = attrsSub.symbols()[1];
    final Symbol currentSource = currentSourceOf(attrsSym);
    if (currentSource != null && currentSource != sourceSym) return MUST_DISABLE;

    return FREE;
  }

  private int checkTableEqForced(int index) {
    return FREE;
  }

  private int checkAttrsEqForced(int index) {
    final Constraint attrsEq = I.get(index);
    final Symbol attrs0 = attrsEq.symbols()[0], attrs1 = attrsEq.symbols()[1];
    final Symbol source0 = currentSourceOf(attrs0), source1 = currentSourceOf(attrs1);
    return currentIsEq(attrs0, attrs1) && !currentIsEq(source0, source1) ? MUST_DISABLE : FREE;
  }

  private int checkPredEqForced(int index) {
    return FREE;
  }

  private int checkNotNullForced(int index) {
    return checkSourceConformity(index) | checkImplication(index);
  }

  private int checkUniqueForced(int index) {
    return checkSourceConformity(index) | checkImplication(index);
  }

  private int checkReferenceForced(int index) {
    final Constraint reference = I.get(index);
    final Symbol attrs0 = reference.symbols()[1], attrs1 = reference.symbols()[3];
    final int conformity = checkSourceConformity(index);
    final int implication = checkImplication(index);
    final int reflexivity = currentIsEq(attrs0, attrs1) ? MUST_ENABLE : FREE;
    return conformity | implication | reflexivity;
  }

  private int checkImplication(int index) {
    final Constraint c = I.get(index);
    final Symbol[] syms = c.symbols();
    final Symbol attrs0 = syms[1], attrs1 = syms.length > 2 ? syms[3] : null;
    final Constraint.Kind kind = c.kind();

    for (int i = I.beginIndexOfKind(kind); i < index; ++i) {
      final Constraint other = I.get(i);
      if (currentIsEq(attrs0, other.symbols()[1])
          && (attrs1 == null || currentIsEq(attrs1, other.symbols()[3]))) {
        return currentIsEnabled(i) ? MUST_ENABLE : MUST_DISABLE;
      }
    }

    return FREE;
  }

  private int checkSourceConformity(int index) {
    final Constraint constraint = I.get(index);
    final Constraint.Kind kind = constraint.kind();
    assert kind.isIntegrityConstraint();
    final Symbol[] syms = constraint.symbols();
    final Symbol source0 = syms[0], attrs0 = syms[1];
    if (currentSourceOf(attrs0) != source0) return MUST_DISABLE;
    if (syms.length > 2) {
      final Symbol source1 = syms[2], attrs1 = syms[3];
      if (currentSourceOf(attrs1) != source1) return MUST_DISABLE;
    }
    return FREE;
  }

  //// rules about instantiation ////

  private boolean validateInstantiation(Symbol from, Symbol to) {
    assert from.kind() == to.kind();
    final Symbol.Kind kind = from.kind();
    switch (kind) {
      case TABLE:
        return validateTableInstantiation(from, to);
      case ATTRS:
        return validateAttrsInstantiation(from, to);
      case PRED:
        return validatePredInstantiation(from, to);
      default:
        throw new IllegalArgumentException("unknown symbol kind " + kind);
    }
  }

  private boolean validateTableInstantiation(Symbol from, Symbol to) {
    // instantiation of TABLE symbol is required exclusive.
    // i.e., if a different symbol has been instantiated from `from`,
    // then instantiation from `from` to `to` is illegal.
    final int begin = I.beginIndexOfInstantiation(TABLE);
    final int end = I.endIndexOfInstantiation(TABLE);
    for (int i = begin; i < end; ++i) {
      final Constraint other = I.get(i);
      if (other.symbols()[0] == from && other.symbols()[1] != to) return false;
    }
    return true;
  }

  private boolean validateAttrsInstantiation(Symbol from, Symbol to) {
    Symbol source = currentSourceOf(from);
    assert source != null;

    final List<Symbol> sourceChain = new ArrayList<>(4);
    while (source != null) {
      sourceChain.add(source);
      source = currentSourceOf(source);
    }

    for (Symbol sourceOfTo : I.viableSourcesOf(to)) {
      final Symbol sourceInstantiation = currentInstantiationOf(sourceOfTo);
      assert sourceInstantiation != null;
      if (sourceChain.contains(sourceInstantiation)) return true;
    }
    return false;
  }

  private boolean validatePredInstantiation(Symbol from, Symbol to) {
    return true;
  }

  //// Enumeration Stages ////

  private interface EnumerationStage {
    int enumerate();

    void setNextStage(EnumerationStage nextStage);
  }

  private abstract static class AbstractEnumerationStage implements EnumerationStage {
    private EnumerationStage nextStage;

    @Override
    public void setNextStage(EnumerationStage nextStage) {
      this.nextStage = nextStage;
    }

    protected EnumerationStage nextStage() {
      return nextStage;
    }
  }

  // AttrsSub
  private class AttrsSourceEnumerator extends AbstractEnumerationStage {
    private final int begin, end;
    private final List<Symbol> attrs;
    private final List<int[]> sourceChoices;

    private AttrsSourceEnumerator() {
      this.begin = I.beginIndexOfKind(AttrsSub);
      this.end = I.endIndexOfKind(AttrsSub);

      final List<Symbol> allAttrs = I.sourceSymbols().symbolsOf(ATTRS);
      this.attrs = new ArrayList<>(allAttrs.size());
      this.sourceChoices = new ArrayList<>(allAttrs.size());

      for (Symbol attr : allAttrs) {
        final Collection<Symbol> sources = I.viableSourcesOf(attr);
        if (sources.size() == 1) continue;

        assert sources.size() > 1;
        final int[] constraintIndices = new int[sources.size()];
        int i = 0;
        for (Symbol source : sources) constraintIndices[i++] = indexOfAttrsSub(attr, source);

        attrs.add(attr);
        sourceChoices.add(constraintIndices);
      }
    }

    @Override
    public int enumerate() {
      Arrays.fill(enabled, begin, end, true);
      return enumerate0(0);
    }

    private int enumerate0(int symIndex) {
      if (symIndex >= attrs.size()) return nextStage().enumerate();

      final int[] sources = sourceChoices.get(symIndex);
      for (int source : sources) enabled[source] = false;
      for (int source : sources) {
        enabled[source] = true;
        final int answer = enumerate0(symIndex + 1);
        enabled[source] = false;

        if (answer == TIMEOUT) return TIMEOUT;
      }

      return EQ; // doesn't matter
    }

    private int indexOfAttrsSub(Symbol attrs, Symbol source) {
      for (int i = begin; i < end; ++i) {
        final Constraint attrsSub = I.get(i);
        if (attrsSub.symbols()[0] == attrs && attrsSub.symbols()[1] == source) return i;
      }
      assert false;
      return -1;
    }
  }

  // instantiations
  private class InstantiationEnumerator extends AbstractEnumerationStage {
    private final Symbol.Kind kind;
    private final List<Symbol> sourceSyms;
    private final List<Symbol> targetSyms;

    private InstantiationEnumerator(Symbol.Kind kind) {
      this.kind = kind;
      this.sourceSyms = I.sourceSymbols().symbolsOf(kind);
      this.targetSyms = I.targetSymbols().symbolsOf(kind);
    }

    @Override
    public int enumerate() {
      return enumerate0(0);
    }

    private int enumerate0(int symIndex) {
      if (symIndex >= targetSyms.size()) return nextStage().enumerate();

      boolean allNeq = true;
      final Symbol targetSym = targetSyms.get(symIndex);
      for (Symbol sourceSym : sourceSyms) {
        if (validateInstantiation(sourceSym, targetSym)) {
          final int index = I.indexOfInstantiation(sourceSym, targetSym);

          enabled[index] = true;
          final int answer = enumerate0(symIndex + 1);
          enabled[index] = false;

          if (answer == TIMEOUT) return TIMEOUT;
          if (answer != NEQ) allNeq = false;
        }
      }

      return allNeq ? NEQ : EQ; // The return value of the 2nd branch does not matter.
    }
  }

  // TableEq, AttrsEq, PredEq
  private class PartitionEnumerator extends AbstractEnumerationStage {
    private final Symbol.Kind kind;
    private final List<Symbol> syms;
    private final Partitioner partitioner;
    private final int beginIndex, endIndex;
    private final List<boolean[]> knownNeqs;
    private final boolean[] buffer;

    private PartitionEnumerator(Symbol.Kind kind) {
      this.kind = kind;
      this.syms = I.sourceSymbols().symbolsOf(kind);
      this.partitioner = new Partitioner((byte) syms.size());
      this.beginIndex = I.beginIndexOfEq(kind);
      this.endIndex = I.endIndexOfEq(kind);
      this.knownNeqs = new LinkedList<>();
      this.buffer = new boolean[(syms.size() * (syms.size() - 1)) >> 1];
    }

    @Override
    public int enumerate() {
      if (syms.isEmpty()) return nextStage().enumerate();

      partitioner.reset();
      resetConstraints();
      boolean alwaysNeq = true;

      do {
        final byte[][] partitions = partitioner.partition();
        setupConstraints(partitions);

        // Only AttrsEq may conflict.
        // Guarantee: if a set of AttrsEq (denoted by Eq_a) are not conflict under a set of TableEq
        // (denoted as Eq_t), then under any stronger Eq_t' than Eq_t, Eq_a won't conflict.
        if (isAttrsConflict()) continue;
        if (isWeakerThanNeq()) continue;

        final int answer = nextStage().enumerate();
        resetConstraints();

        if (answer == TIMEOUT) return TIMEOUT;
        if (answer != NEQ) alwaysNeq = false;
        else rememberNeq();

      } while (partitioner.forward());

      return alwaysNeq ? NEQ : EQ;
    }

    private void setupConstraints(byte[][] partitions) {
      for (byte[] partition : partitions) {
        for (int i = 0, bound = partition.length; i < bound - 1; ++i) {
          for (int j = i + 1; j < bound; ++j) {
            final int index = I.indexOfEq(syms.get(partition[i]), syms.get(partition[j]));
            enabled[index] = true;
          }
        }
      }
    }

    private void resetConstraints() {
      Arrays.fill(enabled, beginIndex, endIndex, false);
    }

    private boolean[] fragment() {
      System.arraycopy(enabled, beginIndex, buffer, 0, buffer.length);
      return buffer;
    }

    private void rememberNeq() {
      final boolean[] neqs = fragment();
      knownNeqs.removeIf(it -> compareBools(neqs, it) == LESS_THAN);
      knownNeqs.add(Arrays.copyOf(neqs, neqs.length));
    }

    private boolean isAttrsConflict() {
      if (kind != ATTRS) return false;
      for (int i = beginIndex, bound = endIndex; i < bound; ++i)
        if (checkForced(i) == CONFLICT) return true;
      return false;
    }

    private boolean isWeakerThanNeq() {
      final boolean[] current = fragment();
      return any(knownNeqs, it -> compareBools(current, it) == LESS_THAN);
    }
  }

  // NotNull, Unique, Reference
  private class BinaryEnumerator extends AbstractEnumerationStage {
    private final Constraint.Kind kind;
    private final int beginIndex, endIndex;

    private BinaryEnumerator(Constraint.Kind kind) {
      assert kind.isIntegrityConstraint();
      this.kind = kind;
      this.beginIndex = I.beginIndexOfKind(kind);
      this.endIndex = I.endIndexOfKind(kind);
    }

    @Override
    public int enumerate() {
      Arrays.fill(enabled, beginIndex, endIndex, true);
      final int answer = enumerate0(beginIndex);
      Arrays.fill(enabled, beginIndex, endIndex, true);
      return answer;
    }

    private int enumerate0(int index) {
      if (index >= endIndex) return nextStage().enumerate();

      final int forced = checkForced(index);
      assert forced != CONFLICT; // cannot be CONFLICT if everything is in place.

      assert enabled[index];

      final boolean mustEnable = (forced & MUST_ENABLE) != 0;
      final boolean mustDisable = (forced & MUST_DISABLE) != 0;

      int answer0 = UNKNOWN, answer1 = UNKNOWN;

      if (!mustDisable) {
        answer0 = nextStage().enumerate();
        if (answer0 == NEQ) return NEQ;
        if (answer0 == TIMEOUT) return TIMEOUT;
        assert answer0 != FAST_REJECTED && answer0 != CONFLICT;
      }

      if (!mustEnable) {
        enabled[index] = false;
        answer1 = nextStage().enumerate();
        enabled[index] = true;
        if (answer1 == TIMEOUT) return TIMEOUT;
        assert answer1 != FAST_REJECTED && answer1 != CONFLICT;
      }

      if (answer0 == EQ || answer1 == EQ) return EQ;
      assert answer0 == UNKNOWN;
      assert answer1 == UNKNOWN || answer1 == NEQ;
      return answer1;
    }
  }

  private class MismatchedOutputBreaker extends AbstractEnumerationStage {
    private final boolean disabled;

    public MismatchedOutputBreaker(boolean disabled) {
      this.disabled = disabled;
    }

    @Override
    public int enumerate() {
      if (!disabled) {
        final Substitution rule = I.mkRule(enabled);
        if (isMismatchedOutput(translateToUExpr(rule))) return NEQ;
      }
      return nextStage().enumerate();
    }
  }

  private class MismatchedSummationBreaker extends AbstractEnumerationStage {
    private final boolean disabled;

    public MismatchedSummationBreaker(boolean disabled) {
      this.disabled = disabled;
    }

    @Override
    public int enumerate() {
      if (!disabled) {
        final Substitution rule = I.mkRule(enabled);
        if (isMismatchedSummation(translateToUExpr(rule))) return UNKNOWN;
      }
      return nextStage().enumerate();
    }
  }

  private static class TimeoutBreaker extends AbstractEnumerationStage {
    private final long start;
    private final long timeout;

    private TimeoutBreaker(long start, long timeout) {
      this.start = start;
      this.timeout = timeout;
    }

    @Override
    public int enumerate() {
      final long now = System.currentTimeMillis();
      if (now - start > timeout) return TIMEOUT;
      else return nextStage().enumerate();
    }
  }

  private class Verifier extends AbstractEnumerationStage {
    private final boolean dryRun;

    private Verifier(boolean dryRun) {
      this.dryRun = dryRun;
    }

    @Override
    public int enumerate() {
      if (dryRun) return EQ;

      for (boolean[] knownEq : knownEqs) {
        final PartialOrder cmp = compareBools(knownEq, enabled);
        if (cmp == LESS_THAN || cmp == SAME) return EQ;
      }

      for (boolean[] knownNeq : knownNeqs) {
        final PartialOrder cmp = compareBools(knownNeq, enabled);
        if (cmp == GREATER_THAN || cmp == SAME) return NEQ;
      }

      final Substitution rule = I.mkRule(enabled);
      final UExprTranslationResult uExprs = translateToUExpr(rule);
      final int answer = LogicSupport.proveEq(uExprs);
      assert answer != FAST_REJECTED; // fast rejection should be checked early.

      if (answer == EQ) rememberEq(Arrays.copyOf(enabled, enabled.length));
      else if (answer == NEQ) rememberNeq(Arrays.copyOf(enabled, enabled.length));

      return answer;
    }
  }
}
