package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class LocalErrorReporterCounter<T> implements LocalErrorReporter<T> {
  private int myCount = 0;
  private final LocalErrorReporter<T> myErrorReporter;

  public LocalErrorReporterCounter(LocalErrorReporter<T> errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError<T> error) {
    myErrorReporter.report(error);
    myCount += 1;
  }

  @Override
  public void report(LocalTypeCheckingError<T> localError) {
    myErrorReporter.report(localError);
    myCount += 1;
  }

  public int getErrorsNumber() {
    return myCount;
  }
}
