package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class DummyLocalErrorReporter extends DummyErrorReporter implements LocalErrorReporter {
  public static final DummyLocalErrorReporter INSTANCE = new DummyLocalErrorReporter();

  private DummyLocalErrorReporter() {}

  @Override
  public void report(LocalTypeCheckingError localError) {}
}
