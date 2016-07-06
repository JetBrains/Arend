package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class UnresolvedReferenceError extends TypeCheckingError {
  public final String name;

  public UnresolvedReferenceError(Abstract.Definition definition, Abstract.SourceNode expression, String name) {
    // FIXME[format]
    super(definition, "Unresolved reference '" + name + "'", expression);
    this.name = name;
  }
}
