package org.arend.typechecking.computation;

import org.arend.util.ComputationInterruptedException;

import java.util.function.Supplier;

public class ComputationRunner<T> {
  private static CancellationIndicator CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;

  public static void checkCanceled() throws ComputationInterruptedException {
    CANCELLATION_INDICATOR.checkCanceled();
  }

  public static CancellationIndicator getCancellationIndicator() {
    return CANCELLATION_INDICATOR;
  }

  public static void resetCancellationIndicator() {
    CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;
  }

  protected T computationInterrupted() {
    return null;
  }

  public T run(CancellationIndicator cancellationIndicator, Supplier<T> runnable) {
    synchronized (ComputationRunner.class) {
      if (cancellationIndicator != null) {
        CANCELLATION_INDICATOR = cancellationIndicator;
      }

      try {
        return runnable.get();
      } catch (ComputationInterruptedException ignored) {
        return computationInterrupted();
      } finally {
        if (cancellationIndicator != null) {
          CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;
        }
      }
    }
  }
}
