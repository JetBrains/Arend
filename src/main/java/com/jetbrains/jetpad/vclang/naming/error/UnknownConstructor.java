package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class UnknownConstructor<T> extends NamingError<T> {
  public final String name;

  public UnknownConstructor(String name, Concrete.SourceNode<T> cause) {
    super("Constructor '" + name + "' is not in scope", cause);
    this.name = name;
  }
}
