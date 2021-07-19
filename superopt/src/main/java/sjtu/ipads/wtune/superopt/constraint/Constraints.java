package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import java.util.List;
import java.util.Set;

public interface Constraints extends List<Constraint> {
  Set<Symbol> eqClassOf(Symbol symbol);

  Symbol sourceOf(Symbol attrSym);

  boolean isSubOf(Symbol subAttrSym, Symbol superAttrSym);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraints mk(List<Constraint> constraints) {
    return ConstraintsImpl.mk(constraints);
  }
}
