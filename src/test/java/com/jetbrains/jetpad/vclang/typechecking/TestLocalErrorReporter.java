package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TestLocalErrorReporter<T> implements LocalErrorReporter<T> {
  private final ErrorReporter<T> errorReporter;
  private final Concrete.Definition<T> fakeDef = new Concrete.Definition<T>(null, "testDefinition", Abstract.Precedence.DEFAULT) {
    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return null;
    }
  };

  public TestLocalErrorReporter(ErrorReporter<T> errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public void report(LocalTypeCheckingError<T> localError) {
    errorReporter.report(new TypeCheckingError<>(fakeDef, localError));
  }

  @Override
  public void report(GeneralError<T> error) {
    errorReporter.report(error);
  }
}
