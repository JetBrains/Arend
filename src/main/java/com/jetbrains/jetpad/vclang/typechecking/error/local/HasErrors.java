package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends LocalTypeCheckingError {
  public HasErrors(Level level, Abstract.Definition cause, Abstract.SourceNode expression) {
    super(level, cause.getName() + " has errors", expression);
  }

  public HasErrors(Abstract.Definition cause, Abstract.SourceNode expression) {
    this(Level.ERROR, cause, expression);
  }
}
