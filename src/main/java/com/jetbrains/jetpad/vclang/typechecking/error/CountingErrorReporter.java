package com.jetbrains.jetpad.vclang.typechecking.error;

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
