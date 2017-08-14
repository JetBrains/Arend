package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class WrongReferable extends NamingError {
  public final Abstract.ReferableSourceNode referable;

  public WrongReferable(String message, Abstract.ReferableSourceNode referable, Abstract.SourceNode cause) {
    super(message, cause);
    this.referable = referable;
  }
}
