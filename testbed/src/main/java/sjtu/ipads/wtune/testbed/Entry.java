package sjtu.ipads.wtune.testbed;

import sjtu.ipads.wtune.testbed.runner.Runner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

public class Entry {
  public static final System.Logger LOG = System.getLogger("superopt");

  private static final String LOGGER_CONFIG =
      ".level = INFO\n"
          + "java.util.logging.ConsoleHandler.level = INFO\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  public static void main(String[] args) throws Exception {
    final String clsName = Entry.class.getPackageName() + "." + args[0];
    final Class<?> cls = Class.forName(clsName);

    if (!Runner.class.isAssignableFrom(cls)) {
      System.err.println("not a runner");
      return;
    }

    final Runner runner = (Runner) cls.getConstructor().newInstance();
    runner.prepare(args);
    runner.run();
    runner.stop();
  }
}