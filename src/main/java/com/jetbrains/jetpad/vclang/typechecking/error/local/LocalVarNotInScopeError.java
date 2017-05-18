package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

@Deprecated
public class LocalVarNotInScopeError extends LocalTypeCheckingError {
  public final String name;

  public LocalVarNotInScopeError(Abstract.SourceNode cause, String name) {
    super("Local variable not in scope: " + name, cause);
    this.name = name;
  }
}
