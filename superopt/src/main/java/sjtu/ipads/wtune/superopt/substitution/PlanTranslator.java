package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.SetOpKind;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.*;

import java.util.*;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.sqlparser.SqlSupport.mkBinary;
import static sjtu.ipads.wtune.sqlparser.SqlSupport.mkFuncCall;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.ColRef_ColName;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.ColName_Col;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind.EQUAL;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.*;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;

class PlanTranslator {
  private final Substitution rule;
  private final NameSequence aliasSeq;

  private final Constraints constraints;
  private final Symbols srcSyms;

  private final Map<Symbol, SourceDesc> sourceDescs;
  private final Map<Symbol, AttrsDesc> attrsDescs;
  private final Map<Symbol, PredDesc> predDescs;

  private final NameSequence tableSeq;
  private final NameSequence sourceSeq;
  private final NameSequence colSeq;
  private final NameSequence predSeq;

  private Schema schema;

  PlanTranslator(Substitution rule) {
    this.rule = rule;
    this.aliasSeq = NameSequence.mkIndexed("q", 0);
    this.constraints = rule.constraints();
    this.srcSyms = rule._0().symbols();
    this.sourceDescs = new HashMap<>();
    this.attrsDescs = new HashMap<>();
    this.predDescs = new HashMap<>();
    this.tableSeq = NameSequence.mkIndexed("r", 0);
    this.sourceSeq = NameSequence.mkIndexed("t", 0);
    this.colSeq = NameSequence.mkIndexed("c", 0);
    this.predSeq = NameSequence.mkIndexed("p", 0);
  }

  Pair<PlanContext, PlanContext> translate() {
    assignAll();
    schema = mkSchema();
    final PlanContext sourcePlan = new PlanConstructor(rule._0(), false).translate();
    final PlanContext targetPlan = new PlanConstructor(rule._1(), true).translate();
    PlanSupport.resolvePlan(sourcePlan);
    PlanSupport.resolvePlan(targetPlan);
    return Pair.of(sourcePlan, targetPlan);
  }

  private <T> T getDescOf(Map<Symbol, T> descs, Symbol sym) {
    return sym.ctx() == srcSyms ? descs.get(sym) : descs.get(constraints.instantiationOf(sym));
  }

  private SourceDesc sourceDescOf(Symbol tableSym) {
    return getDescOf(sourceDescs, tableSym);
  }

  private AttrsDesc attrsDescOf(Symbol attrsSym) {
    final AttrsDesc desc = getDescOf(attrsDescs, attrsSym);
    if (desc != null) return desc;
    if (attrsSym.ctx() != srcSyms) attrsSym = constraints.instantiationOf(attrsSym);
    return attrsDescOf(constraints.sourceOf(attrsSym));
  }

  private PredDesc predDescOf(Symbol predSym) {
    return getDescOf(predDescs, predSym);
  }

  public Schema schema() {
    if (schema == null) schema = mkSchema();
    return schema;
  }

  private void assignAll() {
    for (Symbol table : srcSyms.symbolsOf(TABLE)) sourceDescs.put(table, assignSourceDesc(table));
    for (Symbol attrs : srcSyms.symbolsOf(ATTRS)) attrsDescs.put(attrs, assignAttrsDesc(attrs));
    for (Symbol pred : srcSyms.symbolsOf(PRED)) predDescs.put(pred, assignPredDesc(pred));
  }

