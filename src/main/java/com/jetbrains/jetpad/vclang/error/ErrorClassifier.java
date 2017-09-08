package com.jetbrains.jetpad.vclang.error;

public abstract class ErrorClassifier implements ErrorReporter {
  private final ErrorReporter myErrorReporter;

  public ErrorClassifier(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    switch (error.level) {
      case ERROR:
        reportedError(error);
        break;
      case GOAL:
        reportedGoal(error);
        break;
      case WARNING:
        reportedWarning(error);
        break;
      case INFO:
        reportedInfo(error);
        break;
    }
    myErrorReporter.report(error);
  }

  protected void reportedError(GeneralError error) {}
  protected void reportedGoal(GeneralError error) {}
  protected void reportedWarning(GeneralError error) {}
  protected void reportedInfo(GeneralError error) {}
}
