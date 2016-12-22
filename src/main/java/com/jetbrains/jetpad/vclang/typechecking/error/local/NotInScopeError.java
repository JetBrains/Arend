package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

@Deprecated
public class NotInScopeError extends LocalTypeCheckingError {
  public final String name;

  public NotInScopeError(Abstract.SourceNode cause, String name) {
    super("Not in scope: " + name, cause);
    this.name = name;
  }
}
