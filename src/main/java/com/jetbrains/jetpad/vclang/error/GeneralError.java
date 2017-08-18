package com.jetbrains.jetpad.vclang.error;

import javax.annotation.Nonnull;

/**
 * Marker class for errors that can be readily reported to an ErrorReporter.
 */
public class GeneralError<T> extends Error<T> {
  public GeneralError(@Nonnull Level level, String message) {
    super(level, message);
  }
}
