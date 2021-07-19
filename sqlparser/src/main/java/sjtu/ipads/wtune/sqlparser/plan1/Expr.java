package sjtu.ipads.wtune.sqlparser.plan1;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;

/**
 * Represents an arbitrary scalar expression.
 *
 * <p>1. {@link #refs()} are the columns used in the expression.<br>
 * {@link #template()} is the AST with all the refs replaced by placeholders.
 *
 * <p><b>Example</b>
 *
 * <p>users.id = 1<br>
 * &nbsp;=> refs: [user.id], template: ?.? = 1
 *
 * <p>users.salary + 1000<br>
 * &nbsp;=> refs: [users.salary], template: ?.? + 1000
 *
 * <p>2. {@link #interpolate(List<Value>)} interpolate the placeholder with the values.
 *
 * <p><b>Example</b><br>
 * Expr(user.id = 1).interpolate([user_role.user_id])<br>
 * &nbsp;=> Expr(user_role.user_id = 1)
 *
 * <p>3. Two {@link Expr}s are equal if their templates are identical.
 */
public interface Expr {
  RefBag refs();

  ASTNode template();

  ASTNode interpolate(List<Value> values);

  void setRefs(RefBag refs);

  default void setRefs(List<Ref> refs) {
    setRefs(RefBag.mk(refs));
  }

  default boolean isIdentity() {
    return COLUMN_REF.isInstance(template());
  }

  default boolean isJoinCondition() {
    final ASTNode template = template();
    return template.get(BINARY_OP) == BinaryOp.EQUAL
        && COLUMN_REF.isInstance(template.get(BINARY_LEFT))
        && COLUMN_REF.isInstance(template.get(BINARY_RIGHT));
  }

  default boolean isEquiCondition() {
    final ASTNode template = template();
    final ASTNode lhs = template.get(BINARY_LEFT);
    final ASTNode rhs = template.get(BINARY_RIGHT);
    final BinaryOp op = template.get(BINARY_OP);
    return (op == BinaryOp.EQUAL || op == BinaryOp.IS)
        && ((COLUMN_REF.isInstance(lhs) && LITERAL.isInstance(rhs))
            || (COLUMN_REF.isInstance(rhs) && LITERAL.isInstance(lhs)));
  }

  default ASTNode interpolate(PlanContext ctx) {
    return interpolate(ctx.deRef(refs()));
  }
}
