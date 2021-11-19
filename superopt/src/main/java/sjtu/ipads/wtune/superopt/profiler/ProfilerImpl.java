package sjtu.ipads.wtune.superopt.profiler;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier;
import sjtu.ipads.wtune.stmt.resolver.Params;
import sjtu.ipads.wtune.stmt.resolver.Resolution;
import sjtu.ipads.wtune.superopt.util.Complexity;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static sjtu.ipads.wtune.common.utils.Commons.elemAt;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.SQLSERVER;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.PARAM_MARKER;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.*;

class ProfilerImpl implements Profiler {
  private final Properties dbProps;
  private PlanNode baseline;
  private double baseCost;
  private final List<PlanNode> plans;
  private final TDoubleList costs;

  ProfilerImpl(Properties dbProps) {
    this.dbProps = dbProps;
    this.plans = new ArrayList<>();
    this.costs = new TDoubleArrayList();
  }

  @Override
  public void setBaseline(PlanNode baseline) {
    plans.clear();
    costs.clear();
    this.baseline = baseline;
    this.baseCost = queryCost(translateAsAst(baseline));
  }

  @Override
  public void profile(PlanNode plan) {
    if (plan == null) {
      plans.add(null);
      costs.add(Double.MAX_VALUE);
      return;
    }

    final ASTNode ast = translateAsAst(plan);
    final double cost = queryCost(ast);

    plans.add(plan);
    costs.add(cost);
  }

  @Override
  public PlanNode getPlan(int index) {
    return plans.get(index);
  }

  @Override
  public double getCost(int index) {
    return costs.get(index);
  }

  @Override
  public int minCostIndex() {
    double minCost = baseCost;
    int minCostIndex = -1;
    for (int i = 0, bound = costs.size(); i < bound; ++i) {
      if (costs.get(i) < minCost) {
        minCost = costs.get(i);
        minCostIndex = i;
      }
    }

    // MySQL doesn't correctly estimate some simplification (e.g. remove JOIN),
    // so let's do it ourself.
    if (minCostIndex == -1 && MYSQL.equals(dbProps.getProperty("dbType"))) {
      Complexity minComplexity = Complexity.mk(baseline);
      for (int i = 0, bound = plans.size(); i < bound; i++) {
        final PlanNode plan = plans.get(i);
        if (plan == null) continue;
        final Complexity complexity = Complexity.mk(plan);
        if (minComplexity.compareTo(complexity, false) > 0) {
          minComplexity = complexity;
          minCostIndex = i;
        }
      }
    }

    return minCostIndex;
  }

  private double queryCost(ASTNode ast) {
    String query = ast.toString();
    if (query.contains("?") || query.contains("$")) {
      final List<ASTNode> filled = fillParamMarker(ast);
      query = ast.toString();
      unFillParamMarker(filled);
    }

    final String dbType = dbProps.getProperty("dbType");
    if (SQLSERVER.equals(dbType)) query = adaptToSqlserver(query);

    final DataSource dataSource = DataSourceFactory.instance().mk(dbProps);
    return CostQuery.mk(dbType, dataSource::getConnection, query).getCost();
  }

  private static List<ASTNode> fillParamMarker(ASTNode ast) {
    final Params mgr = Resolution.resolveParamFull(ast);
    final List<ASTNode> filled = new ArrayList<>();
    for (ParamDesc param : mgr.params()) {
      final ASTNode node = param.node();
      if (!PARAM_MARKER.isInstance(node)) continue;

      ParamModifier modifier = tail(param.modifiers());
      if (modifier == null) continue;

      ASTNode value;

      if (modifier.type() == OFFSET_VAL) value = fillOffset();
      else if (modifier.type() == LIMIT_VAL) value = fillLimit();
      else {
        if (modifier.type() == TUPLE_ELEMENT || modifier.type() == ARRAY_ELEMENT)
          modifier = elemAt(param.modifiers(), -2);
        if (modifier == null || modifier.type() != COLUMN_VALUE) continue;

        final Column column = (Column) modifier.args()[1];
        assert column != null;
        value = fillColumnValue(column);
      }

      node.update(value);
      filled.add(node);
    }
    return filled;
  }

