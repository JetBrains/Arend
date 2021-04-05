package org.arend.typechecking.computation;

import org.arend.util.ComputationInterruptedException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ComputationRunner<T> {
  private static CancellationIndicator CANCELLATION_INDICATOR = UnstoppableCancellationIndicator.INSTANCE;
  private static final Lock lock = new ReentrantLock();

  public static void checkCanceled() throws ComputationInterruptedException {
    CANCELLATION_INDICATOR.checkCanceled();
  }

  public static CancellationIndicator getCancellationIndicator() {
    return CANCELLATION_INDICATOR;
  }

  public static void resetCancellationIndicator() {
    CANCELLATION_INDICATOR = UnstoppableCancellationIndicator.INSTANCE;
  }

  public static boolean isCancellationIndicatorSet() {
    return CANCELLATION_INDICATOR != UnstoppableCancellationIndicator.INSTANCE;
  }

  protected T computationInterrupted() {
    return null;
  }

  public static void lock(CancellationIndicator cancellationIndicator) {
    lock.lock();
    if (cancellationIndicator != null) {
      CANCELLATION_INDICATOR = cancellationIndicator;
    }
  }

  public static void unlock() {
    CANCELLATION_INDICATOR = UnstoppableCancellationIndicator.INSTANCE;
    lock.unlock();
  }

  public T run(CancellationIndicator cancellationIndicator, Supplier<T> runnable) {
    lock(cancellationIndicator);
    try {
      return runnable.get();
    } catch (ComputationInterruptedException ignored) {
      return computationInterrupted();
    } finally {
      unlock();
    }
  }
}
