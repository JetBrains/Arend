package com.jetbrains.jetpad.vclang.error;

public abstract class ErrorClassifier<T> implements ErrorReporter<T> {
  private final ErrorReporter<T> myErrorReporter;

  public ErrorClassifier(ErrorReporter<T> errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError<T> error) {
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

  protected void reportedError(GeneralError<T> error) {}
  protected void reportedGoal(GeneralError<T> error) {}
  protected void reportedWarning(GeneralError<T> error) {}
  protected void reportedInfo(GeneralError<T> error) {}
}
