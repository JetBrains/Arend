package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;

import javax.annotation.Nonnull;

public class ProxyErrorReporter implements LocalErrorReporter {
  private final GlobalReferable myDefinition;
  private final ErrorReporter myErrorReporter;

  public ProxyErrorReporter(@Nonnull GlobalReferable definition, ErrorReporter errorReporter) {
    myDefinition = definition;
    myErrorReporter = errorReporter;
  }

  public GlobalReferable getDefinition() {
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
  public void report(LocalError localError) {
    myErrorReporter.report(new ProxyError(myDefinition, localError));
  }
}
