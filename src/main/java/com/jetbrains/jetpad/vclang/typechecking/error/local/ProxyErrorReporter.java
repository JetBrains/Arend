package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import javax.annotation.Nonnull;

public class ProxyErrorReporter<T> implements LocalErrorReporter<T> {
  private final Concrete.Definition<T> myDefinition;
  private final ErrorReporter<T> myErrorReporter;

  public ProxyErrorReporter(@Nonnull Concrete.Definition<T> definition, ErrorReporter<T> errorReporter) {
    myDefinition = definition;
    myErrorReporter = errorReporter;
  }

  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  public ErrorReporter<T> getUnderlyingErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public void report(GeneralError<T> error) {
    myErrorReporter.report(error);
  }

  @Override
  public void report(LocalTypeCheckingError<T> localError) {
    myErrorReporter.report(new TypeCheckingError<>(myDefinition, localError));
  }
}
