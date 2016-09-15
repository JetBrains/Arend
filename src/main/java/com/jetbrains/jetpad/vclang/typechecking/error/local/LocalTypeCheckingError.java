package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class LocalTypeCheckingError extends Error {
  public LocalTypeCheckingError(Level level, String message, Abstract.SourceNode cause) {
    super(level, message, cause);
  }

  public LocalTypeCheckingError(String message, Abstract.SourceNode cause) {
    super(message, cause);
  }
}
