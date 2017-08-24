package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class HasErrors<T> extends LocalTypeCheckingError<T> {
  public HasErrors(Level level, GlobalReferable definition, Concrete.SourceNode<T> expression) {
    super(level, definition.textRepresentation() + " has errors", expression);
  }
}
