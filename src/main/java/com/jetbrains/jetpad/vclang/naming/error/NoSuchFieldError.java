package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NoSuchFieldError extends NamingError {
  public final String name;

  public NoSuchFieldError(String name, Abstract.SourceNode cause) {
    super("No such field: " + name, cause);
    this.name = name;
  }
}
