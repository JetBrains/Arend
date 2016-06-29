package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

public class DuplicateDefinitionError extends GeneralError {
  public final Referable definition1;
  public final Referable definition2;

  public DuplicateDefinitionError(Referable definition1, Referable definition2) {
    super("Duplicate definition name '" + definition1.getName() + "'", definition2);
    this.definition1 = definition1;
    this.definition2 = definition2;
  }
}
