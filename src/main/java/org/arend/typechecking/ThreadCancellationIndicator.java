package org.arend.typechecking;

public class ThreadCancellationIndicator implements CancellationIndicator {
  public static final CancellationIndicator INSTANCE = new ThreadCancellationIndicator();

  private ThreadCancellationIndicator() { }

  @Override
  public boolean isCanceled() {
    return Thread.interrupted();
  }
}
