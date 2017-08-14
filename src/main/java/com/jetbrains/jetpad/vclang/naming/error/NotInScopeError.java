package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NotInScopeError extends NamingError {
  public final String name;

  public NotInScopeError(String name, Abstract.SourceNode cause) {
    super("Not in scope: " + name, cause);
    this.name = name;
  }
}
