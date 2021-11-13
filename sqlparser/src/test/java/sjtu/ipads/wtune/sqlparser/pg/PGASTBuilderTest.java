package sjtu.ipads.wtune.sqlparser.pg;

import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast1.SqlDataType;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.constants.Category;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.ColDef_DataType;

class PGASTBuilderTest {
  private static final PgAstParser1 PARSER = new PgAstParser1();

  private static class TestHelper {
    private String sql;
    private SqlNode node;
    private final Function<PGParser, ParserRuleContext> rule;

    private TestHelper(Function<PGParser, ParserRuleContext> rule) {
      this.rule = rule;
    }

    private SqlNode sql(String sql) {
      if (sql != null) return (node = PARSER.parse(sql, rule));
      return null;
    }
  }

  @Test
  void testCharacterString() {
    final TestHelper helper = new TestHelper(PGParser::table_column_definition);
    final SqlNode node = helper.sql("\"a\" character varying [][]");
    final SqlDataType dataType = node.$(ColDef_DataType);
    assertEquals(Category.STRING, dataType.category());
    assertArrayEquals(new int[] {0, 0}, dataType.dimensions());
  }
}
