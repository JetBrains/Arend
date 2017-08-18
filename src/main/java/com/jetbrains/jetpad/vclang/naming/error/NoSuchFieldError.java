package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class NoSuchFieldError<T> extends NamingError<T> {
  public final String name;

  public NoSuchFieldError(String name, Concrete.SourceNode<T> cause) {
    super("No such field: " + name, cause);
    this.name = name;
  }
}
