package org.arend.error;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;

public class DummyErrorReporter implements ErrorReporter {
  public static final DummyErrorReporter INSTANCE = new DummyErrorReporter();

  private DummyErrorReporter() {}

  @Override
  public void report(GeneralError error) {

  }
}
