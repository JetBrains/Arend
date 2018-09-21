package org.arend.typechecking.error;

import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.typechecking.error.local.LocalError;

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
    if (myLevel == null || error.level.equals(myLevel)) {
      myCount++;
    }
  }

  @Override
  public void report(LocalError localError) {
    myErrorReporter.report(localError);
    if (myLevel == null || localError.level.equals(myLevel)) {
      myCount++;
    }
  }

  public int getErrorsNumber() {
    return myCount;
  }
}
