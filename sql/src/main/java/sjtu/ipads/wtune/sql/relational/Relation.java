package sjtu.ipads.wtune.sql.relational;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.FieldDomain;
import sjtu.ipads.wtune.sql.relational.internal.RelationField;
import sjtu.ipads.wtune.sql.schema.Table;

import java.util.List;

import static sjtu.ipads.wtune.sql.ast.ASTVistor.topDownVisit;
import static sjtu.ipads.wtune.sql.ast.NodeFields.SET_OP_LEFT;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.tableNameOf;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sql.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sql.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sql.relational.internal.RelationImpl.rootedBy;
import static sjtu.ipads.wtune.sql.util.ASTHelper.simpleName;

public interface Relation {
  FieldKey<Relation> RELATION = RelationField.INSTANCE;

  FieldDomain[] RELATION_BOUNDARY = {QUERY, SIMPLE_SOURCE, DERIVED_SOURCE};

  ASTNode node(); // invariant: isRelationBoundary(node())

  String alias();

  List<Relation> inputs();

  List<Attribute> attributes();

  boolean isOutdated();

  default Table table() {
    return isTable() ? node().context().schema().table(tableNameOf(node())) : null;
  }

  default boolean isInput() {
    return TABLE_SOURCE.isInstance(node())
        || (SET_OP.isInstance(node().parent()) && node().parent().get(SET_OP_LEFT) == node());
  }

  default boolean isTable() {
    return SIMPLE_SOURCE.isInstance(node());
  }

  default Relation parent() {
    final ASTNode parentNode = node().parent();
    return parentNode == null ? null : parentNode.get(RELATION);
  }

  default Attribute attribute(String name) {
    name = simpleName(name);
    for (Attribute attribute : attributes()) if (name.equals(attribute.name())) return attribute;
    return null;
  }

  default Relation input(String name) {
    name = simpleName(name);
    for (Relation input : inputs()) if (name.equals(input.alias())) return input;
    final Relation foreign = foreignInput();
    return foreign != null ? foreign.input(name) : null;
  }

  default Relation foreignInput() {
    return DERIVED_SOURCE.isInstance(node()) || SIMPLE_SOURCE.isInstance(node()) ? null : parent();
  }

  default boolean isForeignAttribute(Attribute attr) {
    final Relation foreign = foreignInput();
    return foreign != null && foreign.inputs().contains(attr.owner());
  }

  static boolean isRelationBoundary(ASTNode node) {
    for (FieldDomain fieldDomain : RELATION_BOUNDARY) if (fieldDomain.isInstance(node)) return true;
    return false;
  }

  /**
   * Resolve relation for a node.
   *
   * <ol>
   *   Precondition:
   *   <li>node must be relation boundary.
   *   <li>the relation of node's parent must be resolved.
   * </ol>
   *
   * Advice: If you are not sure whether to call this method, then don't. Instead, attach the node
   * to AST properly, and call node.relation().
   */
  static Relation resolve(ASTNode node) {
    if (!isRelationBoundary(node))
      throw new IllegalArgumentException("cannot resolve relation for " + node.nodeType());
    // This check cannot be actually performed here, because parent().relation() triggers
    // cascading resolution, which is unintended:
    // ensure (node.parent() == null || node.parent().relation() != null)

    node.accept(topDownVisit(it -> it.set(RELATION, rootedBy(it)), RELATION_BOUNDARY));
    return node.get(RELATION);
  }
}