  private static ASTNode fillLimit() {
    final ASTNode value = ASTNode.expr(LITERAL);
    value.set(LITERAL_TYPE, LiteralType.INTEGER);
    value.set(LITERAL_VALUE, 100);
    return value;
  }

  private static ASTNode fillOffset() {
    final ASTNode value = ASTNode.expr(LITERAL);
    value.set(LITERAL_TYPE, LiteralType.INTEGER);
    value.set(LITERAL_VALUE, 0);
    return value;
  }

  private static ASTNode fillColumnValue(Column column) {
    final ASTNode value = ASTNode.expr(LITERAL);
    switch (column.dataType().category()) {
      case INTEGRAL -> {
        value.set(LITERAL_TYPE, LiteralType.INTEGER);
        value.set(LITERAL_VALUE, 1);
      }
      case FRACTION -> {
        value.set(LITERAL_TYPE, LiteralType.FRACTIONAL);
        value.set(LITERAL_VALUE, 1.0);
      }
      case BOOLEAN -> {
        value.set(LITERAL_TYPE, LiteralType.BOOL);
        value.set(LITERAL_VALUE, false);
      }
      case STRING -> {
        value.set(LITERAL_TYPE, LiteralType.TEXT);
        value.set(LITERAL_VALUE, "00001");
      }
      case TIME -> {
        value.set(LITERAL_TYPE, LiteralType.TEXT);
        value.set(LITERAL_VALUE, "2021-01-01 00:00:00.000");
      }
      default -> value.set(LITERAL_TYPE, LiteralType.NULL);
    }
    return value;
  }

  private static void unFillParamMarker(List<ASTNode> filled) {
    for (ASTNode n : filled) {
      final ASTNode marker = ASTNode.expr(PARAM_MARKER);
      n.update(marker);
    }
  }

  private static String adaptToSqlserver(String sql) {
    sql = sql.replaceAll("`([A-Za-z0-9_]+)`", "\\[$1\\]");
    sql = sql.replaceAll("\"([A-Za-z0-9_]+)\"", "\\[$1\\]");

    sql = sql.replaceAll("TRUE", "1");
    sql = sql.replaceAll("FALSE", "0");

    sql =
        sql.replaceAll(
            "(\\(SELECT (DISTINCT)*)(.+)(ORDER BY .+(ASC|DESC)\\))", "$1 TOP 100 PERCENT $3 $4");

    sql =
        sql.replaceAll(
            "(ORDER BY [^()]+ )LIMIT ([0-9]+) OFFSET ([0-9]+)",
            "$1OFFSET $3 ROWS FETCH NEXT $2 ROWS ONLY");
    sql = sql.replaceAll("LIMIT ([0-9]+) OFFSET ([0-9]+)", "LIMIT $1");

    sql =
        sql.replaceAll(
            "\\(SELECT DISTINCT (.+) LIMIT ([0-9]+)\\)", "\\(SELECT DISTINCT TOP $2 $1\\)");
    sql = sql.replaceAll("\\(SELECT (.+) LIMIT ([0-9]+)\\)", "\\(SELECT TOP $2 $1\\)");
    sql = sql.replaceFirst("SELECT DISTINCT (.+)LIMIT ([0-9]+)", "SELECT DISTINCT TOP $2 $1");
    sql = sql.replaceFirst("SELECT (.+)LIMIT ([0-9]+)", "SELECT TOP $2 $1");

    sql = sql.replaceAll("'FALSE'", "0");
    sql = sql.replaceAll("'TRUE'", "1");
    sql = sql.replaceAll("USE INDEX \\([^ ]*\\)", "");
    sql = sql.replaceAll("(\\[[A-Za-z0-9]+] \\* \\[[A-Za-z0-9]+])", "\\($1\\) AS total");
    sql = sql.replaceAll("ORDER BY [^ ]+ IS NULL,", "ORDER BY");

    return sql;
  }
}