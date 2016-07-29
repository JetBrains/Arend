package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends TypeCheckingError {
  public HasErrors(Abstract.Definition definition, String name, Abstract.SourceNode expression) {
    super(definition, name + " has errors", expression);
  }
}
