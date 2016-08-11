package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class DuplicateDefinitionError extends NamingError {
  public final Abstract.Definition definition1;
  public final Abstract.Definition definition2;

  public DuplicateDefinitionError(Abstract.Definition definition1, Abstract.Definition definition2) {
    super("Duplicate definition name", definition2);
    this.definition1 = definition1;
    this.definition2 = definition2;
  }
}
