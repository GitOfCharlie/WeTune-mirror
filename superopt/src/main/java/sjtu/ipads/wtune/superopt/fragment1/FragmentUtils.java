package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment1.pruning.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.superopt.fragment1.Op.mk;

public class FragmentUtils {
  private static final List<Op> DEFAULT_OP_SET =
      listMap(List.of(INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, IN_SUB_FILTER, UNION), Op::mk);
  private static final int DEFAULT_MAX_OPS = 4;
  private static final Set<Class<? extends Rule>> DEFAULT_PRUNING_RULES =
      Set.of(
          MalformedJoin.class,
          MalformedUnion.class,
          MalformedSubquery.class,
          NonLeftDeepJoin.class,
          AllJoin.class);

  static List<Fragment> enumerate() {
    final FragmentEnumerator enumerator = new FragmentEnumerator(DEFAULT_OP_SET, DEFAULT_MAX_OPS);
    enumerator.setPruningRules(DEFAULT_PRUNING_RULES);
    return enumerator.enumerate();
  }

  static int sizeOf(Op tree) {
    if (tree == null) return 0;

    int sub = 0;
    for (Op predecessor : tree.predecessors()) sub += sizeOf(predecessor);
    return sub + 1;
  }

  static int structuralHash(Op tree) {
    int h = tree.hashCode();
    for (Op operator : tree.predecessors()) {
      // Input is out of consideration.
      if (operator == null || operator instanceof Input) h = h * 31;
      else h = h * 31 + structuralHash(tree);
    }
    return h;
  }

  static boolean structuralEq(Op tree0, Op tree1) {
    if (tree0 == tree1) return true;
    if (tree0 == null || tree1 == null) return false;
    if (tree0.type() != tree1.type()) return false;

    final Op[] prevs0 = tree0.predecessors();
    final Op[] prevs1 = tree1.predecessors();
    for (int i = 0, bound = prevs0.length; i < bound; i++)
      if (!structuralEq(prevs0[i], prevs1[i])) return false;

    return true;
  }

  static int structuralCompare(Op tree0, Op tree1) {
    if (tree0 == tree1) return 0;

    final int sz0 = sizeOf(tree0), sz1 = sizeOf(tree1);
    if (sz0 < sz1) return -1;
    if (sz0 > sz1) return 1;

    final OperatorType type0 = tree0.type(), type1 = tree1.type();
    if (type0.numPredecessors() < type1.numPredecessors()) return -1;
    if (type0.numPredecessors() > type1.numPredecessors()) return 1;

    for (int i = 0, bound = type0.numPredecessors(); i < bound; ++i) {
      final int cmp = structuralCompare(tree0.predecessors()[i], tree1.predecessors()[i]);
      if (cmp != 0) return cmp;
    }

    return 0;
  }

  static StringBuilder structuralToString(Op tree, SymbolNaming naming, StringBuilder builder) {
    if (tree == null) return builder;

    builder.append(tree.type().text());

    if (naming != null)
      switch (tree.type()) {
        case INPUT:
          builder.append('<').append(naming.nameOf(((Input) tree).table())).append('>');
          break;
        case PROJ:
          builder.append('<').append(naming.nameOf(((Proj) tree).attrs())).append('>');
          break;
        case SIMPLE_FILTER:
          builder.append('<').append(naming.nameOf(((SimpleFilter) tree).predicate()));
          builder.append(' ').append(naming.nameOf(((SimpleFilter) tree).attrs())).append('>');
          break;
        case IN_SUB_FILTER:
          builder.append('<').append(naming.nameOf(((InSubFilter) tree).attrs())).append('>');
          break;
        case LEFT_JOIN:
        case INNER_JOIN:
          builder.append('<').append(naming.nameOf(((Join) tree).lhsAttrs()));
          builder.append(' ').append(naming.nameOf(((Join) tree).rhsAttrs())).append('>');
          break;
        default:
          throw new UnsupportedOperationException();
      }

    if (tree.type().numPredecessors() > 0) {
      builder.append('(');
      Commons.joining(
          ",", asList(tree.predecessors()), builder, (it, b) -> structuralToString(it, naming, b));
      builder.append(')');
    }

    return builder;
  }

  /** Fill holes with Input operator and call setFragment on each operator. */
  static Fragment setupFragment(Fragment fragment) {
    for (Hole<Op> hole : gatherHoles(fragment)) hole.fill(mk(OperatorType.INPUT));
    fragment.acceptVisitor(OpVisitor.traverse(it -> it.setFragment(fragment)));
    return fragment;
  }

  static List<Hole<Op>> gatherHoles(Fragment fragment) {
    if (fragment.root() == null) return singletonList(Hole.ofSetter(fragment::setRoot));

    final List<Hole<Op>> holes = new ArrayList<>();
    fragment.acceptVisitor(OpVisitor.traverse(x -> gatherHoles(x, holes)));

    return holes;
  }

  static void bindNames(Op op, String[] names, SymbolNaming naming) {
    switch (op.type()) {
      case INPUT:
        naming.setName(((Input) op).table(), names[1]);
        break;
      case INNER_JOIN:
      case LEFT_JOIN:
        naming.setName(((Join) op).lhsAttrs(), names[1]);
        naming.setName(((Join) op).rhsAttrs(), names[2]);
        break;
      case SIMPLE_FILTER:
        naming.setName(((SimpleFilter) op).predicate(), names[1]);
        naming.setName(((SimpleFilter) op).attrs(), names[2]);
        break;
      case IN_SUB_FILTER:
        naming.setName(((InSubFilter) op).attrs(), names[1]);
        break;
      case PROJ:
        naming.setName(((Proj) op).attrs(), names[1]);
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  static boolean replacePredecessor(Op op, Op target, Op rep) {
    final Op[] pres = op.predecessors();

    for (int i = 0; i < pres.length; i++)
      if (pres[i] == target) {
        op.setPredecessor(i, rep);
        return true;
      }

    return false;
  }

  private static void gatherHoles(Op op, List<Hole<Op>> buffer) {
    final Op[] prev = op.predecessors();

    for (int i = 0, bound = prev.length; i < bound; i++)
      if (prev[i] == null) {
        final int j = i;
        buffer.add(Hole.ofSetter(x -> op.setPredecessor(j, x)));
      }
  }
}
