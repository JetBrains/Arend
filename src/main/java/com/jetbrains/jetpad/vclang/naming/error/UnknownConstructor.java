package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class UnknownConstructor extends NamingError {
  public final String name;

  public UnknownConstructor(String name, Concrete.SourceNode cause) {
    super("Constructor '" + name + "' is not in scope", cause);
    this.name = name;
  }
}
