package org.arend.typechecking.error.local;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.naming.reference.GlobalReferable;

public class LocalErrorReporter implements ErrorReporter {
  private final GlobalReferable myDefinition;
  private final ErrorReporter myErrorReporter;

  public LocalErrorReporter(GlobalReferable definition, ErrorReporter errorReporter) {
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
    if (error instanceof LocalError) {
      ((LocalError) error).definition = myDefinition;
    }
    myErrorReporter.report(error);
  }
}
