package com.jetbrains.jetpad.vclang.typechecking.error.reporter;

import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public interface ErrorReporter {
  void report(GeneralError error);
}
