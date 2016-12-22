package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class ProxyErrorReporter implements LocalErrorReporter {
  private final Abstract.Definition myDefinition;
  private final ErrorReporter myErrorReporter;

  public ProxyErrorReporter(Abstract.Definition definition, ErrorReporter errorReporter) {
    myDefinition = definition;
    myErrorReporter = errorReporter;
  }

  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  public ErrorReporter getUnderlyingErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public void report(GeneralError error) {
    myErrorReporter.report(error);
  }

  @Override
  public void report(LocalTypeCheckingError localError) {
    myErrorReporter.report(new TypeCheckingError(myDefinition, localError));
  }
}
