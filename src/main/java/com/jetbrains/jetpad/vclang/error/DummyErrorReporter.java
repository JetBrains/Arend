package com.jetbrains.jetpad.vclang.error;

public class DummyErrorReporter implements ErrorReporter {
  public static final DummyErrorReporter INSTANCE = new DummyErrorReporter();

  protected DummyErrorReporter() {}

  @Override
  public void report(GeneralError error) {

  }
}
