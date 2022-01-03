package sjtu.ipads.wtune.sql.pg;

import static sjtu.ipads.wtune.sql.ast.ASTNode.POSTGRESQL;

import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sql.ASTContext;
import sjtu.ipads.wtune.sql.ASTParser;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.pg.internal.PGLexer;
import sjtu.ipads.wtune.sql.pg.internal.PGParser;

public class PGASTParser implements ASTParser {
  public static boolean IS_ERROR_MUTED = false;

  public <T extends ParserRuleContext> T parse0(String str, Function<PGParser, T> rule) {
    final PGLexer lexer = new PGLexer(CharStreams.fromString(str));
    final PGParser parser = new PGParser(new CommonTokenStream(lexer));
    if (IS_ERROR_MUTED) {
      lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
      parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
    }

    //    lexer.getInterpreter().clearDFA();
    //    parser.getInterpreter().clearDFA();
    return rule.apply(parser);
  }

  public ASTNode parse(String str, Function<PGParser, ParserRuleContext> rule) {
    return parse(str, true, rule);
  }

  public ASTNode parse(String str, boolean managed, Function<PGParser, ParserRuleContext> rule) {
    try {
      final ASTNode root = parse0(str, rule).accept(new PGASTBuilder());
      if (root != null && managed) {
        ASTContext.manage(root, ASTContext.build());
        root.context().setDbType(POSTGRESQL);
      }
      return root;

    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public ASTNode parse(String string, boolean managed) {
    return parse(string, managed, PGParser::statement);
  }
}