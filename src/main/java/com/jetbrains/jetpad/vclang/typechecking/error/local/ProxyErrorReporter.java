package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import javax.annotation.Nonnull;

public class ProxyErrorReporter implements LocalErrorReporter {
  private final Concrete.Definition myDefinition;
  private final ErrorReporter myErrorReporter;

  public ProxyErrorReporter(@Nonnull Concrete.Definition definition, ErrorReporter errorReporter) {
    myDefinition = definition;
    myErrorReporter = errorReporter;
  }

  public Concrete.Definition getDefinition() {
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
    myErrorReporter.report(new TypeCheckingError(myDefinition.getData(), localError));
  }
}
