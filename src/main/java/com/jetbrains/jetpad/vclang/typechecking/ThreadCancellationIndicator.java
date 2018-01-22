package com.jetbrains.jetpad.vclang.typechecking;

public class ThreadCancellationIndicator implements CancellationIndicator {
  public static final CancellationIndicator INSTANCE = new ThreadCancellationIndicator();

  private ThreadCancellationIndicator() { }

  @Override
  public boolean isCanceled() {
    return Thread.interrupted();
  }
}
