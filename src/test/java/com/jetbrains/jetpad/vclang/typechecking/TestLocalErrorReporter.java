package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TestLocalErrorReporter implements LocalErrorReporter<Position> {
  private final ErrorReporter<Position> errorReporter;
  private final Concrete.Definition<Position> fakeDef;

  public TestLocalErrorReporter(ErrorReporter<Position> errorReporter) {
    this.errorReporter = errorReporter;
    GlobalReference reference = new GlobalReference("testDefinition");
    fakeDef = new Concrete.Definition<Position>(null, reference, Precedence.DEFAULT) {
      @Override
      public <P, R> R accept(ConcreteDefinitionVisitor<Position, ? super P, ? extends R> visitor, P params) {
        return null;
      }
    };
    reference.setDefinition(fakeDef);
  }

  @Override
  public void report(LocalTypeCheckingError<Position> localError) {
    errorReporter.report(new TypeCheckingError<>(fakeDef, localError));
  }

  @Override
  public void report(GeneralError<Position> error) {
    errorReporter.report(error);
  }
}