  private TableDesc assignTableDesc(Symbol table) {
    final Constraints constraints = this.constraints;
    for (var pair : sourceDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), table)) return pair.getValue().tableDesc;
    }
    return new TableDesc(tableSeq.next());
  }

  private SourceDesc assignSourceDesc(Symbol table) {
    final TableDesc tableDesc = assignTableDesc(table);
    return new SourceDesc(tableDesc, sourceSeq.next());
  }

  private AttrsDesc assignAttrsDesc(Symbol attrs) {
    final Constraints constraints = this.constraints;
    final Symbol source = constraints.sourceOf(attrs);
    if (source.kind() == ATTRS) return null;

    for (var pair : attrsDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), attrs) && pair.getValue() != null) return pair.getValue();
    }

    final TableDesc tableDesc = sourceDescOf(source).tableDesc;
    final String colName = colSeq.next();
    tableDesc.colNames.add(colName);
    return new AttrsDesc(colName);
  }

  private PredDesc assignPredDesc(Symbol pred) {
    final Constraints constraints = this.constraints;
    for (var pair : predDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), pred)) return pair.getValue();
    }

    return new PredDesc(predSeq.next());
  }

  private Schema mkSchema() {
    final StringBuilder builder = new StringBuilder();

    for (SourceDesc source : sourceDescs.values()) {
      final TableDesc table = source.tableDesc;
      if (table.initialized) continue;
      table.initialized = true;

      builder.append("create table ").append(table.name).append("(\n");
      for (String attrName : table.colNames)
        builder.append("  ").append(attrName).append(" int,\n");
      builder.append("  ").append(colSeq.next()).append(" int);");
    }

    final Set<String> initiatedConstraints = new HashSet<>();
    for (Constraint notNull : constraints.ofKind(NotNull)) {
      final AttrsDesc attr = attrsDescs.get(notNull.symbols()[1]);
      if (!initiatedConstraints.add(attr.colName)) continue;

      final TableDesc table = sourceDescs.get(notNull.symbols()[0]).tableDesc;
      builder
          .append("alter table ")
          .append(table.name)
          .append(" modify column ")
          .append(attr.colName)
          .append(" int not null;\n");
    }

    initiatedConstraints.clear();
    for (Constraint uniqueKey : constraints.ofKind(Unique)) {
      final AttrsDesc attrs = attrsDescs.get(uniqueKey.symbols()[1]);
      if (!initiatedConstraints.add(attrs.colName)) continue;

      final TableDesc table = sourceDescs.get(uniqueKey.symbols()[0]).tableDesc;
      builder
          .append("alter table ")
          .append(table.name)
          .append(" add unique (")
          .append(attrs.colName)
          .append(");");
    }

    initiatedConstraints.clear();
    for (Constraint foreignKey : constraints.ofKind(Reference)) {
      if (!sourceDescs.containsKey(foreignKey.symbols()[0])) continue;

      final AttrsDesc attrs = attrsDescs.get(foreignKey.symbols()[1]);
      final AttrsDesc refAttrs = attrsDescs.get(foreignKey.symbols()[3]);
      if (!initiatedConstraints.add(attrs.colName + refAttrs.colName)) continue;

      final TableDesc refTable = sourceDescs.get(foreignKey.symbols()[2]).tableDesc;
      final TableDesc table = sourceDescs.get(foreignKey.symbols()[0]).tableDesc;

      builder
          .append("alter table ")
          .append(table.name)
          .append(" add foreign key (")
          .append(attrs.colName)
          .append(") references ")
          .append(refTable.name)
          .append('(')
          .append(refAttrs.colName)
          .append(");\n");
    }

    return SqlSupport.parseSchema(MYSQL, builder.toString());
  }

  private static class TableDesc {
    private final String name;
    private final Set<String> colNames;
    private boolean initialized;

    private TableDesc(String name) {
      this.name = name;
      this.colNames = new HashSet<>();
      this.initialized = false;
    }
  }

  private static class SourceDesc {
    private final TableDesc tableDesc;
    private final String qualification;

    private SourceDesc(TableDesc tableDesc, String qualification) {
      this.tableDesc = tableDesc;
      this.qualification = qualification;
    }
  }

  private static class AttrsDesc {
    private final String colName;

    private AttrsDesc(String colName) {
      this.colName = colName;
    }
  }

  private static class PredDesc {
    private final String predName;

    private PredDesc(String predName) {
      this.predName = predName;
    }
  }

  private class PlanConstructor {
    private final Fragment template;
    private final SqlContext sql;
    private final PlanContext plan;
    private final boolean isTargetSide;
    private final Map<Op, PlanNode> instantiatedOps;

    private PlanConstructor(Fragment template, boolean isTargetSide) {
      this.template = template;
      this.sql = SqlContext.mk(16);
      this.plan = PlanContext.mk(schema);
      this.isTargetSide = isTargetSide;
      this.instantiatedOps = new HashMap<>(8);
    }

    private PlanContext translate() {
      final int rootId = trTree(template.root());
      assert rootId != NO_SUCH_NODE;
      return plan;
    }

    private int trTree(Op op) {
      int lhsChild = -1, rhsChild = -1;
      final int numPredecessors = op.kind().numPredecessors();
      if (numPredecessors > 0) lhsChild = trTree(op.predecessors()[0]);
      if (numPredecessors > 1) rhsChild = trTree(op.predecessors()[1]);
      final PlanNode node = tr(op);
      instantiatedOps.put(op, node);
      final int nodeId = plan.bindNode(node);
      if (numPredecessors > 0) plan.setChild(nodeId, 0, lhsChild);
      if (numPredecessors > 1) plan.setChild(nodeId, 1, rhsChild);
      return nodeId;
    }

    private PlanNode tr(Op op) {
      switch (op.kind()) {
        case INPUT:
          return trInput((Input) op);
        case INNER_JOIN:
        case LEFT_JOIN:
          return trJoin((Join) op);
        case SIMPLE_FILTER:
          return trSimpleFilter((SimpleFilter) op);
        case IN_SUB_FILTER:
          return trInSubFilter((InSubFilter) op);
        case PROJ:
          return trProj((Proj) op);
        case SET_OP:
          return trUnion((Union) op);
        default:
          throw new IllegalArgumentException("unknown operator type: " + op.kind());
      }
    }

    private PlanNode trInput(Input input) {
      final SourceDesc desc = sourceDescOf(input.table());
      return InputNode.mk(schema.table(desc.tableDesc.name), desc.qualification);
    }

    private PlanNode trJoin(Join join) {
      final SqlNode lhsKey = trAttrs(join.lhsAttrs(), join.predecessors()[0]);
      final SqlNode rhsKey = trAttrs(join.rhsAttrs(), join.predecessors()[1]);
      final SqlNode joinCond = mkBinary(sql, EQUAL, lhsKey, rhsKey);
      final Expression joinCondExpr = Expression.mk(joinCond);
      return JoinNode.mk(joinKindOf(join), joinCondExpr);
    }

    private PlanNode trSimpleFilter(SimpleFilter filter) {
      final SqlNode key = trAttrs(filter.attrs(), filter.predecessors()[0]);
      final String predName = predDescOf(filter.predicate()).predName;
      final SqlNode pred = mkFuncCall(sql, predName, singletonList(key));
      return SimpleFilterNode.mk(Expression.mk(pred));
    }

    private PlanNode trInSubFilter(InSubFilter filter) {
      final SqlNode key = trAttrs(filter.attrs(), filter.predecessors()[0]);
      return InSubNode.mk(Expression.mk(key));
    }

    private PlanNode trProj(Proj proj) {
      final SqlNode colRef = trAttrs(proj.attrs(), proj.predecessors()[0]);
      final String colName = colRef.$(ColRef_ColName).$(ColName_Col);
      final String qualification = aliasSeq.next();
      final List<String> nameList = singletonList(colName);
      final List<Expression> exprList = singletonList(Expression.mk(colRef));
      final ProjNode node = ProjNode.mk(proj.isDeduplicated(), nameList, exprList);
      node.setQualification(qualification);
      return node;
    }

    private PlanNode trUnion(Union union) {
      return SetOpNode.mk(union.isDeduplicated(), SetOpKind.UNION);
    }

    private SqlNode trAttrs(Symbol attrs, Op predecessor) {
      final String name = attrsDescOf(attrs).colName;
      final String qualification = findSourceIn(deepSourceOf(attrs), predecessor);
      assert qualification != null;
      return SqlSupport.mkColRef(sql, qualification, name);
    }

    private Symbol deepSourceOf(Symbol attrs) {
      final Constraints constraints = rule.constraints();
      if (isTargetSide) attrs = constraints.instantiationOf(attrs);
      Symbol source = constraints.sourceOf(attrs);
      while (source.kind() != TABLE) source = constraints.sourceOf(source);
      return source;
    }

    private String findSourceIn(Symbol source, Op root) {
      if (root.kind() == INPUT) {
        Symbol table = ((Input) root).table();
        if (isTargetSide) table = rule.constraints().instantiationOf(table);
        if (source == table) return ((InputNode) instantiatedOps.get(root)).qualification();

      } else if (root.kind() == PROJ) {
        if (findSourceIn(source, root.predecessors()[0]) != null)
          return ((ProjNode) instantiatedOps.get(root)).qualification();

      } else {
        for (Op predecessor : root.predecessors()) {
          final String found = findSourceIn(source, predecessor);
          if (found != null) return found;
        }
      }

      return null;
    }

    private static JoinKind joinKindOf(Join join) {
      final OpKind kind = join.kind();
      if (kind == LEFT_JOIN) return JoinKind.LEFT_JOIN;
      else if (kind == INNER_JOIN) return JoinKind.INNER_JOIN;
      else throw new IllegalArgumentException("unsupported join kind: " + kind);
    }
  }
}
