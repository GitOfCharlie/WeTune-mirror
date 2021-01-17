package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;

import java.util.List;

public interface Constraint {
  List<? extends Column> columns();

  List<KeyDirection> directions();

  ConstraintType type();

  SQLNode refTableName();

  List<SQLNode> refColNames();

  default boolean isIndex() {
    return type() != ConstraintType.NOT_NULL && type() != ConstraintType.CHECK;
  }
}