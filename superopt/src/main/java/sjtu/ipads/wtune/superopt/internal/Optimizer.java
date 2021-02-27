package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator;
import sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public class Optimizer {
  private final List<PlanNode> optimized = new ArrayList<>();
  private final Set<String> known = new HashSet<>();

  private final Queue<PlanNode> toOptimized = new LinkedList<>();

  private final SubstitutionBank repo;
  private final Schema schema;

  private Optimizer(SubstitutionBank bank, Schema schema) {
    this.repo = bank;
    this.schema = schema;
  }

  public static Optimizer make(SubstitutionBank bank, Schema schema) {
    return new Optimizer(bank, schema);
  }

  public List<ASTNode> optimize(ASTNode ast) {
    final List<PlanNode> plans = optimize(ToPlanTranslator.translate(ast));
    return listMap(ToASTTranslator::translate, plans);
  }

  public List<PlanNode> optimize(PlanNode node) {
    optimized.clear();
    known.clear();
    toOptimized.clear();

    known.add(ToASTTranslator.translate(node).toString().toLowerCase());
    toOptimized.offer(node);

    optimize0();

    return optimized;
  }

  private void optimize0() {
    while (!toOptimized.isEmpty()) {
      final PlanNode plan = toOptimized.poll();
      matchAndSubstitute(plan);
    }
  }

  /*
   Implementation Note:
   Please pay extra attention to the successor/predecessor relation when mutating a plan tree.
   Specifically, make sure that the invariant holds:
        for any node.
          node \in node.successor.predecessors /\
          for any p \in node.predecessors. p.successor = node

   Guide: say you have mutated a sub-tree in a plan, then
     1. All nodes that on the path from the sub-tree root to the plan tree root should be copied
        (do this by PlanNode::copyToRoot)
     2. All child trees of the sub-tree should be totally copied
        (do this by PlanNode::copyTree)
  */

  private void matchAndSubstitute(PlanNode node) {
    for (Substitution sub : match0(node)) {
      final Interpretations interpretations = Interpretations.constrainedBy(sub.constraints());
      for (Match match : match1(node, sub.g0().head(), interpretations)) {
        // impl note: `substituted` should be the root of new plan instead of matching point
        final PlanNode substituted = match.substitute(sub.g1(), interpretations);
        // match the substituted plan from beginning
        final ASTNode ast = toAST(substituted);
        if (known.add(ast.toString().toLowerCase())) {
          // re-translate the plan to reset rearrangement
          final PlanNode newPlan = ToPlanTranslator.translate(ast);
          optimized.add(newPlan);
          toOptimized.offer(newPlan);
        }
      }
    }
    // percolate down
    for (PlanNode predecessor : node.predecessors()) matchAndSubstitute(predecessor);
  }

  public List<Substitution> match0(PlanNode op) {
    return stream(Fingerprint.make(op))
        .map(repo::findByFingerprint)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public static List<Match> match1(PlanNode node, Operator op, Interpretations inter) {
    final List<Match> ret = new ArrayList<>();

    for (PlanNode n : Hint.apply(node, op, inter)) {
      // setup snapshot to isolate each possible plan given by hint
      final Snapshot snapshot = inter.snapshot();
      inter.derive();

      if (!op.match(n, inter)) {
        inter.setSnapshot(snapshot);
        continue;
      }

      final PlanNode[] nodePreds = n.predecessors();
      final Operator[] opPreds = op.predecessors();

      // Subtlety here is that
      // 1. each child may have multiple matching
      // 2. matching of the next child is affected by how the previous child is matched.
      // In other words, the final matching is accumulated by the matching of each child.
      //
      // Thus, a list `lastResults` is maintained, represents the accumulated matching so far.
      // The matching of each child will be based on each of `lastResults`.

      List<Match> lastResults = singletonList(Match.build(n, inter.snapshot()));
      for (int i = 0, bound = opPreds.length; i < bound; i++) {
        // failed to match last child, break
        if (lastResults.isEmpty()) break;

        final PlanNode nodePred = nodePreds[i];
        final Operator opPred = opPreds[i];

        final List<Match> currentResults = new ArrayList<>();
        // based on each matching of last child, try to match current child
        for (Match lastResult : lastResults) {
          // 1. restore the interpretation of last result -- the meaning of "based on"
          inter.setSnapshot(lastResult.interpretation());
          // 2. current child reports multiple matching
          final List<Match> results = match1(nodePred, opPred, inter);
          // 3. accumulate the results to `lastResult`
          currentResults.addAll(listMap(Match::percolateUp, results));
        }

        lastResults = currentResults;
      }

      ret.addAll(lastResults);

      inter.setSnapshot(snapshot);
    }

    return ret;
  }

  private ASTNode toAST(PlanNode plan) {
    final ASTNode ast = ToASTTranslator.translate(plan);
    ASTContext.manage(schema.dbType(), ast);
    ast.context().setSchema(schema);
    return ast;
  }
}