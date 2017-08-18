package com.jetbrains.jetpad.vclang.error;

public class DummyErrorReporter<T> implements ErrorReporter<T> {
  @Override
  public void report(GeneralError<T> error) {

  }
}
