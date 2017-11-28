package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

public class LocalErrorReporterCounter implements LocalErrorReporter {
  private int myCount = 0;
  private final Error.Level myLevel;
  private final LocalErrorReporter myErrorReporter;

  public LocalErrorReporterCounter(Error.Level level, LocalErrorReporter errorReporter) {
    myLevel = level;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    myErrorReporter.report(error);
    if (myLevel == null || error.getLevel().equals(myLevel)) {
      myCount++;
    }
  }

  @Override
  public void report(LocalError localError) {
    myErrorReporter.report(localError);
    if (myLevel == null || localError.getLevel().equals(myLevel)) {
      myCount++;
    }
  }

  public int getErrorsNumber() {
    return myCount;
  }
}
