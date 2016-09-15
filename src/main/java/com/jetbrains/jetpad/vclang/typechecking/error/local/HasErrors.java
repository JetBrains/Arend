package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends LocalTypeCheckingError {
  public HasErrors(Abstract.Definition cause, Abstract.SourceNode expression) {
    super(cause.getName() + " has errors", expression);
  }
}
