package com.jetbrains.jetpad.vclang.typechecking.error.reporter;

import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class CountingErrorReporter implements ErrorReporter {
  private int myCounter = 0;

  public int getErrorsNumber() {
    return myCounter;
  }

  @Override
  public void report(GeneralError error) {
    ++myCounter;
  }
}
