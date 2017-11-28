package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

public class CompositeLocalErrorReporter implements LocalErrorReporter {
  private final LocalErrorReporter myLocalErrorReporter;
  private final ErrorReporter myErrorReporter;

  public CompositeLocalErrorReporter(LocalErrorReporter localErrorReporter, ErrorReporter errorReporter) {
    myLocalErrorReporter = localErrorReporter;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    myLocalErrorReporter.report(error);
    myErrorReporter.report(error);
  }

  @Override
  public void report(LocalError localError) {
    myLocalErrorReporter.report(localError);
  }
}
