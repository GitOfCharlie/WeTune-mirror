package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.elemAt;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipMap;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.add;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.eqPred;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.mul;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.not;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.squash;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.sum;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.table;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.uninterpretedPred;
import static sjtu.ipads.wtune.prover.uexpr.Var.mkBase;
import static sjtu.ipads.wtune.prover.uexpr.Var.mkConstant;
import static sjtu.ipads.wtune.prover.utils.Constants.NOT_NULL_PRED;
import static sjtu.ipads.wtune.prover.utils.Constants.NULL_VAR;
import static sjtu.ipads.wtune.prover.utils.Constants.TRANSLATOR_VAR_PREFIX;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LIMIT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SORT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.UNION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sjtu.ipads.wtune.prover.utils.Util;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.ExistsFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.InSubFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.InputNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.SimpleFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan1.Ref;
import sjtu.ipads.wtune.sqlparser.plan1.RefBag;
import sjtu.ipads.wtune.sqlparser.plan1.SetOpNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.plan1.ValueBag;

public class UExprTranslator {
  private static final Var ROOT_VAR = Var.mkBase(TRANSLATOR_VAR_PREFIX);

  private final PlanNode plan;
  private final PlanContext ctx;

  private final List<QueryScope> scopes;
  private final Map<PlanNode, Var> varOwnership;

  private int nextVarIdx = 0;

  UExprTranslator(PlanNode plan) {
    this.plan = plan;
    this.ctx = requireNonNull(plan.context());
    this.scopes = new ArrayList<>(4);
    this.varOwnership = new HashMap<>(8);

    pushScope();
  }

  public static UExpr translate(PlanNode plan) {
    return new UExprTranslator(plan).onNode(plan);
  }

  private UExpr onNode(PlanNode node) {
    // TODO: come up with how to deal Agg
    // Note: some weird query like (Select Sum ...) Union (...) cannot be handled for now.
    return switch (node.type()) {
      case PROJ -> onProj((ProjNode) node);
      case INPUT -> onInput((InputNode) node);
      case INNER_JOIN, LEFT_JOIN -> onJoin((JoinNode) node);
      case SIMPLE_FILTER -> onPlainFilter((SimpleFilterNode) node);
      case IN_SUB_FILTER -> onInSubFilter((InSubFilterNode) node);
      case EXISTS_FILTER -> onExistsFilter((ExistsFilterNode) node);
      case UNION -> onUnion((SetOpNode) node);
      case AGG, SORT, LIMIT -> onNode(node.predecessors()[0]);
    };
  }

  private UExpr onInput(InputNode input) {
    return table(input.table().name(), localScope().makeVar(input));
  }

  private UExpr onProj(ProjNode proj) {
    pushScope();

    final QueryScope outerScope = outerScope();
    final ValueBag vs = proj.values();
    final List<Var> joints;
    if (outerScope.joints() != null) {
      // this branch is for 1. the root query 2. the subquery in a Union
      // 3. the subquery in a IN-Sub/Exists Filter
      joints = outerScope.joints;
    } else {
      // this branch is for the subquery in From/Join
      final Var pivotVar = outerScope.makeVar(proj);
      joints = listMap(vs, it -> pivotVar.proj(it.name()));
    }

    // Each select item {a.x AS b} turns into a predicate [a.x = t.b],
    // where `a` is var for table `a`, `t` is the outer var.
    final List<UExpr> terms = new ArrayList<>(vs.size() + 1);
    terms.add(onNode(proj.predecessors()[0]));
    terms.addAll(zipMap(joints, vs, (o, v) -> eqPred(o, asTuple(v.expr()))));

    final UExpr expr = sum(localScope().localVars(), Util.mkProduct(terms));

    popScope();

    return proj.isExplicitDistinct() ? squash(expr) : expr;
  }

