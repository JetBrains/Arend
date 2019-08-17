package org.arend.typechecking.error;

import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;

public class ErrorReporterCounter implements ErrorReporter {
  private int myCount = 0;
  private final GeneralError.Level myLevel;
  private final ErrorReporter myErrorReporter;

  public ErrorReporterCounter(GeneralError.Level level, ErrorReporter errorReporter) {
    myLevel = level;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    myErrorReporter.report(error);
    if (myLevel == null || error.level.equals(myLevel)) {
      myCount++;
    }
  }

  public int getErrorsNumber() {
    return myCount;
  }
}
