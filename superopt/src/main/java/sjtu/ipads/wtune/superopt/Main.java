package sjtu.ipads.wtune.superopt;

import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.LogManager;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.fragment.ToASTTranslator;
import sjtu.ipads.wtune.superopt.internal.OptimizerRunner;
import sjtu.ipads.wtune.superopt.internal.ProofRunner;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;

public class Main {

  private static final String LOGGER_CONFIG =
      ".level = INFO\n"
          + "java.util.logging.ConsoleHandler.level = INFO\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length >= 1 && "run".equals(args[0])) {
      ProofRunner.build(args).run();

    } else {
      //      final Statement stmt = Statement.findOne("broadleaf", 415);
      //      final ASTNode ast = stmt.parsed();
      //      ast.context().setSchema(stmt.app().schema("base", true));
      //      normalize(ast);
      //      System.out.println(ast.toString(false));
      //      test0();
      test1();
      //            cleanBank();
    }
  }

  private static void cleanBank() throws IOException {
    final SubstitutionBank bank =
        SubstitutionBank.make()
            .importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));
    try (final PrintWriter writer =
        new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "filtered_bank")))) {
      for (Substitution substitution : bank) {
        writer.println("====");
        writer.println(substitution);
      }
    }
  }

  private static void printReadableBank() throws IOException {
    final SubstitutionBank bank =
        SubstitutionBank.make()
            .importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));
    try (final PrintWriter writer =
        new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "readable_bank")))) {
      for (Substitution substitution : bank) {
        writer.println("====");
        final ToASTTranslator translator =
            ToASTTranslator.build().setNumbering(substitution.numbering());
        writer.println(translator.translate(substitution.g0()));
        writer.println(translator.translate(substitution.g1()));
        writer.println(substitution);
      }
    }
  }

  private static void test0() throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "filtered_bank"));
    final SubstitutionBank bank = SubstitutionBank.make().importFrom(lines, false);

    final String sql =
        "SELECT \"tags\".\"name\" AS \"name\" FROM \"tags\" AS \"tags\" INNER JOIN \"tag_group_memberships\" AS \"tag_group_memberships\" ON \"tags\".\"id\" = \"tag_group_memberships\".\"tag_id\" INNER JOIN \"tag_groups\" AS \"tag_groups\" ON \"tag_group_memberships\".\"tag_group_id\" = \"tag_groups\".\"id\" INNER JOIN \"tag_group_permissions\" AS \"tag_group_permissions\" ON \"tag_groups\".\"id\" = \"tag_group_permissions\".\"tag_group_id\" WHERE \"tag_group_permissions\".\"group_id\" = 0 AND \"tag_group_permissions\".\"permission_type\" = 3";
    //    final Statement stmt = Statement.findOne("diaspora", 460);
    final Statement stmt = Statement.findOne("guns", 60);

    final ASTNode ast = stmt.parsed();
    //    final ASTNode ast = ASTParser.mysql().parse(sql);
    //    final ASTNode ast = ASTParser.postgresql().parse(sql);
    final Schema schema = stmt.app().schema("base", true);
    //    final Schema schema = App.of("discourse").schema("base", true);
    ast.context().setSchema(schema);
    normalize(ast);

    System.out.println(stmt);
    System.out.println(ast.toString(false));

    //    final PlanNode plan = ToPlanTranslator.toPlan(ast);
    //    System.out.println(UniquenessInference.inferUniqueness(plan));
    final List<ASTNode> optimized = Optimizer.make(bank, schema).optimize(ast);
    System.out.println(optimized.size());

    for (ASTNode opt : optimized) System.out.println(opt);
    System.out.println(stmt);
  }

  private static void test1() throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "filtered_bank"));
    final SubstitutionBank bank = SubstitutionBank.make().importFrom(lines, false);
    final OptimizerRunner runner = new OptimizerRunner(bank);

    out = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "optimizations")));
    err = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "err")));

    App.all().forEach(it -> it.schema("base", true));

    Statement.findAll().parallelStream().forEach(it -> doOptimize(it, runner));
  }

  private static PrintWriter out, err;

  private static void doOptimize(Statement stmt, OptimizerRunner runner) {
    try {
      System.out.println(stmt);
      final List<ASTNode> optimized = runner.optimize(stmt);
      synchronized (out) {
        for (int i = 0; i < optimized.size(); i++)
          out.printf(
              "%s;%d;%d;%s;%s\n",
              stmt.appName(), stmt.stmtId(), i, stmt.parsed(), optimized.get(i));
        out.flush();
      }

    } catch (Throwable ex) {
      synchronized (err) {
        err.printf("======\n%s-%d\n", stmt.appName(), stmt.stmtId());
        ex.printStackTrace(err);
        err.flush();
      }
    }
  }
}
