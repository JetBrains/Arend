package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import javax.annotation.Nonnull;

/**
 * If you would like to add a new type checking error, please, extend {@link LocalTypeCheckingError} instead.
 */
public class TypeCheckingError extends GeneralError {
  public final Abstract.Definition definition;
  public final LocalTypeCheckingError localError;

  public TypeCheckingError(@Nonnull Abstract.Definition definition, LocalTypeCheckingError localError) {
    super(localError.level, localError.message, localError.cause);
    this.definition = definition;
    this.localError = localError;
  }

  @Override
  public Doc getDoc(SourceInfoProvider src) {
    String name = src.nameFor(definition);
    if (name == null && definition.getName() != null) {
      name = "???." + definition.getName();
    }

    return DocFactory.hang(localError.getHeaderDoc(src), DocFactory.vList(
      localError.getBodyDoc(src),
      localError.getCauseDoc(),
      DocFactory.text("While typechecking: " + name)));
  }
}
