package sjtu.ipads.wtune.sqlparser.schema.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.FOREIGN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection.ASC;

class TableBuilder {
  private final TableImpl table;

  private TableBuilder(TableImpl table) {
    this.table = table;
  }

  static TableBuilder fromCreateTable(ASTNode tableDef) {
    final TableBuilder builder = new TableBuilder(TableImpl.build(tableDef));

    tableDef.get(CREATE_TABLE_COLUMNS).forEach(builder::setColumn);
    tableDef.get(CREATE_TABLE_CONSTRAINTS).forEach(builder::setConstraint);

    return builder;
  }

  TableBuilder fromAlterTable(ASTNode alterTable) {
    for (ASTNode action : alterTable.get(ALTER_TABLE_ACTIONS))
      switch (action.get(ALTER_TABLE_ACTION_NAME)) {
        case "add_constraint" -> setConstraint((ASTNode) action.get(ALTER_TABLE_ACTION_PAYLOAD));
        case "modify_column" -> setColumn((ASTNode) action.get(ALTER_TABLE_ACTION_PAYLOAD));
      }
    return this;
  }

  TableBuilder fromCreateIndex(ASTNode createIndex) {
    setConstraint(createIndex);
    return this;
  }

  TableImpl table() {
    return table;
  }

  private void setColumn(ASTNode colDef) {
    final ColumnImpl column = ColumnImpl.build(table.name(), colDef);
    table.addColumn(column);

    final EnumSet<ConstraintType> constraints = colDef.get(COLUMN_DEF_CONS);
    if (constraints != null)
      for (ConstraintType cType : constraints) {
        final ConstraintImpl c = ConstraintImpl.build(cType, singletonList(column));

        table.addConstraint(c);
        column.addConstraint(c);
      }

    final ASTNode references = colDef.get(COLUMN_DEF_REF);
    if (references != null) {
      final ConstraintImpl c = ConstraintImpl.build(FOREIGN, singletonList(column));
      c.setRefTableName(references.get(REFERENCES_TABLE));
      c.setRefColNames(references.get(REFERENCES_COLUMNS));

      table.addConstraint(c);
      column.addConstraint(c);
    }
  }

  private void setConstraint(ASTNode constraintDef) {
    final List<ASTNode> keys = constraintDef.get(INDEX_DEF_KEYS);
    final List<Column> columns = new ArrayList<>(keys.size());
    final List<KeyDirection> directions = new ArrayList<>(keys.size());
    for (ASTNode key : keys) {
      final String columnName = key.get(KEY_PART_COLUMN);
      final ColumnImpl column = table.column(columnName);
      if (column == null) return;
      columns.add(column);
      directions.add(coalesce(key.get(KEY_PART_DIRECTION), ASC));
    }

    final ConstraintImpl c = ConstraintImpl.build(constraintDef.get(INDEX_DEF_CONS), columns);
    c.setIndexType(constraintDef.get(INDEX_DEF_TYPE));
    c.setDirections(directions);

    final ASTNode refs = constraintDef.get(INDEX_DEF_REFS);
    if (refs != null) {
      c.setRefTableName(refs.get(REFERENCES_TABLE));
      c.setRefColNames(refs.get(REFERENCES_COLUMNS));
    }

    columns.forEach(col -> ((ColumnImpl) col).addConstraint(c));
    table.addConstraint(c);
  }
}
