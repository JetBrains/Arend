package org.arend.error;

import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.LocalError;

public class DummyErrorReporter implements LocalErrorReporter {
  public static final DummyErrorReporter INSTANCE = new DummyErrorReporter();

  private DummyErrorReporter() {}

  @Override
  public void report(GeneralError error) {

  }

  @Override
  public void report(LocalError localError) {

  }
}
