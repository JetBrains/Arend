package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class WrongReferable<T> extends NamingError<T> {
  public final Referable referable;

  public WrongReferable(String message, Referable referable, Concrete.SourceNode<T> cause) {
    super(message, cause);
    this.referable = referable;
  }
}
