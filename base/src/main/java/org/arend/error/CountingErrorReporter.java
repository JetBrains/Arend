package org.arend.error;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;

public class CountingErrorReporter implements ErrorReporter {
  private int myCounter = 0;
  private final GeneralError.Level myLevel;
  private final ErrorReporter myErrorReporter;

  public CountingErrorReporter(GeneralError.Level level, ErrorReporter errorReporter) {
    myLevel = level;
    myErrorReporter = errorReporter;
  }

  public CountingErrorReporter(ErrorReporter errorReporter) {
    myLevel = null;
    myErrorReporter = errorReporter;
  }

  public int getErrorsNumber() {
    return myCounter;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public void report(GeneralError error) {
    if (myLevel == null || myLevel == error.level) {
      ++myCounter;
    }
    myErrorReporter.report(error);
  }
}
