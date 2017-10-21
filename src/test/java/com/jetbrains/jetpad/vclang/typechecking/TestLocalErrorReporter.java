package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

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
