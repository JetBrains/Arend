package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends LocalTypeCheckingError {
  public HasErrors(Level level, Abstract.GlobalReferableSourceNode definition, Abstract.SourceNode expression) {
    super(level, definition.getName() + " has errors", expression);
  }

  public HasErrors(Abstract.GlobalReferableSourceNode definition, Abstract.SourceNode expression) {
    this(Level.ERROR, definition, expression);
  }
}
