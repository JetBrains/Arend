package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class WrongReferable<T> extends NamingError<T> {
  public final Abstract.ReferableSourceNode referable;

  public WrongReferable(String message, Abstract.ReferableSourceNode referable, Concrete.SourceNode<T> cause) {
    super(message, cause);
    this.referable = referable;
  }
}
