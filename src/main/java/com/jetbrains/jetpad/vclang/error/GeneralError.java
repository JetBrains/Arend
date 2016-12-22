package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

/**
 * Marker class for errors that can be readily reported to an ErrorReporter.
 */
public class GeneralError extends Error {
  public GeneralError(String message, Abstract.SourceNode cause) {
    super(message, cause);
  }

  public GeneralError(Level level, String message, Abstract.SourceNode cause) {
    super(level, message, cause);
  }
}
