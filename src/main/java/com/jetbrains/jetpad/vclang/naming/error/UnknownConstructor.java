package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class UnknownConstructor extends NamingError {
  public final String name;

  public UnknownConstructor(String name, Abstract.SourceNode cause) {
    super("Constructor '" + name + "' is not in scope", cause);
    this.name = name;
  }
}
