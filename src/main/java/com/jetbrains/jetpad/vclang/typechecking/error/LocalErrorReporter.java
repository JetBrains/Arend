package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public interface LocalErrorReporter extends ErrorReporter {
  void report(LocalTypeCheckingError localError);
}
