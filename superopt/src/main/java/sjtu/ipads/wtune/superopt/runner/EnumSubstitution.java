package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.FragmentSupport;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.stream.IntStream.range;
import static sjtu.ipads.wtune.prover.ProverSupport.mkLogicCtx;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;

public class EnumSubstitution implements Runner {
  private PrintWriter out;
  private PrintWriter err;
  private PrintWriter fail;

  private boolean echo, runFailure, parallel;
  private int iBegin, jBegin;
  private int numSegments, segMask;
  private List<Fragment> fragments;
  private long timeout;

  private String failFile;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    echo = args.getOptional("echo", boolean.class, false);
    runFailure = args.getOptional("runFailure", boolean.class, false);
    parallel = args.getOptional("parallel", boolean.class, true);
    timeout = args.getOptional("timeout", long.class, 30000L);
    numSegments = args.getPositional(0, int.class);
    segMask = args.getPositional(1, int.class);

    final String outFile = args.getOptional("out", String.class, "wtune_data/enum.out");
    final String errFile = args.getOptional("err", String.class, "wtune_data/enum.err");
    final String failFile = args.getOptional("failure", String.class, "wtune_data/enum.fail");

    out = new PrintWriter(Files.newOutputStream(Path.of(outFile)));
    err = new PrintWriter(Files.newOutputStream(Path.of(errFile)));
    fail = new PrintWriter(Files.newOutputStream(Path.of(failFile)));

    final String from = args.getOptional("from", String.class, "0,0");
    final String[] split = from.split(",");
    iBegin = parseInt(split[0]);
    jBegin = parseInt(split[1]);

    this.failFile = failFile;
  }

  @Override
  public void run(String[] args) throws Exception {
    if (runFailure) fromFailures();
    else fromEnumeration();
  }

  private void fromEnumeration() {
    fragments = FragmentSupport.enumFragments();

    final int total = fragments.size();
    for (int x = 0; x < total; ++x) {
      final int i = x;
      final IntStream stream = !parallel ? range(x + 1, total) : range(x + 1, total).parallel();

      stream.forEach(j -> fromFragments(i, j));
    }
  }

  private void fromFailures() {
    Stream<String> stream;
    try {
      stream = Files.lines(Path.of(failFile));
      if (parallel) stream = stream.parallel();

      stream.forEach(this::fromString);

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void fromString(String fragmentPair) {
    final String[] split = fragmentPair.split("\\|");
    final Fragment f0 = Fragment.parse(split[0], null);
    final Fragment f1 = Fragment.parse(split[1], null);
    enumerate(f0, f1);
  }

  private void fromFragments(int i, int j) {
    assert i < j;
    if (i < iBegin || j < jBegin) return;

    final int ordinal = (((fragments.size() << 1) - i - 1) * i) >> 1 + j - i - 1;
    if ((ordinal % numSegments) != segMask) return;
    final Fragment f0 = fragments.get(i);
    final Fragment f1 = fragments.get(j);
    if (echo) {
      System.out.printf("%d,%d\n", i, j);
      System.out.println(f0);
      System.out.println(f1);
    }
    enumerate(f0, f1);
  }

  private void enumerate(Fragment f0, Fragment f1) {
    try {
      final List<Substitution> substitutions = enumConstraints(f0, f1, mkLogicCtx(), timeout);
      for (Substitution substitution : substitutions) out.println(substitution);
      out.flush();

      if (echo) substitutions.forEach(System.out::println);

    } catch (Throwable ex) {
      err.print(f0);
      err.print(f1);
      ex.printStackTrace(err);
      err.print("====");
      err.flush();

      if (echo) {
        System.err.println(f0);
        System.err.println(f1);
        ex.printStackTrace();
      }

      fail.print(f0);
      fail.print("|");
      fail.print(f1);
      fail.flush();
    }
  }
}