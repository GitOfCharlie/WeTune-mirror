package sjtu.ipads.wtune.stmt.rawlog.internal;

import sjtu.ipads.wtune.stmt.rawlog.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class LogReaderImpl implements LogReader {
  private static final Comparator<StackTrace> IDX_CMP = Comparator.comparingInt(StackTrace::id);
  private final Supplier<StmtLogParser> sqlLogParserFactory;
  private final Supplier<TraceLogParser> traceLogParserFactory;

  private LogReaderImpl(
      Supplier<StmtLogParser> sqlLogParserFactory, Supplier<TraceLogParser> traceLogParserFactory) {
    this.sqlLogParserFactory = sqlLogParserFactory;
    this.traceLogParserFactory = traceLogParserFactory;
  }

  public static LogReader build() {
    return new LogReaderImpl(StmtLogParser::forTaggedFormat, TraceLogParser::forTaggedFormat);
  }

  public static LogReader build(
      Supplier<StmtLogParser> sqlLogParserFactory, Supplier<TraceLogParser> traceLogParserFactory) {
    return new LogReaderImpl(sqlLogParserFactory, traceLogParserFactory);
  }

  @Override
  public List<RawStmt> readFrom(Iterable<String> sqlLog, Iterable<String> traceLog) {
    final StmtLogParser stmtLogParser = sqlLogParserFactory.get();

    sqlLog.forEach(stmtLogParser::accept);
    final List<RawStmt> rawStmts = stmtLogParser.stmts();

    if (traceLog == null) return rawStmts;

    final TraceLogParser traceLogParser = traceLogParserFactory.get();
    traceLog.forEach(traceLogParser::accept);
    final List<StackTrace> stackTraces = traceLogParser.stackTraces();
    stackTraces.sort(IDX_CMP);

    for (RawStmt rawStmt : rawStmts) {
      final int idx = Collections.binarySearch(stackTraces, new StackTrace(rawStmt.id()), IDX_CMP);
      if (idx >= 0) rawStmt.setStackTrace(stackTraces.get(idx));
    }

    return rawStmts;
  }
}