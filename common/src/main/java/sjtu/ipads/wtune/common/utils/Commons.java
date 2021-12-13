package sjtu.ipads.wtune.common.utils;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public interface Commons {
  /** if str[0] == '"' and str[-1] == '"', return str[1:-2] */
  static String unquoted(String str) {
    return unquoted(str, '"');
  }

  /** if str[0] == quota and str[-1] == quota, return str[1:-2] */
  static String unquoted(String str, char quota) {
    if (str == null) return null;
    final int length = str.length();
    if (length <= 1) return str;

    final char c0 = str.charAt(0);
    final char ce = str.charAt(length - 1);
    return quota == c0 && quota == ce ? str.substring(1, length - 1) : str;
  }

  /** Helper to make compiler happy. */
  static <T> T assertFalse() {
    assert false;
    return null;
  }

  static StringBuilder trimTrailing(StringBuilder sb, int i) {
    return sb.delete(sb.length() - i, sb.length());
  }

  static String surround(String str, char quota) {
    return quota + str + quota;
  }

  @Contract("!null, _ -> param1; null, !null -> param2; null, null -> null")
  static <T> T coalesce(T val0, T val1) {
    return val0 != null ? val0 : val1;
  }

  @SafeVarargs
  static <T> T coalesce(T... vals) {
    for (T val : vals) if (val != null) return val;
    return null;
  }

  static <T> T coalesce(T val, Supplier<T> other) {
    return val == null ? other.get() : val;
  }

  static <T> T[] sorted(T[] arr, Comparator<? super T> comparator) {
    Arrays.sort(arr, comparator);
    return arr;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[] arr, T... vs) {
    final T[] ret =
        (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length + vs.length);
    System.arraycopy(arr, 0, ret, 0, arr.length);
    System.arraycopy(vs, 0, ret, arr.length, vs.length);
    return ret;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[] arr1, T[] arr2, T... vs) {
    final T[] ret =
        (T[])
            Array.newInstance(
                arr1.getClass().getComponentType(), arr1.length + arr2.length + vs.length);
    System.arraycopy(arr1, 0, ret, 0, arr1.length);
    System.arraycopy(arr2, 0, ret, arr1.length, arr2.length);
    System.arraycopy(vs, 0, ret, arr1.length + arr2.length, vs.length);
    return ret;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[]... arrs) {
    final int length = stream(arrs).mapToInt(it -> it.length).sum();
    final T[] ret =
        (T[]) Array.newInstance(arrs.getClass().getComponentType().getComponentType(), length);

    int base = 0;
    for (final T[] arr : arrs) {
      System.arraycopy(arr, 0, ret, base, arr.length);
      base += arr.length;
    }
    return ret;
  }

  static <T> T head(List<T> xs) {
    if (xs.isEmpty()) return null;
    else return xs.get(0);
  }

  static <T> T tail(List<T> xs) {
    if (xs.isEmpty()) return null;
    else return xs.get(xs.size() - 1);
  }

  static <T> T elemAt(List<T> xs, int idx) {
    if (idx >= xs.size() || idx <= -xs.size() - 1) return null;
    if (idx >= 0) return xs.get(idx);
    else return xs.get(xs.size() + idx);
  }

  static <T> T safeGet(List<T> xs, int index) {
    if (index < 0 || index >= xs.size()) return null;
    else return xs.get(index);
  }

  static <T> void push(List<T> xs, T x) {
    xs.add(x);
  }

  static boolean isEmpty(Collection<?> xs) {
    return xs == null || xs.isEmpty();
  }

  static <T> List<T> listSort(List<T> arr, Comparator<? super T> comparator) {
    arr.sort(comparator);
    return arr;
  }

  static <T> List<T> listSwap(List<T> list, int idx0, int idx1) {
    final T e0 = list.get(idx0);
    list.set(idx0, list.get(idx1));
    list.set(idx1, e0);
    return list;
  }

  static <T> List<T> removeIf(List<T> xs, Predicate<T> pred) {
    final List<T> ret = new ArrayList<>();
    // so dirty, but efficient
    xs.removeIf(it -> pred.test(it) && ret.add(it));
    return ret;
  }

  static int countOccurrences(String str, String target) {
    int index = -1, occurrences = 0;
    while ((index = str.indexOf(target, index + 1)) != -1) {
      ++occurrences;
    }
    return occurrences;
  }

  static int compareStringLengthFirst(String x, String y) {
    if (x.length() < y.length()) return -1;
    if (x.length() > y.length()) return 1;
    return x.compareTo(y);
  }

  static int max(int[] arr) {
    int max = Integer.MIN_VALUE;
    for (int i : arr) if (i > max) max = i;
    return max;
  }

  static int sum(int[] arr, int start, int end) {
    int sum = 0;
    for (int i = start; i < end; ++i) sum += arr[i];
    return sum;
  }

  static Iterable<int[]> permutation(int n, int k) {
    return PermutationIter.permute(n, k);
  }

  static <T> T echo(T t) {
    System.out.println(t);
    return t;
  }

  static <T> Set<T> newIdentitySet() {
    return Collections.newSetFromMap(new IdentityHashMap<>());
  }

  static <T> Set<T> newIdentitySet(int expectedSize) {
    return Collections.newSetFromMap(new IdentityHashMap<>(expectedSize));
  }

  static <T> Set<T> newIdentitySet(Collection<T> xs) {
    final Set<T> set = Collections.newSetFromMap(new IdentityHashMap<>(xs.size()));
    set.addAll(xs);
    return set;
  }

  static String joining(String sep, Iterable<?> objs) {
    return joining(sep, objs, new StringBuilder()).toString();
  }

  static <T> String joining(String sep, Iterable<T> objs, Function<T, String> func) {
    return joining(sep, objs, new StringBuilder(), func).toString();
  }

  static String joining(
      String headOrPrefix, String sep, String tailOrSuffix, boolean asFixture, Iterable<?> objs) {
    return joining(headOrPrefix, sep, tailOrSuffix, asFixture, objs, new StringBuilder())
        .toString();
  }

  static <T> String joining(
      String headOrPrefix,
      String sep,
      String tailOrSuffix,
      boolean asFixture,
      Iterable<T> objs,
      Function<T, String> func) {
    return joining(headOrPrefix, sep, tailOrSuffix, asFixture, objs, new StringBuilder(), func)
        .toString();
  }

  static String joining(
      String head, String prefix, String sep, String suffix, String tail, Iterable<?> objs) {
    return joining(head, prefix, sep, suffix, tail, objs, new StringBuilder()).toString();
  }

  static <T> String joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<T> objs,
      Function<T, String> func) {
    return joining(head, prefix, sep, suffix, tail, objs, new StringBuilder(), func).toString();
  }

  static StringBuilder joining(String sep, Iterable<?> objs, StringBuilder dest) {
    return joining("", "", sep, "", "", objs, dest);
  }

  static <T> StringBuilder joining(
      String sep, Iterable<T> objs, StringBuilder dest, Function<T, String> func) {
    return joining("", "", sep, "", "", objs, dest, func);
  }

  static <T> StringBuilder joining(
      String sep, Iterable<T> objs, StringBuilder dest, BiConsumer<T, StringBuilder> func) {
    return joining("", "", sep, "", "", objs, dest, func);
  }

  static StringBuilder joining(
      String headOrPrefix,
      String sep,
      String tailOrSuffix,
      boolean asFixture,
      Iterable<?> objs,
      StringBuilder dest) {
    return joining(headOrPrefix, sep, tailOrSuffix, asFixture, objs, dest, Objects::toString);
  }

  static <T> StringBuilder joining(
      String headOrPrefix,
      String sep,
      String tailOrSuffix,
      boolean asFixture,
      Iterable<T> objs,
      StringBuilder dest,
      Function<T, String> func) {
    if (asFixture) return joining("", headOrPrefix, sep, tailOrSuffix, "", objs, dest, func);
    else return joining(headOrPrefix, "", sep, "", tailOrSuffix, objs, dest, func);
  }

  static StringBuilder joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<?> objs,
      StringBuilder dest) {
    return joining(head, prefix, sep, suffix, tail, objs, dest, it -> Objects.toString(it));
  }

  static <T> StringBuilder joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<T> objs,
      StringBuilder dest,
      Function<T, String> func) {
    final StringBuilder builder = dest != null ? dest : new StringBuilder();
    builder.append(head);
    boolean isFirst = true;
    for (T obj : objs) {
      if (!isFirst) builder.append(sep);
      isFirst = false;
      builder.append(prefix);
      builder.append(func.apply(obj));
      builder.append(suffix);
    }
    builder.append(tail);
    return builder;
  }

  static <T> StringBuilder joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<T> objs,
      StringBuilder dest,
      BiConsumer<T, StringBuilder> func) {
    final StringBuilder builder = dest != null ? dest : new StringBuilder();
    builder.append(head);
    boolean isFirst = true;
    for (T obj : objs) {
      if (!isFirst) builder.append(sep);
      isFirst = false;
      builder.append(prefix);
      func.accept(obj, builder);
      builder.append(suffix);
    }
    builder.append(tail);
    return builder;
  }

  static TIntList newIntList(int expectedSize) {
    return new TIntArrayList(expectedSize);
  }
}
