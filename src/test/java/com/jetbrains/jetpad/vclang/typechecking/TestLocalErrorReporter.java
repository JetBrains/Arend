package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TestLocalErrorReporter implements LocalErrorReporter {
  private final ErrorReporter errorReporter;
  private final Abstract.Definition fakeDef = new Abstract.Definition() {
    @Override
    public Abstract.Precedence getPrecedence() {
      return null;
    }

    @Override
    public Abstract.Definition getParentDefinition() {
      return null;
    }

    @Override
    public boolean isStatic() {
      return true;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return null;
    }

    @Override
    public String getName() {
      return "testDefinition";
    }
  };

  public TestLocalErrorReporter(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public void report(LocalTypeCheckingError localError) {
    errorReporter.report(new TypeCheckingError(fakeDef, localError));
  }

  @Override
  public void report(GeneralError error) {
    errorReporter.report(error);
  }
}
