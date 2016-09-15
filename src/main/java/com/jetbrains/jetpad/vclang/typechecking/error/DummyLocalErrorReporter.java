package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class DummyLocalErrorReporter extends DummyErrorReporter implements LocalErrorReporter {
  @Override
  public void report(LocalTypeCheckingError localError) {}
}
