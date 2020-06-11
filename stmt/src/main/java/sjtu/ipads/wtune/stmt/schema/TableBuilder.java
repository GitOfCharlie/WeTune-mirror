package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.orElse;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.StandardAttrs.RESOLVED_COLUMN;
import static sjtu.ipads.wtune.stmt.StandardAttrs.RESOLVED_TABLE;

class TableBuilder {
  private static System.Logger LOG = System.getLogger(TableBuilder.class.getSimpleName());

  private final Table table;

  TableBuilder(Table table) {
    this.table = requireNonNull(table);
  }

  Table fromCreateTable(SQLNode createTable) {
    if (createTable == null || createTable.type() != Type.CREATE_TABLE) {
      LOG.log(TRACE, "illegal statement: " + createTable);
      return table;
    }

    createTable.put(RESOLVED_TABLE, table);

    if (POSTGRESQL.equals(createTable.dbType())) {
      table.setEngine(POSTGRESQL);
    } else {
      table.setEngine(orElse(createTable.get(CREATE_TABLE_ENGINE), "innodb"));
    }

    setName(createTable.get(CREATE_TABLE_NAME));

    createTable.get(CREATE_TABLE_COLUMNS).forEach(this::setColumn);
    createTable.get(CREATE_TABLE_COLUMNS).forEach(this::setConstraintFromColumnDef);
    createTable.get(CREATE_TABLE_CONSTRAINTS).forEach(this::setConstraint);

    return table;
  }

  private void setName(SQLNode name) {
    table.setSchemaName(name.get(TABLE_NAME_SCHEMA));
    table.setTableName(name.get(TABLE_NAME_TABLE));
  }

  private void setColumn(SQLNode column) {
    table.addColumn(ColumnBuilder.fromColumnDef(column));
  }

  private void setConstraintFromColumnDef(SQLNode colDef) {
    final Column column = colDef.get(RESOLVED_COLUMN);
    final EnumSet<ConstraintType> constraints = colDef.get(COLUMN_DEF_CONS);

    for (ConstraintType cType : constraints) {
      final Constraint c = new Constraint();
      c.setType(cType);
      c.setColumns(singletonList(column));

      table.addConstraint(c);
      column.addConstraint(c);
    }

    final SQLNode references = colDef.get(COLUMN_DEF_REF);
    if (references != null) {
      final Constraint c = new Constraint();
      c.setType(ConstraintType.FOREIGN);
      c.setColumns(singletonList(column));
      c.setRefTable(references.get(REFERENCES_TABLE));
      c.setRefColNames(references.get(REFERENCES_COLUMNS));

      table.addConstraint(c);
      column.addConstraint(c);
    }
  }

  private void setConstraint(SQLNode constraintDef) {
    final Constraint c = new Constraint();
    c.setType(constraintDef.get(INDEX_DEF_CONS));
    c.setIndexType(constraintDef.get(INDEX_DEF_TYPE));

    final List<SQLNode> keys = constraintDef.get(INDEX_DEF_KEYS);
    final List<Column> columns = new ArrayList<>(keys.size());
    final List<KeyDirection> directions = new ArrayList<>(keys.size());

    for (SQLNode key : keys) {
      final String columnName = key.get(KEY_PART_COLUMN);
      final Column column = table.getColumn(columnName);
      if (column == null) {
        LOG.log(
            WARNING,
            "invalid column {0} in constraint definition: {1}",
            columnName,
            constraintDef.toString());
        return;
      }

      columns.add(column);
      directions.add(orElse(key.get(KEY_PART_DIRECTION), KeyDirection.ASC));
    }

    final SQLNode refs = constraintDef.get(INDEX_DEF_REFS);
    if (refs != null) {
      c.setRefTable(refs.get(REFERENCES_TABLE));
      c.setRefColNames(refs.get(REFERENCES_COLUMNS));
    }

    c.setColumns(columns);
    c.setDirections(directions);

    columns.forEach(col -> col.addConstraint(c));
    table.addConstraint(c);
  }
}