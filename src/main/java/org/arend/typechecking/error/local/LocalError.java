package org.arend.typechecking.error.local;

import org.arend.error.Error;

import javax.annotation.Nonnull;

public class LocalError extends Error {
  public LocalError(@Nonnull Level level, String message) {
    super(level, message);
  }
}
