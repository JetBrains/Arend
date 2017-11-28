package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Errors that can be readily reported to an ErrorReporter.
 */
public abstract class GeneralError extends Error {
  public GeneralError(@Nonnull Level level, String message) {
    super(level, message);
  }

  public abstract Collection<? extends GlobalReferable> getAffectedDefinitions();
}
