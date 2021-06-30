package sjtu.ipads.wtune.prover.normalform;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Conjunction {
  List<Tuple> vars();

  List<UExpr> predicates();

  List<UExpr> tables();

  Disjunction negation();

  Disjunction squash();

  boolean isEmpty();

  UExpr toExpr();

  void subst(Tuple v1, Tuple v2);

  boolean uses(Tuple v);

  boolean usesInBody(Tuple v);

  Conjunction copy();

  static Conjunction make(
      List<Tuple> sumTuples,
      List<UExpr> tables,
      List<UExpr> predicates,
      Disjunction squash,
      Disjunction negation) {
    return ConjunctionImpl.make(sumTuples, tables, predicates, squash, negation);
  }

  static Conjunction empty() {
    return ConjunctionImpl.empty();
  }
}
