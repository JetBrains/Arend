package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class NamingError extends GeneralError {
  public NamingError(String message, Abstract.SourceNode cause) {
    super(message, cause);
  }

  public NamingError(Level level, String message, Abstract.SourceNode cause) {
    super(level, message, cause);
  }
}
