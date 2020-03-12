package org.arend.error;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;

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
    if (myLevel == null || myLevel == error.level) {
      ++myCounter;
    }
  }
}
