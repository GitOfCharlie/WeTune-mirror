package sjtu.ipads.wtune.common.utils;

public interface NaturalCongruence<T> extends Congruence<T, T> {
  static <T> NaturalCongruence<T> mk() {
    return new BaseNaturalCongruence<>();
  }
}
