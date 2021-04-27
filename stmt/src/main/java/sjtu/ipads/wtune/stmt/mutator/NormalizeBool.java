package sjtu.ipads.wtune.stmt.mutator;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.UNARY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.UNARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.EXISTS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.MATCH;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.TERNARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.UNARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp;
import sjtu.ipads.wtune.stmt.utils.BoolCollector;

class NormalizeBool {
  public static ASTNode normalize(ASTNode node) {
    BoolCollector.collect(node).forEach(NormalizeBool::normalizeExpr);
    return node;
  }

  private static void normalizeExpr(ASTNode expr) {
    assert EXPR.isInstance(expr);
    // `expr` must be evaluated as boolean

    if (COLUMN_REF.isInstance(expr)) {
      final ASTNode trueLiteral = ASTNode.expr(LITERAL);
      trueLiteral.set(LITERAL_TYPE, LiteralType.BOOL);
      trueLiteral.set(LITERAL_VALUE, true);

      final ASTNode binary = ASTNode.expr(BINARY);
      binary.set(BINARY_LEFT, expr.shallowCopy());
      binary.set(BINARY_OP, BinaryOp.EQUAL);
      binary.set(BINARY_RIGHT, trueLiteral);

      expr.update(binary);

    } else if (expr.get(BINARY_OP) == BinaryOp.IS) {
      final ASTNode right = expr.get(BINARY_RIGHT);
      if (LITERAL.isInstance(right) && right.get(LITERAL_TYPE) == LiteralType.BOOL) {
        expr.set(BINARY_OP, BinaryOp.EQUAL);

        if (right.get(LITERAL_VALUE).equals(Boolean.FALSE)) {
          normalizeExpr(expr.get(BINARY_LEFT));

          final ASTNode unary = ASTNode.expr(UNARY);
          unary.set(UNARY_OP, UnaryOp.NOT);
          unary.set(UNARY_EXPR, expr.get(BINARY_LEFT));
          expr.update(unary);
        }
      }

    } else if (BINARY.isInstance(expr) && expr.get(BINARY_OP).isLogic()) {
      normalizeExpr(expr.get(BINARY_LEFT));
      normalizeExpr(expr.get(BINARY_RIGHT));

    } else if (UNARY.isInstance(expr) && expr.get(UNARY_OP).isLogic())
      normalizeExpr(expr.get(UNARY_EXPR));

    final ExprKind exprKind = expr.get(EXPR_KIND);
    assert exprKind == UNARY
        || exprKind == BINARY
        || exprKind == TERNARY
        || exprKind == EXISTS
        || exprKind == MATCH
        || exprKind == COLUMN_REF
        || exprKind == LITERAL;
  }
}
