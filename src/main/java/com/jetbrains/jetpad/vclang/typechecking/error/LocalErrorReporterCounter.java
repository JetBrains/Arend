package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class LocalErrorReporterCounter implements LocalErrorReporter {
  private int myCount = 0;
  private final LocalErrorReporter myErrorReporter;

  public LocalErrorReporterCounter(LocalErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    myErrorReporter.report(error);
    myCount += 1;
  }

  @Override
  public void report(LocalTypeCheckingError localError) {
    myErrorReporter.report(localError);
    myCount += 1;
  }

  public int getErrorsNumber() {
    return myCount;
  }
}
