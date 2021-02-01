package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeFieldBase;

import java.util.EnumSet;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;

public interface NodeFields {
  FieldKey<NodeType> NODE_TYPE = NodeFieldBase.build("nodeType", NodeType.class);
  FieldKey<SQLNode> PARENT = NodeFieldBase.build("parent", SQLNode.class);

  //// TableName
  FieldKey<String> TABLE_NAME_SCHEMA = TABLE_NAME.strAttr("schema");
  FieldKey<String> TABLE_NAME_TABLE = TABLE_NAME.strAttr("table");

  //// ColumnName
  FieldKey<String> COLUMN_NAME_SCHEMA = COLUMN_NAME.strAttr("schema");
  FieldKey<String> COLUMN_NAME_TABLE = COLUMN_NAME.strAttr("table");
  FieldKey<String> COLUMN_NAME_COLUMN = COLUMN_NAME.strAttr("column");

  //// CommonName2
  FieldKey<String> NAME_2_0 = NAME_2.strAttr("part0");
  FieldKey<String> NAME_2_1 = NAME_2.strAttr("part1");

  //// CommonName3
  FieldKey<String> NAME_3_0 = NAME_3.strAttr("part0");
  FieldKey<String> NAME_3_1 = NAME_3.strAttr("part1");
  FieldKey<String> NAME_3_2 = NAME_3.strAttr("part2");

  //// CreateTable
  FieldKey<SQLNode> CREATE_TABLE_NAME = CREATE_TABLE.nodeAttr("name");
  FieldKey<List<SQLNode>> CREATE_TABLE_COLUMNS = CREATE_TABLE.nodesAttr("columns");
  FieldKey<List<SQLNode>> CREATE_TABLE_CONSTRAINTS = CREATE_TABLE.nodesAttr("constraints");
  FieldKey<String> CREATE_TABLE_ENGINE = CREATE_TABLE.strAttr("engine");

  //// ColumnDef
  FieldKey<SQLNode> COLUMN_DEF_NAME = COLUMN_DEF.nodeAttr("name");
  FieldKey<String> COLUMN_DEF_DATATYPE_RAW = COLUMN_DEF.strAttr("typeRaw");
  FieldKey<SQLDataType> COLUMN_DEF_DATATYPE = COLUMN_DEF.attr("dataType", SQLDataType.class);
  FieldKey<EnumSet<ConstraintType>> COLUMN_DEF_CONS = COLUMN_DEF.attr("constraint", EnumSet.class);
  FieldKey<SQLNode> COLUMN_DEF_REF = COLUMN_DEF.nodeAttr("references");
  FieldKey<Boolean> COLUMN_DEF_GENERATED = COLUMN_DEF.boolAttr("genearted");
  FieldKey<Boolean> COLUMN_DEF_DEFAULT = COLUMN_DEF.boolAttr("default");
  FieldKey<Boolean> COLUMN_DEF_AUTOINCREMENT = COLUMN_DEF.boolAttr("autoInc");

  //// References
  FieldKey<SQLNode> REFERENCES_TABLE = REFERENCES.nodeAttr("table");
  FieldKey<List<SQLNode>> REFERENCES_COLUMNS = REFERENCES.nodesAttr("columns");

  //// IndexDef
  FieldKey<String> INDEX_DEF_NAME = INDEX_DEF.strAttr("name");
  FieldKey<SQLNode> INDEX_DEF_TABLE = INDEX_DEF.nodeAttr("table");
  FieldKey<IndexType> INDEX_DEF_TYPE = INDEX_DEF.attr("type", IndexType.class);
  FieldKey<ConstraintType> INDEX_DEF_CONS = INDEX_DEF.attr("constraint", ConstraintType.class);
  FieldKey<List<SQLNode>> INDEX_DEF_KEYS = INDEX_DEF.nodesAttr("keys");
  FieldKey<SQLNode> INDEX_DEF_REFS = INDEX_DEF.nodeAttr("references");

  //// KeyPart
  FieldKey<String> KEY_PART_COLUMN = KEY_PART.strAttr("column");
  FieldKey<Integer> KEY_PART_LEN = KEY_PART.attr("length", Integer.class);
  FieldKey<SQLNode> KEY_PART_EXPR = KEY_PART.nodeAttr("expr");
  FieldKey<KeyDirection> KEY_PART_DIRECTION = KEY_PART.attr("direction", KeyDirection.class);

  //// Union
  FieldKey<SQLNode> SET_OP_LEFT = SET_OP.nodeAttr("left");
  FieldKey<SQLNode> SET_OP_RIGHT = SET_OP.nodeAttr("right");
  FieldKey<SetOperation> SET_OP_TYPE = SET_OP.attr("type", SetOperation.class);
  FieldKey<SetOperationOption> SET_OP_OPTION = SET_OP.attr("option", SetOperationOption.class);

  //// Query
  FieldKey<SQLNode> QUERY_BODY = QUERY.nodeAttr("body");
  FieldKey<List<SQLNode>> QUERY_ORDER_BY = QUERY.nodesAttr("orderBy");
  FieldKey<SQLNode> QUERY_LIMIT = QUERY.nodeAttr("limit");
  FieldKey<SQLNode> QUERY_OFFSET = QUERY.nodeAttr("offset");

