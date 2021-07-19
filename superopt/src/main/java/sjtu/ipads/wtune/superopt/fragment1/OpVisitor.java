package sjtu.ipads.wtune.superopt.fragment1;

import java.util.function.Consumer;

public interface OpVisitor {
  static OpVisitor traverse(Consumer<Op> consumer) {
    return new OpVisitor() {
      @Override
      public boolean enter(Op op) {
        consumer.accept(op);
        return true;
      }
    };
  }

  default void enterEmpty(Op parent, int idx) {}

  default boolean enter(Op op) {
    return true;
  }

  default void leave(Op op) {}

  default boolean enterAgg(Agg op) {
    return true;
  }

  default void leaveAgg(Agg op) {}

  default boolean enterInnerJoin(InnerJoin op) {
    return true;
  }

  default void leaveInnerJoin(InnerJoin op) {}

  default boolean enterLeftJoin(LeftJoin op) {
    return true;
  }

  default void leaveLeftJoin(LeftJoin op) {}

  default boolean enterLimit(Limit op) {
    return true;
  }

  default void leaveLimit(Limit op) {}

  default boolean enterPlainFilter(SimpleFilter op) {
    return true;
  }

  default void leaveSimpleFilter(SimpleFilter op) {}

  default boolean enterProj(Proj op) {
    return true;
  }

  default void leaveProj(Proj op) {}

  default boolean enterSubqueryFilter(InSubFilter op) {
    return true;
  }

  default void leaveInSubFilter(InSubFilter op) {}

  default boolean enterUnion(Union op) {
    return true;
  }

  default void leaveUnion(Union op) {}

  default boolean enterSort(Sort op) {
    return true;
  }

  default void leaveSort(Sort op) {}

  default boolean enterInput(Input input) {
    return true;
  }

  default void leaveInput(Input input) {}
}
