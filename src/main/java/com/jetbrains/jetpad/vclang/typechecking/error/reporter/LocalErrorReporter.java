package com.jetbrains.jetpad.vclang.typechecking.error.reporter;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class LocalErrorReporter implements ErrorReporter {
  private final ResolvedName myResolvedName;
  private final ErrorReporter myErrorReporter;

  public LocalErrorReporter(ResolvedName resolvedName, ErrorReporter errorReporter) {
    myResolvedName = resolvedName;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    error.setResolvedName(myResolvedName);
    myErrorReporter.report(error);
  }
}
