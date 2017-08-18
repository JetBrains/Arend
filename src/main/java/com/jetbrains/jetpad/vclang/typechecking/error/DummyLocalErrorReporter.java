package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class DummyLocalErrorReporter<T> extends DummyErrorReporter<T> implements LocalErrorReporter<T> {
  @Override
  public void report(LocalTypeCheckingError<T> localError) {}
}
