package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class NoSuchFieldError extends NamingError {
  public final String name;

  public NoSuchFieldError(String name, Concrete.SourceNode cause) {
    super("No such field: " + name, cause);
    this.name = name;
  }
}
