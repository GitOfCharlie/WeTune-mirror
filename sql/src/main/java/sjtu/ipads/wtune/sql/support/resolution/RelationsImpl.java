package sjtu.ipads.wtune.sql.support.resolution;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import static sjtu.ipads.wtune.sql.SqlSupport.simpleName;
import static sjtu.ipads.wtune.sql.ast.SqlKind.TableSource;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.tableSourceNameOf;

class RelationsImpl implements Relations {
  private final SqlContext ctx;
  private TIntObjectMap<RelationImpl> relations;

  RelationsImpl(SqlContext ctx) {
    this.ctx = ctx;
  }

  private TIntObjectMap<RelationImpl> relations() {
    if (relations == null) {
      relations = new TIntObjectHashMap<>();
      new ResolveRelation(ctx).resolve(this);
    }
    return relations;
  }

  Relation bindRelationRoot(SqlNode node) {
    assert Relation.isRelationRoot(node);

    final SqlNode parent = node.parent();
    final String qualification;
    if (TableSource.isInstance(node)) qualification = simpleName(tableSourceNameOf(node));
    else if (parent != null) qualification = simpleName(tableSourceNameOf(parent));
    else qualification = null;

    final RelationImpl relation = new RelationImpl(node, qualification);
    relations.put(node.nodeId(), relation);
    return relation;
  }

  @Override
  public RelationImpl enclosingRelationOf(SqlNode node) {
    return relations().get(ResolutionSupport.scopeRootOf(node));
  }

  @Override
  public void relocateNode(int oldId, int newId) {
    final RelationImpl relation = relations().remove(oldId);
    if (relation != null) relations().put(newId, relation);
  }

  @Override
  public void deleteNode(int nodeId) {
    relations().remove(nodeId);
  }
}
