package org.arend.error;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;

public class CountingErrorReporter implements ErrorReporter {
  private int myCounter = 0;
  private final GeneralError.Level myLevel;
  private final Class<? extends GeneralError> myError;
  private final ErrorReporter myErrorReporter;

  public CountingErrorReporter(Class<? extends GeneralError> error, ErrorReporter errorReporter) {
    myLevel = null;
    myError = error;
    myErrorReporter = errorReporter;
  }

  public CountingErrorReporter(GeneralError.Level level, ErrorReporter errorReporter) {
    myLevel = level;
    myError = null;
    myErrorReporter = errorReporter;
  }

  public CountingErrorReporter(ErrorReporter errorReporter) {
    myLevel = null;
    myError = null;
    myErrorReporter = errorReporter;
  }

  public int getErrorsNumber() {
    return myCounter;
  }

  public void resetErrorsNumber() {
    myCounter = 0;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public void report(GeneralError error) {
    if ((myLevel == null || myLevel == error.level) && (myError == null || myError.isInstance(error))) {
      ++myCounter;
    }
    myErrorReporter.report(error);
  }
}
