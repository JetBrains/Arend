package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

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
