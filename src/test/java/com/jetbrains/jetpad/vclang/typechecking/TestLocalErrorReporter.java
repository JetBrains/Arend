package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TestLocalErrorReporter implements LocalErrorReporter {
  private final ErrorReporter errorReporter;
  private final Concrete.Definition fakeDef;

  public TestLocalErrorReporter(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
    GlobalReference reference = new GlobalReference("testDefinition", Precedence.DEFAULT);
    fakeDef = new Concrete.Definition(null, reference) {
      @Override
      public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
        return null;
      }
    };
    reference.setDefinition(fakeDef);
  }

  @Override
  public void report(LocalTypeCheckingError localError) {
    errorReporter.report(new TypeCheckingError(fakeDef.getReferable(), localError));
  }

  @Override
  public void report(GeneralError error) {
    errorReporter.report(error);
  }
}