  //// QuerySpec
  FieldKey<Boolean> QUERY_SPEC_DISTINCT = QUERY_SPEC.boolAttr("distinct");
  FieldKey<List<SQLNode>> QUERY_SPEC_DISTINCT_ON = QUERY_SPEC.nodesAttr("distinctOn");
  FieldKey<List<SQLNode>> QUERY_SPEC_SELECT_ITEMS = QUERY_SPEC.nodesAttr("items");
  FieldKey<SQLNode> QUERY_SPEC_FROM = QUERY_SPEC.nodeAttr("from");
  FieldKey<SQLNode> QUERY_SPEC_WHERE = QUERY_SPEC.nodeAttr("where");
  FieldKey<List<SQLNode>> QUERY_SPEC_GROUP_BY = QUERY_SPEC.nodesAttr("groupBy");
  FieldKey<OLAPOption> QUERY_SPEC_OLAP_OPTION = QUERY_SPEC.attr("olapOption", OLAPOption.class);
  FieldKey<SQLNode> QUERY_SPEC_HAVING = QUERY_SPEC.nodeAttr("having");
  FieldKey<List<SQLNode>> QUERY_SPEC_WINDOWS = QUERY_SPEC.nodesAttr("windows");

  //// SelectItem
  FieldKey<SQLNode> SELECT_ITEM_EXPR = SELECT_ITEM.nodeAttr("expr");
  FieldKey<String> SELECT_ITEM_ALIAS = SELECT_ITEM.strAttr("alias");

  //// OrderItem
  FieldKey<SQLNode> ORDER_ITEM_EXPR = ORDER_ITEM.nodeAttr("expr");
  FieldKey<KeyDirection> ORDER_ITEM_DIRECTION = ORDER_ITEM.attr("direction", KeyDirection.class);

  //// GroupItem
  FieldKey<SQLNode> GROUP_ITEM_EXPR = GROUP_ITEM.nodeAttr("expr");

  //// WindowSpec
  FieldKey<String> WINDOW_SPEC_ALIAS = WINDOW_SPEC.strAttr("alias");
  FieldKey<String> WINDOW_SPEC_NAME = WINDOW_SPEC.strAttr("name");
  FieldKey<List<SQLNode>> WINDOW_SPEC_PARTITION = WINDOW_SPEC.nodesAttr("partition");
  FieldKey<List<SQLNode>> WINDOW_SPEC_ORDER = WINDOW_SPEC.nodesAttr("order");
  FieldKey<SQLNode> WINDOW_SPEC_FRAME = WINDOW_SPEC.nodeAttr("frame");

  //// WindowFrame
  FieldKey<WindowUnit> WINDOW_FRAME_UNIT = WINDOW_FRAME.attr("unit", WindowUnit.class);
  FieldKey<SQLNode> WINDOW_FRAME_START = WINDOW_FRAME.nodeAttr("start");
  FieldKey<SQLNode> WINDOW_FRAME_END = WINDOW_FRAME.nodeAttr("end");
  FieldKey<WindowExclusion> WINDOW_FRAME_EXCLUSION =
      WINDOW_FRAME.attr("exclusion", WindowExclusion.class);

  //// FrameBound
  FieldKey<SQLNode> FRAME_BOUND_EXPR = FRAME_BOUND.nodeAttr("expr");
  FieldKey<FrameBoundDirection> FRAME_BOUND_DIRECTION =
      FRAME_BOUND.attr("direction", FrameBoundDirection.class);

  //// IndexHint
  FieldKey<IndexHintType> INDEX_HINT_TYPE = INDEX_HINT.attr("type", IndexHintType.class);
  FieldKey<IndexHintTarget> INDEX_HINT_TARGET = INDEX_HINT.attr("target", IndexHintTarget.class);
  FieldKey<List<String>> INDEX_HINT_NAMES = INDEX_HINT.attr("names", List.class);

  //// Statement
  FieldKey<StmtType> STATEMENT_TYPE = STATEMENT.attr("type", StmtType.class);
  FieldKey<SQLNode> STATEMENT_BODY = STATEMENT.nodeAttr("body");

  //// AlterSequence
  FieldKey<SQLNode> ALTER_SEQUENCE_NAME = ALTER_SEQUENCE.nodeAttr("name");
  FieldKey<String> ALTER_SEQUENCE_OPERATION = ALTER_SEQUENCE.strAttr("operation");
  FieldKey<Object> ALTER_SEQUENCE_PAYLOAD = ALTER_SEQUENCE.attr("payload", Object.class);

  //// AlterTable
  FieldKey<SQLNode> ALTER_TABLE_NAME = ALTER_TABLE.nodeAttr("name");
  FieldKey<List<SQLNode>> ALTER_TABLE_ACTIONS = ALTER_TABLE.nodesAttr("actions");

  //// AlterTableAction
  FieldKey<String> ALTER_TABLE_ACTION_NAME = ALTER_TABLE_ACTION.strAttr("name");
  FieldKey<Object> ALTER_TABLE_ACTION_PAYLOAD = ALTER_TABLE_ACTION.attr("payload", Object.class);

  //// Expr
  FieldKey<ExprType> EXPR_KIND = EXPR.attr("kind", ExprType.class);
  // for named argument in PG
  FieldKey<String> EXPR_FUNC_ARG_NAME = EXPR.strAttr("argName");
  FieldKey<Boolean> EXPR_FUNC_ARG_VARIADIC = EXPR.boolAttr("variadic");

  //// TableSource
  FieldKey<TableSourceType> TABLE_SOURCE_KIND = TABLE_SOURCE.attr("kind", TableSourceType.class);
}