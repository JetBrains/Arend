package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class WrongDefinition extends NamingError {
  public final Abstract.Definition definition;

  public WrongDefinition(String message, Abstract.Definition definition, Abstract.SourceNode cause) {
    super(message, cause);
    this.definition = definition;
  }
}
