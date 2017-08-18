package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class HasErrors<T> extends LocalTypeCheckingError<T> {
  public HasErrors(Level level, Abstract.GlobalReferableSourceNode definition, Concrete.SourceNode<T> expression) {
    super(level, definition.getName() + " has errors", expression);
  }
}
