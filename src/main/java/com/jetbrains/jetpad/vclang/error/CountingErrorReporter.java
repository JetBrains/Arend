package com.jetbrains.jetpad.vclang.error;

public class CountingErrorReporter<T> implements ErrorReporter<T> {
  private int myCounter = 0;
  private final Error.Level myLevel;

  public CountingErrorReporter(Error.Level level) {
    myLevel = level;
  }

  public CountingErrorReporter() {
    myLevel = null;
  }

  public int getErrorsNumber() {
    return myCounter;
  }

  @Override
  public void report(GeneralError<T> error) {
    if (myLevel == null || myLevel == error.getLevel()) {
      ++myCounter;
    }
  }
}
