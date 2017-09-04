package com.jetbrains.jetpad.vclang.error;

public class DummyErrorReporter<T> implements ErrorReporter<T> {
  public static final DummyErrorReporter INSTANCE = new DummyErrorReporter<>();

  protected DummyErrorReporter() {}

  @Override
  public void report(GeneralError<T> error) {

  }
}
