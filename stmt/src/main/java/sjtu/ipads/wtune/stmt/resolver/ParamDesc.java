package sjtu.ipads.wtune.stmt.resolver;

import java.util.Deque;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface ParamDesc {
  int index();

  void setIndex(int i);

  ASTNode node();

  boolean isCheckNull();

  boolean isElement();

  Deque<ParamModifier> modifiers();
}
