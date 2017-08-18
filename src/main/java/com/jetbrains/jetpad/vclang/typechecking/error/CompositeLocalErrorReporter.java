package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class CompositeLocalErrorReporter<T> implements LocalErrorReporter<T> {
  private final LocalErrorReporter<T> myLocalErrorReporter;
  private final ErrorReporter<T> myErrorReporter;

  public CompositeLocalErrorReporter(LocalErrorReporter<T> localErrorReporter, ErrorReporter<T> errorReporter) {
    myLocalErrorReporter = localErrorReporter;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError<T> error) {
    myLocalErrorReporter.report(error);
    myErrorReporter.report(error);
  }

  @Override
  public void report(LocalTypeCheckingError<T> localError) {
    myLocalErrorReporter.report(localError);
  }
}
