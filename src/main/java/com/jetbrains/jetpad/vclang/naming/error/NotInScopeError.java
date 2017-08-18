package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class NotInScopeError<T> extends NamingError<T> {
  public final String name;

  public NotInScopeError(String name, Concrete.SourceNode<T> cause) {
    super("Not in scope: " + name, cause);
    this.name = name;
  }
}