  private UExpr onPlainFilter(SimpleFilterNode filter) {
    final UExpr remaining = onNode(filter.predecessors()[0]);

    final Expr predicate = filter.predicate();
    final UExpr condExpr;
    if (predicate.isJoinCondition()) {
      final RefBag refs = predicate.refs();
      assert refs.size() == 2;
      condExpr = mkNullSafeEq(asTuple(refs.get(0)), asTuple(refs.get(1)));

    } else if (predicate.isEquiCondition()) {
      final RefBag refs = predicate.refs();
      assert refs.size() == 1;

      final ASTNode lhsExpr = predicate.template().get(BINARY_LEFT);
      final ASTNode rhsExpr = predicate.template().get(BINARY_RIGHT);
      final Var lhs = asTuple(refs.get(0));
      final Var rhs = mkConstant((LITERAL.isInstance(lhsExpr) ? lhsExpr : rhsExpr).toString());
      condExpr = eqPred(lhs, rhs);

    } else {
      condExpr = mkUninterpretedPred(predicate);
    }

    return mul(condExpr, remaining);
  }

  private UExpr onInSubFilter(InSubFilterNode filter) {
    final UExpr lhs = onNode(filter.predecessors()[0]);
    localScope().setJoints(asList(asTuples(filter.lhsRefs())));
    final UExpr rhs = onNode(filter.predecessors()[1]);
    localScope().setJoints(null);

    return mul(lhs, UExpr.squash(rhs));
  }

  private UExpr onExistsFilter(ExistsFilterNode filter) {
    final UExpr lhs = onNode(filter.predecessors()[0]);
    localScope().setJoints(emptyList());
    final UExpr rhs = onNode(filter.predecessors()[1]);
    localScope().setJoints(null);
    return mul(lhs, UExpr.squash(rhs));
  }

  private UExpr onJoin(JoinNode join) {
    final UExpr lhsExpr = onNode(join.predecessors()[0]);
    final UExpr rhsExpr = onNode(join.predecessors()[1]);

    if (join.condition() == null) return mul(lhsExpr, rhsExpr);

    final UExpr cond;
    if (join.isEquiJoin()) {
      final RefBag lhs = join.lhsRefs(), rhs = join.rhsRefs();
      assert lhs.size() == rhs.size();
      cond = Util.mkProduct(zipMap(lhs, rhs, (x, y) -> mkNullSafeEq(asTuple(x), asTuple(y))));
    } else {
      cond = mkUninterpretedPred(join.condition());
    }
    // L(x) * R(y) * p(x,y)
    final UExpr symmPart = mul(mul(cond, lhsExpr), rhsExpr);

    if (join.type() == INNER_JOIN) return symmPart;

    if (join.type() == LEFT_JOIN) {
      // L(x) * [y = null] * not(Sum{y'}(R(y') * p(x,y')))
      final UExpr asymmPart = mkAsymmetricJoin(join, cond, lhsExpr, rhsExpr);
      // L(x) * R(y) * p(x,y) +
      // L(x) * [y = null] * not(Sum{y'}(R(y') * p(x,y')))
      return add(symmPart, asymmPart);
    }

    throw new IllegalArgumentException("unsupported join type: " + join.type());
  }

  private UExpr onUnion(SetOpNode setOp) {
    // TODO: support other set operation
    if (setOp.operation() != SetOperation.UNION)
      throw failed("unsupported set operation: " + setOp.operation());

    // The root of union tree is responsible to create the join var.
    // e.g., Select sub.a From ((Select A.a From A) Union (Select B.b From B)) As sub
    // => Sum{x}(Sum{y}([x.a = y.a] * A(y)) + Sum{z}([x.a = z.b] * B(z)))

    final QueryScope localScope = localScope();
    final boolean isUnionRoot = isUnionRoot(setOp);

    if (isUnionRoot) {
      final ValueBag values = setOp.values();
      final Var joint = localScope.makeVar(ctx.ownerOf(values.get(0)));
      localScope.setJoints(listMap(values, it -> joint.proj(it.name())));
    }

    final UExpr lhsExpr = onNode(setOp.predecessors()[0]);
    final UExpr rhsExpr = onNode(setOp.predecessors()[1]);

    if (isUnionRoot) localScope.setJoints(null);

    if (setOp.distinct()) return squash(add(lhsExpr, rhsExpr));
    else return add(lhsExpr, rhsExpr);
  }

  private void pushScope() {
    scopes.add(new QueryScope(scopes.isEmpty()));
  }

  private void popScope() {
    scopes.remove(scopes.size() - 1);
  }

  private QueryScope outerScope() {
    return elemAt(scopes, -2);
  }

