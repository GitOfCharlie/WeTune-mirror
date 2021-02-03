package sjtu.ipads.wtune.stmt.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
  public static final String CSV_SEP = ";";
  private static Path dataDir;

  static {
    setDataDir(System.getProperty("wtune.dataDir", System.getProperty("user.dir") + "/wtune_data"));
  }

  public static void setDataDir(String path) {
    dataDir = Paths.get(path);
  }

  public static Path dataDir() {
    return dataDir;
  }

  public static Path testScriptDir() {
    return dataDir().resolve("tests");
  }

  public static Path dbPath() {
    return dataDir().resolve("wtune.db");
  }

  public static String readFile(String first, String... remaining) {
    final Path filePath = dataDir().resolve(Paths.get(first, remaining));
    return safeIO(() -> Files.readString(filePath));
  }

  public static Iterable<String> readLines(String first, String... remaining) {
    final Path filePath = dataDir().resolve(Paths.get(first, remaining));
    return safeIO(() -> Files.readAllLines(filePath));
  }

  private static <T> T safeIO(IO<T> io) {
    try {
      return io.get();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @FunctionalInterface
  private interface IO<T> {
    T get() throws IOException;
  }
}
