package sjtu.ipads.wtune.common.utils;

public interface Lockable {
  void lock();

  void unlock();

  default LockGuard guard() {
    return new LockGuard(this);
  }

  static Lockable compose(Lockable... ls) {
    return new ComposedLockable(ls);
  }
}