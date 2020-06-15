package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

abstract class MySQLBaseParser extends Parser implements MySQLRecognizerCommon {
  private long serverVersion = 0;
  private int sqlMode = NoMode;

  public MySQLBaseParser(TokenStream input) {
    super(input);
  }

  public void setServerVersion(long serverVersion) {
    this.serverVersion = serverVersion;
  }

  public void setSqlMode(int sqlMode) {
    this.sqlMode = sqlMode;
  }

  @Override
  public long serverVersion() {
    return serverVersion;
  }

  @Override
  public int sqlMode() {
    return sqlMode;
  }
}