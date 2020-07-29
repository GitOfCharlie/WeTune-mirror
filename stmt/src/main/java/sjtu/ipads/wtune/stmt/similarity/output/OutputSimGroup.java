package sjtu.ipads.wtune.stmt.similarity.output;

import sjtu.ipads.wtune.stmt.dao.StructGroupDao;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

public class OutputSimGroup extends SimGroup {
  public OutputSimGroup() {}

  public OutputSimGroup(Set<Statement> stmts) {
    super(stmts);
  }

  public void save() {
    StructGroupDao.instance().save(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends SimGroup.Builder {
    private final Map<OutputSimKey, Set<Statement>> groups = new HashMap<>(1024);

    private Builder() {}

    @Override
    public void add(Statement stmt) {
      for (OutputSimKey key : OutputFingerprint.extractKey(stmt))
        groups.computeIfAbsent(key, ignored -> new HashSet<>()).add(stmt);
    }

    @Override
    public List<SimGroup> build() {
      final List<SimGroup> groups =
          this.groups.values().stream()
              .filter(it -> it.size() > 1)
              .distinct()
              .map(OutputSimGroup::new)
              .collect(Collectors.toList());
      for (int i = 0; i < groups.size(); i++) groups.get(i).setGroupId(i);
      return groups;
    }
  }
}
