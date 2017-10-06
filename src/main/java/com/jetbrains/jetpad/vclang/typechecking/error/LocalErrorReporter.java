package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

public interface LocalErrorReporter extends ErrorReporter {
  void report(LocalError localError);
}
