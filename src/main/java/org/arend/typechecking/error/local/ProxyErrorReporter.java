package org.arend.typechecking.error.local;

import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.ProxyError;

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
