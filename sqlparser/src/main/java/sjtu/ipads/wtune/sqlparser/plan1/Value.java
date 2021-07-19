package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.schema.Column;

public interface Value {
  String qualification();

  String name();

  // the following three are exclusive:
  // if column() != null, then expr() == null && wildcardQualification() == null
  // and so on.
  // All three are null is permitted, which indicates the value is an unqualified wildcard,
  // i.e., a plain "*".
  Column column();

  Expr expr();

  String wildcardQualification();

  void setQualification(String qualification);

  default Ref selfish() {
    return new RefImpl(qualification(), name());
  }
}
