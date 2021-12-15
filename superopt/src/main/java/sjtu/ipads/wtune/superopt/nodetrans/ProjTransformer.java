package sjtu.ipads.wtune.superopt.nodetrans;

import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeNode.SPJNode;
import sjtu.ipads.wtune.spes.AlgeNode.UnionNode;
import sjtu.ipads.wtune.spes.RexNodeHelper.RexNodeHelper;
import sjtu.ipads.wtune.sqlparser.plan1.Expression;
import sjtu.ipads.wtune.sqlparser.plan1.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.plan1.Values;

import java.util.ArrayList;
import java.util.List;

public class ProjTransformer extends BaseTransformer {
  private RexNode Expression2RexNode(Expression expr) {
    Values valuesOfNode = planCtx.valuesOf(planNode);

    // Get a value's index just in projected values
    // Since expr's valueRefs may not equal to values on this plan node
    Values values = planCtx.valuesReg().valueRefsOf(expr);
    assert !values.isEmpty();

    List<RexInputRef> rexRefs = new ArrayList<>(values.size());
    for (Value val : values) {
      int idx = values.indexOf(val);
      rexRefs.add(new RexInputRef(idx, defaultIntType()));
    }

    return rexRefs.get(0);
  }

  @Override
  public AlgeNode transform() {
    ProjNode proj = ((ProjNode) planNode);
    AlgeNode childNode = transformNode(proj.child(planCtx, 0), planCtx, z3Context);

    List<RexNode> columns = ListSupport.map(proj.attrExprs(), this::Expression2RexNode);
    if (childNode instanceof UnionNode) {
      updateUnion((UnionNode) childNode, columns);
      return childNode;
    }
    if (childNode instanceof SPJNode) {
      updateSPJ(childNode, columns);
      return childNode;
    } else {
      // System.out.println("error in project parser:" + childNode.toString());
      return childNode;
    }
  }

  private void updateSPJ(AlgeNode spjNode, List<RexNode> columns) {
    updateOutputExprs(spjNode, columns);
  }

  private void updateUnion(UnionNode unionNode, List<RexNode> columns) {
    for (AlgeNode input : unionNode.getInputs()) {
      updateOutputExprs(input, columns);
    }
  }

  private void updateOutputExprs(AlgeNode inputNode, List<RexNode> columns) {
    List<RexNode> inputExprs = inputNode.getOutputExpr();
    List<RexNode> newOutputExpr = new ArrayList<>();
    for (RexNode expr : columns) {
      newOutputExpr.add(RexNodeHelper.substitute(expr, inputExprs));
    }
    inputNode.setOutputExpr(newOutputExpr);
  }
}