package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class DummyLocalErrorReporter<T> extends DummyErrorReporter<T> implements LocalErrorReporter<T> {
  public static final DummyLocalErrorReporter INSTANCE = new DummyLocalErrorReporter<>();

  private DummyLocalErrorReporter() {}

  @Override
  public void report(LocalTypeCheckingError<T> localError) {}
}
