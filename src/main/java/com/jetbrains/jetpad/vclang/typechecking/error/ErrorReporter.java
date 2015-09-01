package com.jetbrains.jetpad.vclang.typechecking.error;

public interface ErrorReporter {
  void report(GeneralError error);
}
