package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;

class ConstraintImpl implements Constraint {
  private final Kind kind;
  private final Symbol[] symbols;

  ConstraintImpl(Kind kind, Symbol[] symbols) {
    this.kind = kind;
    this.symbols = symbols;
  }

  static Constraint parse(String str, SymbolNaming naming) {
    final String[] fields = str.split("[(),\\[\\] ]+");
    final Kind kind = Kind.valueOf(fields[0].replace("Pick", "Attrs") /* backward compatible */);

    if (fields.length != kind.numSyms() + 1)
      throw new IllegalArgumentException("invalid serialized constraint: " + str);

    final Symbol[] symbols =
        arrayMap(asList(fields).subList(1, fields.length), naming::symbolOf, Symbol.class);

    return new ConstraintImpl(kind, symbols);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public Symbol[] symbols() {
    return symbols;
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    builder.append(kind.name()).append('(');
    Commons.joining(",", asList(symbols), builder, naming::nameOf);
    builder.append(')');
    return builder;
  }
}
