package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;

public interface ExprFields {
  // Variable
  FieldKey<VariableScope> VARIABLE_SCOPE = VARIABLE.attr("scope", VariableScope.class);
  FieldKey<String> VARIABLE_NAME = VARIABLE.strAttr("name");
  FieldKey<SQLNode> VARIABLE_ASSIGNMENT = VARIABLE.nodeAttr("assignment");
  // Col Ref
  FieldKey<SQLNode> COLUMN_REF_COLUMN = COLUMN_REF.nodeAttr("column");
  // Func Call
  FieldKey<SQLNode> FUNC_CALL_NAME = FUNC_CALL.nodeAttr("name");
  FieldKey<List<SQLNode>> FUNC_CALL_ARGS = FUNC_CALL.nodesAttr("args");
  // Collate
  FieldKey<SQLNode> COLLATE_EXPR = COLLATE.nodeAttr("expr");
  FieldKey<SQLNode> COLLATE_COLLATION = COLLATE.nodeAttr("collation");
  // Interval
  FieldKey<SQLNode> INTERVAL_EXPR = INTERVAL.nodeAttr("expr");
  FieldKey<IntervalUnit> INTERVAL_UNIT = INTERVAL.attr("unit", IntervalUnit.class);
  // Symbol
  FieldKey<String> SYMBOL_TEXT = SYMBOL.strAttr("text");
  // Literal
  FieldKey<LiteralType> LITERAL_TYPE = LITERAL.attr("type", LiteralType.class);
  FieldKey<Object> LITERAL_VALUE = LITERAL.attr("value", Object.class);
  FieldKey<String> LITERAL_UNIT = LITERAL.strAttr("unit");
  // Aggregate
  FieldKey<String> AGGREGATE_NAME = AGGREGATE.strAttr("name");
  FieldKey<Boolean> AGGREGATE_DISTINCT = AGGREGATE.boolAttr("distinct");
  FieldKey<List<SQLNode>> AGGREGATE_ARGS = AGGREGATE.nodesAttr("args");
  FieldKey<String> AGGREGATE_WINDOW_NAME = AGGREGATE.strAttr("windowName");
  FieldKey<SQLNode> AGGREGATE_WINDOW_SPEC = AGGREGATE.nodeAttr("windowSpec");
  FieldKey<SQLNode> AGGREGATE_FILTER = AGGREGATE.nodeAttr("filter");
  FieldKey<List<SQLNode>> AGGREGATE_WITHIN_GROUP_ORDER = AGGREGATE.nodesAttr("withinGroupOrder");
  FieldKey<List<SQLNode>> AGGREGATE_ORDER = AGGREGATE.nodesAttr("order");
  FieldKey<String> AGGREGATE_SEP = AGGREGATE.strAttr("sep");
  // Wildcard
  FieldKey<SQLNode> WILDCARD_TABLE = WILDCARD.nodeAttr("table");
  // Grouping
  FieldKey<List<SQLNode>> GROUPING_OP_EXPRS = GROUPING_OP.nodesAttr("exprs");
  // Unary
  FieldKey<UnaryOp> UNARY_OP = UNARY.attr("op", UnaryOp.class);
  FieldKey<SQLNode> UNARY_EXPR = UNARY.nodeAttr("expr");
  // Binary
  FieldKey<BinaryOp> BINARY_OP = BINARY.attr("op", BinaryOp.class);
  FieldKey<SQLNode> BINARY_LEFT = BINARY.nodeAttr("left");
  FieldKey<SQLNode> BINARY_RIGHT = BINARY.nodeAttr("right");
  FieldKey<SubqueryOption> BINARY_SUBQUERY_OPTION =
      BINARY.attr("subqueryOption", SubqueryOption.class);
  // TERNARY
  FieldKey<TernaryOp> TERNARY_OP = TERNARY.attr("op", TernaryOp.class);
  FieldKey<SQLNode> TERNARY_LEFT = TERNARY.nodeAttr("left");
  FieldKey<SQLNode> TERNARY_MIDDLE = TERNARY.nodeAttr("middle");
  FieldKey<SQLNode> TERNARY_RIGHT = TERNARY.nodeAttr("right");
  // Tuple
  FieldKey<List<SQLNode>> TUPLE_EXPRS = TUPLE.nodesAttr("exprs");
  FieldKey<Boolean> TUPLE_AS_ROW = TUPLE.boolAttr("asRow");
  // Exists
  FieldKey<SQLNode> EXISTS_SUBQUERY_EXPR = EXISTS.nodeAttr("subquery");
  // MatchAgainst
  FieldKey<List<SQLNode>> MATCH_COLS = MATCH.nodesAttr("columns");
  FieldKey<SQLNode> MATCH_EXPR = MATCH.nodeAttr("expr");
  FieldKey<MatchOption> MATCH_OPTION = MATCH.attr("option", MatchOption.class);
  // Cast
  FieldKey<SQLNode> CAST_EXPR = CAST.nodeAttr("expr");
  FieldKey<SQLDataType> CAST_TYPE = CAST.attr("type", SQLDataType.class);
  FieldKey<Boolean> CAST_IS_ARRAY = CAST.boolAttr("isArray");
  // Case
  FieldKey<SQLNode> CASE_COND = CASE.nodeAttr("condition");
  FieldKey<List<SQLNode>> CASE_WHENS = CASE.nodesAttr("when");
  FieldKey<SQLNode> CASE_ELSE = CASE.nodeAttr("else");
  // When
  FieldKey<SQLNode> WHEN_COND = WHEN.nodeAttr("condition");
  FieldKey<SQLNode> WHEN_EXPR = WHEN.nodeAttr("expr");
  // ConvertUsing
  FieldKey<SQLNode> CONVERT_USING_EXPR = CONVERT_USING.nodeAttr("expr");
  FieldKey<SQLNode> CONVERT_USING_CHARSET = CONVERT_USING.nodeAttr("charset");
  // Default
  FieldKey<SQLNode> DEFAULT_COL = DEFAULT.nodeAttr("col");
  // Values
  FieldKey<SQLNode> VALUES_EXPR = VALUES.nodeAttr("expr");
  // QueryExpr
  FieldKey<SQLNode> QUERY_EXPR_QUERY = QUERY_EXPR.nodeAttr("query");
  // Indirection
  FieldKey<SQLNode> INDIRECTION_EXPR = INDIRECTION.nodeAttr("expr");
  FieldKey<List<SQLNode>> INDIRECTION_COMPS = INDIRECTION.nodesAttr("comps");
  // IndirectionComp
  FieldKey<Boolean> INDIRECTION_COMP_SUBSCRIPT = INDIRECTION_COMP.boolAttr("subscript");
  FieldKey<SQLNode> INDIRECTION_COMP_START = INDIRECTION_COMP.nodeAttr("start");
  FieldKey<SQLNode> INDIRECTION_COMP_END = INDIRECTION_COMP.nodeAttr("end");
  // ParamMarker
  FieldKey<Integer> PARAM_MARKER_NUMBER = PARAM_MARKER.attr("number", Integer.class);
  // ComparisonMod
  FieldKey<SubqueryOption> COMPARISON_MOD_OPTION =
      COMPARISON_MOD.attr("option", SubqueryOption.class);
  FieldKey<SQLNode> COMPARISON_MOD_EXPR = COMPARISON_MOD.nodeAttr("expr");
  // Array
  FieldKey<List<SQLNode>> ARRAY_ELEMENTS = ARRAY.nodesAttr("elements");
  // TypeCoercion
  FieldKey<SQLDataType> TYPE_COERCION_TYPE = TYPE_COERCION.attr("type", SQLDataType.class);
  FieldKey<String> TYPE_COERCION_STRING = TYPE_COERCION.strAttr("type");
  // DataTimeOverlap
  FieldKey<SQLNode> DATETIME_OVERLAP_LEFT_START = DATETIME_OVERLAP.nodeAttr("leftStart");
  FieldKey<SQLNode> DATETIME_OVERLAP_LEFT_END = DATETIME_OVERLAP.nodeAttr("leftEnd");
  FieldKey<SQLNode> DATETIME_OVERLAP_RIGHT_START = DATETIME_OVERLAP.nodeAttr("rightStart");
  FieldKey<SQLNode> DATETIME_OVERLAP_RIGHT_END = DATETIME_OVERLAP.nodeAttr("rightEnd");

  static int getOperatorPrecedence(SQLNode node) {
    if (!EXPR.isInstance(node)) return -1;
    final ExprType exprKind = node.get(EXPR_KIND);
    return switch (exprKind) {
      case UNARY -> node.get(UNARY_OP).precedence();
      case BINARY -> node.get(BINARY_OP).precedence();
      case TERNARY -> node.get(TERNARY_OP).precedence();
      case CASE, WHEN -> 5;
      case COLLATE -> 13;
      case INTERVAL -> 14;
      default -> -1;
    };
  }
}