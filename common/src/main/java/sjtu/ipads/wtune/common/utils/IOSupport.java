package sjtu.ipads.wtune.common.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.*;

public interface IOSupport {
  static void appendTo(Path path, Consumer<PrintWriter> writer) {
    try (final var out = new PrintWriter(Files.newOutputStream(path, APPEND, WRITE, CREATE))) {
      writer.accept(out);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  static void printWithLock(Path path, Consumer<PrintWriter> writer) {
    try (final var os = new FileOutputStream(path.toFile(), true);
        final var out = new PrintWriter(os);
        final var lock = os.getChannel().lock(); ) {
      writer.accept(out);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }
}
