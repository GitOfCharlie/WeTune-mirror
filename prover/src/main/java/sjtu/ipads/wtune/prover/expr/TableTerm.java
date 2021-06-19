package sjtu.ipads.wtune.prover.expr;

public interface TableTerm extends UTerm {
  Name name();

  Tuple tuple();

  @Override
  default Kind kind() {
    return Kind.TABLE;
  }
}