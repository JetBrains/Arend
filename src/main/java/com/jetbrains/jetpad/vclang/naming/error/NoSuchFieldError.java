package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NoSuchFieldError extends NamingError {
  public final String name;

  public NoSuchFieldError(Abstract.SourceNode cause, String name) {
    super("No such field: " + name, cause);
    this.name = name;
  }
}
