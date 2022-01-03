package sjtu.ipads.wtune.sql.relational.internal;

import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.relational.Attribute;
import sjtu.ipads.wtune.sql.relational.Relation;
import sjtu.ipads.wtune.sql.schema.Column;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sql.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sql.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sql.ast.NodeFields.*;
import static sjtu.ipads.wtune.sql.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sql.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sql.relational.Relation.RELATION;

public class DerivedAttribute extends BaseAttribute {
  // Subtleties:
  // 1. sometimes `reference` cannot be resolved beforehand,
  //    e.g. from x where (select x.a from y)
  //    such attribute is resolved lazily.
  // 2. sometimes `selectItem` is not available
  //    e.g. select * from t
  //    such attribute is resolved eagerly

  // invariant: reference == null => selectItem != null
  private final ASTNode selectItem;
  private Attribute reference;

  private DerivedAttribute(ASTNode selection, String name, Attribute ref) {
    super(selection.get(RELATION), name);
    this.reference = ref;
    this.selectItem = selection;
  }

  public static List<Attribute> projectionAttributesOf(ASTNode querySpec) {
    if (!QUERY_SPEC.isInstance(querySpec)) throw new IllegalArgumentException();

    final List<ASTNode> items = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final int estimatedSize =
        WILDCARD.isInstance(items.get(0).get(SELECT_ITEM_EXPR)) ? 8 : items.size();
    final List<Attribute> attributes = new ArrayList<>(estimatedSize);

    for (int i = 0; i < items.size(); i++) {
      final ASTNode item = items.get(i);
      if (WILDCARD.isInstance(item.get(SELECT_ITEM_EXPR))) expandWildcard(item, attributes);
      else attributes.add(new DerivedAttribute(item, selectItemName(item, i), null));
    }

    return attributes;
  }

  @Override
  public ASTNode selectItem() {
    return selectItem;
  }

  @Override
  public Column column(boolean recursive) {
    if (!recursive) return null;
    final Attribute ref = reference(true);
    return ref == null ? null : ref.column(true);
  }

  @Override
  public Attribute reference(boolean recursive) {
    final Attribute ref = reference0();
    if (!recursive || ref == null) return ref;
    return ref.reference(true);
  }

  private Attribute reference0() {
    if (reference != null) return reference;

    assert selectItem != null;
    final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
    return COLUMN_REF.isInstance(expr) ? (reference = Attribute.resolve(expr)) : null;
  }

  private static void expandWildcard(ASTNode item, List<Attribute> dest) {
    final Relation relation = item.get(RELATION);
    final ASTNode tableName = item.get(SELECT_ITEM_EXPR).get(WILDCARD_TABLE);
    final List<Relation> inputs =
        tableName == null
            ? relation.inputs()
            : singletonList(relation.input(tableName.get(TABLE_NAME_TABLE)));

    for (Relation input : inputs)
      for (Attribute ref : input.attributes())
        dest.add(new DerivedAttribute(item, ref.name(), ref));
  }

  private static String selectItemName(ASTNode selectItem, int index) {
    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    if (alias != null) return alias;

    final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
    if (COLUMN_REF.isInstance(expr)) return expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN);

    return "item" + index;
  }
}