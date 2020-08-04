package org.arend.typechecking.computation;

public class ThreadCancellationIndicator implements CancellationIndicator {
  public static final CancellationIndicator INSTANCE = new ThreadCancellationIndicator();

  private ThreadCancellationIndicator() { }

  @Override
  public boolean isCanceled() {
    return Thread.currentThread().isInterrupted();
  }

  @Override
  public void cancel() {
    Thread.currentThread().interrupt();
  }
}
