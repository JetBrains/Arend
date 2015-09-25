package com.jetbrains.jetpad.vclang.typechecking.error.reporter;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class LocalErrorReporter implements ErrorReporter {
  private final Namespace myNamespace;
  private final ErrorReporter myErrorReporter;

  public LocalErrorReporter(Namespace namespace, ErrorReporter errorReporter) {
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  @Override
  public void report(GeneralError error) {
    error.setNamespace(myNamespace);
    myErrorReporter.report(error);
  }
}
