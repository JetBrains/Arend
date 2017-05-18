package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

@Deprecated
public class ConstructorNotInScopeError extends LocalTypeCheckingError {
  public final String name;

  public ConstructorNotInScopeError(String name, Abstract.SourceNode cause) {
    super("Constructor not in scope: " + name, cause);
    this.name = name;
  }
}
