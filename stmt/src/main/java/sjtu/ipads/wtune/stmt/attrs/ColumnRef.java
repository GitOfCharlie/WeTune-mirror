package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeHash;

public class ColumnRef {
  private SQLNode node;
  private TableSource source;
  private Column refColumn;
  private SelectItem refItem;
  private boolean isDependent;

  public ColumnRef() {}

  public SQLNode node() {
    return node;
  }

  public TableSource source() {
    return source;
  }

  public Column refColumn() {
    return refColumn;
  }

  public SelectItem refItem() {
    return refItem;
  }

  public boolean isDependent() {
    return isDependent;
  }

  public ColumnRef setNode(SQLNode node) {
    this.node = node;
    return this;
  }

  public void setSource(TableSource source) {
    this.source = source;
  }

  public void setRefColumn(Column refColumn) {
    this.refColumn = refColumn;
  }

  public ColumnRef setRefItem(SelectItem refItem) {
    this.refItem = refItem;
    return this;
  }

  private ColumnRef nextLevel() {
    if (refItem == null || refItem.expr() == null) return null;
    return refItem.expr().get(RESOLVED_COLUMN_REF);
  }

  public ColumnRef resolveRootRef() {
    if (refColumn != null) return this;
    final ColumnRef columnRef = nextLevel();
    return columnRef != null ? columnRef.resolveRootRef() : this;
  }

  public Column resolveAsColumn() {
    final ColumnRef rootRef = resolveRootRef();
    if (rootRef != null) return rootRef.refColumn();
    else return null;
  }

  public boolean isFrom(TableSource source) {
    if (this.source != null && this.source.equals(source)) return true;
    final ColumnRef nextLevel = nextLevel();
    if (nextLevel != null) return nextLevel.isFrom(source);
    else return false;
  }

  public void setDependent(boolean dependent) {
    isDependent = dependent;
  }

  public boolean refEquals(ColumnRef other) {
    if (other == null) return false;
    final ColumnRef thisRoot = resolveRootRef();
    final ColumnRef thatRoot = other.resolveRootRef();

    return Objects.equals(thisRoot.source(), thatRoot.source())
        && Objects.equals(thisRoot.refColumn(), thatRoot.refColumn())
        && Objects.equals(thisRoot.refItem(), thatRoot.refItem());
  }

  public int refHash() {
    final ColumnRef root = resolveRootRef();
    return Objects.hash(root.source(), root.refColumn(), root.refItem());
  }

  public void putColumnName(String name) {
    node.get(COLUMN_REF_COLUMN).put(COLUMN_NAME_COLUMN, name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnRef other = (ColumnRef) o;
    return nodeEquals(node, other.node);
  }

  @Override
  public int hashCode() {
    return nodeHash(node);
  }

  @Override
  public String toString() {
    final ColumnRef rootRef = resolveRootRef();
    if (rootRef == this) return String.format("{%s | %s}", node, source);
    else return String.format("{%s < %s}", node, rootRef);
  }

  private Identity identity;

  public Identity identity() {
    return identity != null ? identity : (identity = new Identity(this));
  }

  public static class Identity {
    private final ColumnRef ref;

    private Identity(ColumnRef ref) {
      this.ref = ref;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Identity identity = (Identity) o;
      return ref.refEquals(identity.ref);
    }

    @Override
    public int hashCode() {
      return ref.refHash();
    }
  }
}
