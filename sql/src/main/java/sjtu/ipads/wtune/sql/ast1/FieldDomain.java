package sjtu.ipads.wtune.sql.ast1;

import sjtu.ipads.wtune.common.field.FieldKey;

import java.util.List;

interface FieldDomain {
  String name();

  boolean isInstance(SqlNode node);

  List<FieldKey<?>> fields();

  <T, R extends T> FieldKey<R> field(String name, Class<T> clazz);

  default FieldKey<String> textField(String name) {
    return field(name, String.class);
  }

  default FieldKey<Boolean> boolField(String name) {
    return field(name, Boolean.class);
  }

  default FieldKey<SqlNode> nodeField(String name) {
    return field(name, SqlNode.class);
  }

  default FieldKey<SqlNodes> nodesField(String name) {
    return field(name, SqlNodes.class);
  }
}