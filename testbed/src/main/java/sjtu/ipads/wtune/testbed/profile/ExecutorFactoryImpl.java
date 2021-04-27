package sjtu.ipads.wtune.testbed.profile;

import static sjtu.ipads.wtune.testbed.util.DataSourceHelper.makeDataSource;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

class ExecutorFactoryImpl implements ExecutorFactory {
  private final Properties dbProperties;
  private DataSource dataSource;

  ExecutorFactoryImpl(Properties dbProperties) {
    this.dbProperties = dbProperties;
  }

  private DataSource dataSource() {
    if (dataSource == null) dataSource = makeDataSource(dbProperties);
    return dataSource;
  }

  @Override
  public Executor make(String sql) {
    try {
      return new ExecutorImpl(dataSource().getConnection(), sql);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    if (dataSource != null) {
      try {
        dataSource.unwrap(HikariDataSource.class).close();
      } catch (SQLException exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
