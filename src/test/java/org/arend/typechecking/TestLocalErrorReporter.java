package org.arend.typechecking;

import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.Precedence;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.ProxyError;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;

public class TestLocalErrorReporter implements LocalErrorReporter {
  private final ErrorReporter errorReporter;
  private final GlobalReferable fakeDef;

  public TestLocalErrorReporter(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
    fakeDef = new GlobalReferable() {
      @Nonnull
      @Override
      public Precedence getPrecedence() {
        return Precedence.DEFAULT;
      }

      @Nonnull
      @Override
      public String textRepresentation() {
        return "testDefinition";
      }
    };
  }

  @Override
  public void report(LocalError localError) {
    errorReporter.report(new ProxyError(fakeDef, localError));
  }

  @Override
  public void report(GeneralError error) {
    errorReporter.report(error);
  }
}
