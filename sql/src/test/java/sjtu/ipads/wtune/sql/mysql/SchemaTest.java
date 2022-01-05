package sjtu.ipads.wtune.sql.mysql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.sql.schema.Constraint;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.schema.Table;

import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.ast.constants.ConstraintKind.FOREIGN;

public class SchemaTest {
  @Test
  @DisplayName("[Stmt.Schema] from CREATE TABLE")
  void test() {
    final String createTable =
        "create table `public`.t ("
            + "`i` int(10) primary key references b(x),"
            + "j varchar(512) NOT NULL DEFAULT 'a',"
            + "k int AUTO_INCREMENT CHECK (k < 100),"
            + "index (j(100)),"
            + "unique (j DESC) using rtree,"
            + "constraint fk_cons foreign key fk (k) references b(y)"
            + ") ENGINE = 'myisam';"
            + "create table b (x int(10), y int);";

    final Schema schema = SqlSupport.parseSchema(MySQL, createTable);

    assertEquals(2, schema.tables().size());

    final Table table0 = schema.table("t");
    final Table table1 = schema.table("b");
    {
      final Column col0 = table0.column("i");
      final Constraint constraint = getOnlyElement(col0.constraints(FOREIGN));
      assertSame(table1, constraint.refTable());

      final List<Column> refCols = constraint.refColumns();
      assertSame(table1.column("x"), refCols.get(0));
    }
    {
      final Column col0 = table0.column("k");
      final Constraint constraint = getOnlyElement(col0.constraints(FOREIGN));
      assertSame(table1, constraint.refTable());

      final List<Column> refCols = constraint.refColumns();
      assertSame(table1.column("y"), refCols.get(0));
    }
  }
}
