package org.arend.error;

public class DummyErrorReporter implements ErrorReporter {
  public static final DummyErrorReporter INSTANCE = new DummyErrorReporter();

  private DummyErrorReporter() {}

  @Override
  public void report(GeneralError error) {

  }
}
