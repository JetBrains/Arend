package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.Error;

import javax.annotation.Nonnull;

public class LocalError extends Error {
  public LocalError(@Nonnull Level level, String message) {
    super(level, message);
  }
}
