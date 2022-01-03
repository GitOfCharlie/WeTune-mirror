package sjtu.ipads.wtune.sql.schema;

import sjtu.ipads.wtune.sql.ast1.SqlDataType;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.constants.Category;
import sjtu.ipads.wtune.sql.util.ASTHelper;

import java.util.*;

import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.schema.Column.Flag.*;
import static sjtu.ipads.wtune.sql.util.ASTHelper.simpleName;

class ColumnImpl implements Column {
  private final String table;
  private final String name;
  private final String rawDataType;
  private final SqlDataType dataType;
  private final EnumSet<Flag> flags;
  private List<Constraint> constraints;
  private List<SchemaPatch> patches;

  ColumnImpl(String table, String name, String rawDataType, SqlDataType dataType) {
    this.table = table;
    this.name = name;
    this.rawDataType = rawDataType;
    this.dataType = dataType;
    this.flags = EnumSet.noneOf(Flag.class);

    if (dataType.category() == Category.BOOLEAN) flags.add(IS_BOOLEAN);
    else if (dataType.category() == Category.ENUM) flags.add(IS_ENUM);
  }

  static ColumnImpl build(String table, SqlNode colDef) {
    final String colName = simpleName(colDef.$(ColDef_Name).$(ColName_Col));
    final String rawDataType = colDef.$(ColDef_RawType);
    final SqlDataType dataType = colDef.$(ColDef_DataType);

    final ColumnImpl column = new ColumnImpl(table, colName, rawDataType, dataType);

    if (colDef.isFlag(ColDef_Generated)) column.flag(GENERATED);
    if (colDef.isFlag(ColDef_Default)) column.flag(HAS_DEFAULT);
    if (colDef.isFlag(ColDef_AutoInc)) column.flag(AUTO_INCREMENT);

    return column;
  }

  void flag(Flag... flags) {
    this.flags.addAll(Arrays.asList(flags));
  }

  @Override
  public String tableName() {
    return table;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String rawDataType() {
    return rawDataType;
  }

  @Override
  public SqlDataType dataType() {
    return dataType;
  }

  @Override
  public Collection<Constraint> constraints() {
    return constraints == null ? Collections.emptyList() : constraints;
  }

  @Override
  public Collection<SchemaPatch> patches() {
    return patches == null ? Collections.emptyList() : patches;
  }

  @Override
  public boolean isFlag(Flag flag) {
    return flags.contains(flag);
  }

  void addConstraint(Constraint constraint) {
    if (constraints == null) constraints = new ArrayList<>(3);
    constraints.add(constraint);
    if (constraint.kind() == null) flags.add(INDEXED);
    else
      switch (constraint.kind()) {
        case PRIMARY:
          flags.add(PRIMARY);
          flags.add(NOT_NULL);
        case UNIQUE:
          flags.add(UNIQUE);
          flags.add(INDEXED);
          break;
        case NOT_NULL:
          flags.add(NOT_NULL);
          break;
        case FOREIGN:
          flags.add(FOREIGN_KEY);
          flags.add(INDEXED);
          break;
        case CHECK:
          flags.add(HAS_CHECK);
          break;
      }
  }

  void addPatch(SchemaPatch patch) {
    if (patches == null) patches = new ArrayList<>(2);
    patches.add(patch);
    switch (patch.type()) {
      case NOT_NULL -> flags.add(NOT_NULL);
      case INDEX -> flags.add(INDEXED);
      case BOOLEAN -> flags.add(IS_BOOLEAN);
      case ENUM -> flags.add(IS_ENUM);
      case UNIQUE -> flags.add(UNIQUE);
      case FOREIGN_KEY -> flags.add(FOREIGN_KEY);
    }
  }

  @Override public String toString() {
    return table + "." + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnImpl column = (ColumnImpl) o;
    return Objects.equals(table, column.table) && Objects.equals(name, column.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, name);
  }

  @Override
  public StringBuilder toDdl(String dbType, StringBuilder buffer) {
     buffer.append(ASTHelper.quoted(dbType, name)).append(' ').append(dataType.toString());
     if (isFlag(PRIMARY)) buffer.append(' ').append("PRIMARY KEY");
     else if (isFlag(UNIQUE)) buffer.append(' ').append("UNIQUE");
     if (isFlag(NOT_NULL) && !isFlag(PRIMARY)) buffer.append(' ').append("NOT NULL");
     return buffer;
  }
}