package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

@Deprecated
public class NotInScopeError extends TypeCheckingError {
  public final String name;

  public NotInScopeError(Abstract.Definition definition, Abstract.SourceNode cause, String name) {
    super(definition, "Not in scope: " + name, cause);
    this.name = name;
  }

  @Deprecated
  public NotInScopeError(Abstract.Definition definition, String name) {
    super(definition, "Not in scope: " + name, null);
    this.name = name;
  }
}
