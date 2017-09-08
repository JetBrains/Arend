package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class WrongReferable extends NamingError {
  public final Referable referable;

  public WrongReferable(String message, Referable referable, Concrete.SourceNode cause) {
    super(message, cause);
    this.referable = referable;
  }
}
