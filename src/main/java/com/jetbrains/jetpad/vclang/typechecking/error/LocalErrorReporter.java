package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class LocalErrorReporter implements ErrorReporter {
  private final Abstract.Definition myDefinition;
  private final ErrorReporter myErrorReporter;

  public LocalErrorReporter(Abstract.Definition definition, ErrorReporter errorReporter) {
    myDefinition = definition;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    myErrorReporter.report(error);
  }

  public void report(TypeCheckingError error) {
    error.setDefinition(myDefinition);
    myErrorReporter.report(error);
  }
}
