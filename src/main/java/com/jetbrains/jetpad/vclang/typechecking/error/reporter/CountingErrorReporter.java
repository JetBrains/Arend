package com.jetbrains.jetpad.vclang.typechecking.error.reporter;

import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class CountingErrorReporter implements ErrorReporter {
  private int myCounter = 0;
  private final GeneralError.Level myLevel;

  public CountingErrorReporter(GeneralError.Level level) {
    myLevel = level;
  }

  public CountingErrorReporter() {
    myLevel = null;
  }

  public int getErrorsNumber() {
    return myCounter;
  }

  @Override
  public void report(GeneralError error) {
    if (myLevel == null || myLevel == error.getLevel()) {
      ++myCounter;
    }
  }
}
