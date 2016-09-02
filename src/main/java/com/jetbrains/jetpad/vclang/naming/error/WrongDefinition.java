package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class WrongDefinition extends NamingError {
  public WrongDefinition(String message, Abstract.SourceNode cause) {
    super(message, cause);
  }
}
