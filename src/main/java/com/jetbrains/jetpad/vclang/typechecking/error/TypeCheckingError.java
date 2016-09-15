package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

/**
 * If you would like to add a new type checking error, please, extend {@link LocalTypeCheckingError} instead.
 */
public class TypeCheckingError extends GeneralError {
  public final Abstract.Definition definition;
  public final LocalTypeCheckingError localError;

  public TypeCheckingError(Abstract.Definition definition, LocalTypeCheckingError localError) {
    super(localError.level, localError.message, localError.cause);
    this.definition = definition;
    this.localError = localError;
  }
}
