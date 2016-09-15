package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class UnresolvedReferenceError extends LocalTypeCheckingError {
  public final String name;

  public UnresolvedReferenceError(Abstract.SourceNode expression, String name) {
    // FIXME[errorformat]
    super("Unresolved reference '" + name + "'", expression);
    this.name = name;
  }
}
