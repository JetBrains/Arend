package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Collections;

public class TestLocalErrorReporter implements LocalErrorReporter {
  private final ErrorReporter errorReporter;
  private final Abstract.Definition fakeDef = new Concrete.FunctionDefinition(null, "testDef", Abstract.Precedence.DEFAULT, Collections.emptyList(), null, Abstract.Definition.Arrow.RIGHT, null, Collections.emptyList());

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
