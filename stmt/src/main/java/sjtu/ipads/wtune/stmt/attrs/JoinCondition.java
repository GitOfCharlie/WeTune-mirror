package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.binary;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.columnRef;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class JoinCondition {
  private final Relation left;
  private final Relation right;
  private String leftColumn;
  private String rightColumn;

  private JoinCondition(Relation left, Relation right, String leftColumn, String rightColumn) {
    this.left = left;
    this.right = right;
    this.leftColumn = leftColumn;
    this.rightColumn = rightColumn;
  }

  public static JoinCondition of(
      Relation left, Relation right, String leftColumn, String rightColumn) {
    assert left != null && right != null && leftColumn != null && rightColumn != null;

    return new JoinCondition(left, right, leftColumn, rightColumn);
  }

  public Relation left() {
    return left;
  }

  public Relation right() {
    return right;
  }

  public String leftColumn() {
    return leftColumn;
  }

  public String rightColumn() {
    return rightColumn;
  }

  public void setLeftColumn(String leftColumn) {
    this.leftColumn = leftColumn;
  }

  public void setRightColumn(String rightColumn) {
    this.rightColumn = rightColumn;
  }

  public Relation thisRelation(Relation relation) {
    if (left.equals(relation)) return left;
    else if (right.equals(relation)) return right;
    else return null;
  }

  public Relation thatRelation(Relation relation) {
    if (left.equals(relation)) return right;
    else if (right.equals(relation)) return left;
    else return null;
  }

  public String thisColumn(Relation relation) {
    if (left.equals(relation)) return leftColumn;
    else if (right.equals(relation)) return rightColumn;
    else return null;
  }

  public String thatColumn(Relation relation) {
    if (left.equals(relation)) return rightColumn;
    else if (right.equals(relation)) return leftColumn;
    else return null;
  }

  @Override
  public String toString() {
    return String.format("<%s.%s = %s.%s>", left.name(), leftColumn, right.name(), rightColumn);
  }
}
