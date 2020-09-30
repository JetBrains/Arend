package org.arend.typechecking.computation;

public class UnstoppableCancellationIndicator implements CancellationIndicator {
  public static final CancellationIndicator INSTANCE = new UnstoppableCancellationIndicator();

  private UnstoppableCancellationIndicator() {}

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public void cancel() {

  }
}
