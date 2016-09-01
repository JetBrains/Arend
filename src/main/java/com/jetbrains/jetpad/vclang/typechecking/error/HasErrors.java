package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends TypeCheckingError {
  public HasErrors(Abstract.Definition definition, Abstract.Definition cause, Abstract.SourceNode expression) {
    super(definition, cause.getName() + " has errors", expression);
  }
}
