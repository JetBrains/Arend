package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TestLocalErrorReporter implements LocalErrorReporter {
  private final ErrorReporter errorReporter;

  public TestLocalErrorReporter(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public void report(LocalTypeCheckingError localError) {
    errorReporter.report(new TypeCheckingError(null, localError));
  }

  @Override
  public void report(GeneralError error) {
    errorReporter.report(error);
  }
}
