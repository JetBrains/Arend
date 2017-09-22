package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

public class HasErrors extends LocalTypeCheckingError {
  public HasErrors(Level level, GlobalReferable definition, Concrete.SourceNode expression) {
    super(level, definition.textRepresentation() + " has errors", expression);
  }
}
