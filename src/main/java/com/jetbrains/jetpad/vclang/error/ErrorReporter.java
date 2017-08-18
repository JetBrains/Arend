package com.jetbrains.jetpad.vclang.error;

public interface ErrorReporter<T> {
  void report(GeneralError<T> error);
}
