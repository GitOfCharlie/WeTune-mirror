package sjtu.ipads.wtune.sql.ast.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ASTContext;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.ASTVistor;
import sjtu.ipads.wtune.sql.ast.Formatter;
import sjtu.ipads.wtune.sql.ast.constants.ExprKind;
import sjtu.ipads.wtune.sql.ast.constants.NodeType;
import sjtu.ipads.wtune.sql.ast.constants.TableSourceKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.ListSupport.join;
import static sjtu.ipads.wtune.sql.ast.NodeFields.*;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.TABLE_SOURCE;

public class ASTNodeImpl implements ASTNode {
  private final Map<FieldKey, Object> directAttrs;
  private ASTContext context;

  private ASTNodeImpl(NodeType type) {
    this(type, new HashMap<>(8));
  }

  private ASTNodeImpl(NodeType type, Map<FieldKey, Object> directAttrs) {
    this.directAttrs = directAttrs;
    this.directAttrs.put(NODE_TYPE, type);
  }

  public static ASTNode build(NodeType nodeType) {
    return new ASTNodeImpl(nodeType);
  }

  @Override
  public Map<FieldKey, Object> directAttrs() {
    return directAttrs;
  }

  @Override
  public ASTContext context() {
    // don't use parent(), otherwise StackOverflow
    final ASTNode parent = FieldKey.get0(this, PARENT);
    // parent's context always overrider `this`'s context
    if (parent == null) return context;
    else return parent.context();
  }

  @Override
  public void setContext(ASTContext context) {
    this.context = context;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update(ASTNode other) {
    assert other instanceof ASTNodeImpl;
    final ASTNodeImpl otherNode = (ASTNodeImpl) other;

    // unset all current fields
    for (FieldKey fieldKey : fields0()) unset(fieldKey);

    // first set type & kind (otherwise, the other field are refused to be set)
    final NodeType nodeType = otherNode.nodeType();
    set(NODE_TYPE, nodeType);
    if (nodeType == EXPR) set(EXPR_KIND, otherNode.get(EXPR_KIND));
    if (nodeType == TABLE_SOURCE) set(TABLE_SOURCE_KIND, otherNode.get(TABLE_SOURCE_KIND));

    // set all fields from otherNode
    for (FieldKey fieldKey : otherNode.fields0()) set(fieldKey, otherNode.get(fieldKey));

    // extra care to ensure other.parent() != other
    final ASTNode parent = parent();
    if (parent == null) otherNode.unset(PARENT);
    else otherNode.set(PARENT, parent);
  }

  @Override
  public ASTNode shallowCopy() {
    return new ASTNodeImpl(nodeType(), new HashMap<>(fields()));
  }

  @Override
  public ASTNode deepCopy() {
    final ASTNodeImpl copy = new ASTNodeImpl(nodeType());
    for (FieldKey key : fields0()) {
      final Object value = deepCopy0(get(key));
      if (value != null) {
        FieldKey.set0(copy, key, value);
        ASTNode.setParent(value, copy);
      }
    }

    ASTContext.manage(copy, this.context());

    return copy;
  }

  @Override
  public void accept(ASTVistor visitor) {
    if (VisitorController.enter(this, visitor)) VisitorController.visitChildren(this, visitor);
    VisitorController.leave(this, visitor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ASTNodeImpl other = (ASTNodeImpl) o;

    final NodeType type = nodeType();
    if (other.nodeType() != type) return false;

    for (FieldKey fieldKey : fields0())
      if (!Objects.equals(this.get(fieldKey), other.get(fieldKey))) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hash = nodeType().hashCode();
    for (FieldKey fieldKey : fields0()) hash = hash * 31 + Objects.hashCode(get(fieldKey));

    return hash;
  }

  private Iterable<FieldKey> fields0() {
    // collect all fields that belongs to current node type/kind
    final List<FieldKey> nodeFields = nodeType().fields();

    final ExprKind exprKind = get(EXPR_KIND);
    final List<FieldKey> exprFields = exprKind == null ? emptyList() : exprKind.fields();

    final TableSourceKind sourceKind = get(TABLE_SOURCE_KIND);
    final List<FieldKey> sourceFields = sourceKind == null ? emptyList() : sourceKind.fields();

    return join(nodeFields, exprFields, sourceFields);
  }

  @Override
  public String toString() {
    return toString(true);
  }

  @Override
  public String toString(boolean oneline) {
    final Formatter formatter = new Formatter(oneline);
    accept(formatter);
    return formatter.toString();
  }

  private static Object deepCopy0(Object obj) {
    // recursively copy AST (according to obj's class)
    if (obj instanceof ASTNode) return ((ASTNode) obj).deepCopy();
    else if (obj instanceof Iterable)
      return ListSupport.map(
          (Iterable<?>) obj, (Function<? super Object, ?>) ASTNodeImpl::deepCopy0);
    else return obj;
  }
}