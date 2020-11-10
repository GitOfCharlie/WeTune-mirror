package sjtu.ipads.wtune.sqlparser.pg;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.sqlparser.SQLParserException;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGLexer;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;

import java.util.Properties;
import java.util.function.Function;

public class PGASTParser implements SQLParser {

  public <T extends ParserRuleContext> T parse0(String str, Function<PGParser, T> rule) {
    final PGLexer lexer = new PGLexer(CharStreams.fromString(str));

    final PGParser parser = new PGParser(new CommonTokenStream(lexer));

    //    lexer.getInterpreter().clearDFA();
    //    parser.getInterpreter().clearDFA();
    return rule.apply(parser);
  }

  public SQLNode parse(String str, Function<PGParser, ParserRuleContext> rule) {
    final SQLNode node;
    try {
      node = parse0(str, rule).accept(new PGASTBuilder());
    } catch (SQLParserException ex) {
      return null;
    }

    if (node != null) {
      node.relinkAll();
      node.setDbTypeRec(SQLNode.POSTGRESQL);
    }
    return node;
  }

  @Override
  public SQLNode parse(String string) {
    return parse(string, PGParser::statement);
  }

  @Override
  public SQLNode parse(String string, Properties props) {
    return parse(string, PGParser::statement);
  }
}