  private QueryScope localScope() {
    return elemAt(scopes, -1);
  }

  private Var asTuple(Ref ref) {
    final Value v = ctx.deRef(ref);
    final Var var = varOwnership.get(ctx.ownerOf(v));
    return var.proj(v.name());
  }

  private Var asTuple(Expr expr) {
    final RefBag refs = expr.refs();
    if (expr.isIdentity()) {
      assert refs.size() == 1;
      return asTuple(refs.get(0));
    } else {
      return Var.mkFunc(expr.template().toString(), asTuples(refs));
    }
  }

  private Var[] asTuples(Collection<Ref> refs) {
    return arrayMap(refs, this::asTuple, Var.class);
  }

  private UExpr mkUninterpretedPred(Expr expr) {
    final Var[] args = asTuples(expr.refs());
    return UExpr.uninterpretedPred(expr.template().toString(), args);
  }

  private UExpr mkAsymmetricJoin(PlanNode asymmJoin, UExpr cond, UExpr lhsExpr, UExpr rhsExpr) {
    // A LEFT JOIN B ON p(A.a,B.b) =>
    // Sum{x,y}(A(x) * B(y) * [p(x.a,y.b)] * [NotNull(x.a)] * [NotNull(y.b)]) -- symmetric part
    // + Sum{x}(A(x) * [IsNull(y)] * not(Sum{y}(B(y) * [p(x.a,y.b)]))) -- asymmetric part
    // this method returns the part "[IsNull(y)] * not(Sum{y'}(B(y') * p(x.a,y'.b))

    cond = cond.copy();
    lhsExpr = lhsExpr.copy();
    rhsExpr = rhsExpr.copy();

    // Usually the RHS is a single source (a plain or a subquery),
    // but we handle the most general case.
    final List<Var> oldVars =
        asymmJoin.predecessors()[1].values().stream()
            .map(ctx::ownerOf)
            .map(varOwnership::get)
            .distinct()
            .toList();

    final List<Var> newVars = new ArrayList<>(oldVars.size());
    final List<UExpr> isNullPreds = new ArrayList<>(oldVars.size());

    for (Var oldVar : oldVars) {
      final Var newVar = mkBase(TRANSLATOR_VAR_PREFIX + nextVarIdx++);
      newVars.add(newVar);
      isNullPreds.add(eqPred(oldVar, NULL_VAR));

      cond.subst(oldVar, newVar);
      rhsExpr.subst(oldVar, newVar);
    }

    return mul(lhsExpr, mul(Util.mkProduct(isNullPreds), not(sum(newVars, mul(cond, rhsExpr)))));
  }

  private static UExpr mkNullSafeEq(Var t0, Var t1) {
    return mul(eqPred(t0, t1), uninterpretedPred(NOT_NULL_PRED, t1));
  }

  private static boolean isUnionRoot(SetOpNode node) {
    // if the query the left-most query in a Union
    // returns true if `node` is not a union component
    PlanNode n = node;

    while (n.successor() != null) {
      final PlanNode succ = n.successor();
      final OperatorType succType = succ.type();

      if (succType == UNION) return false;
      if (succType == SORT || succType == LIMIT) n = succ;
      else return true;
    }

    return true;
  }

  private RuntimeException failed(String reason) {
    return new IllegalArgumentException(
        "failed to translate plan to U-expr. [" + reason + "] " + plan);
  }

  private class QueryScope {
    protected boolean isRoot;
    protected final List<Var> localVars;
    protected List<Var> joints;
    // `hinge` is the connection between current query to a subquery
    // Currently it is used in "`outer_col` IN <subquery>"

    private QueryScope(boolean isRoot) {
      this.isRoot = isRoot;
      this.localVars = new ArrayList<>(4);
    }

    private List<Var> localVars() {
      return localVars;
    }

    private List<Var> joints() {
      return joints;
    }

    private void setJoints(List<Var> joints) {
      this.joints = joints;
    }

    private Var makeVar(PlanNode owner) {
      if (isRoot) return ROOT_VAR;

      final Var var = Var.mkBase(TRANSLATOR_VAR_PREFIX + nextVarIdx++);
      varOwnership.put(owner, var);
      localVars.add(var);
      return var;
    }
  }
}
