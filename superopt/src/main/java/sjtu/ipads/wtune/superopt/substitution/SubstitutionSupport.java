package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.Complexity;
import sjtu.ipads.wtune.superopt.fragment1.FragmentSupport;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.Symbols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.none;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentSupport.translateAsPlan;

public class SubstitutionSupport {
  public static Substitution flip(Substitution sub) {
    return Substitution.mk(sub._1(), sub._0(), sub.constraints());
  }

  public static boolean isEligible(Substitution sub) {
    final Symbols symbols = sub._1().symbols();
    final Constraints constraints = sub.constraints();

    for (Symbol.Kind kind : Symbol.Kind.values())
      for (Symbol symbol : symbols.symbolsOf(kind))
        if (none(constraints.eqClassOf(symbol), it -> it.ctx() != symbol.ctx())) {
          return false;
        }

    final Complexity complexity0 = FragmentSupport.calcComplexity(sub._0());
    final Complexity complexity1 = FragmentSupport.calcComplexity(sub._1());

    return complexity0.compareTo(complexity1, true) >= 0;
  }

  public static SubstitutionBank loadBank(Path path) throws IOException {
    return SubstitutionBank.parse(Files.readAllLines(path));
  }

  public static SubstitutionBank minimize(SubstitutionBank bank) {
    final DuplicationChecker checker = new DuplicationChecker(bank);
    final Set<Substitution> duplicated = new HashSet<>(bank.size() >> 2);

    for (Substitution substitution : bank)
      if (checker.isDuplicated(substitution)) {
        duplicated.add(substitution);
      }

    bank.removeAll(duplicated);
    bank.removeIf(MeaninglessChecker::isMeaningless);
    return bank;
  }

  public static void printReadable(Substitution substitution) {
    System.out.println(substitution);
    final Pair<PlanNode, PlanNode> pair = translateAsPlan(substitution, false);
    System.out.println(" q0: " + translateAsAst(disambiguate(pair.getLeft())));
    System.out.println(" q1: " + translateAsAst(disambiguate(pair.getRight())));
  }
}
