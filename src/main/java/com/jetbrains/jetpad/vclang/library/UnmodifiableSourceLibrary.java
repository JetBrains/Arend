package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;

/**
 * Provides an implementation of {@link Library#getModuleScopeProvider} in which the scopes are fixed after loading.
 */
public abstract class UnmodifiableSourceLibrary extends SourceLibrary {
  /**
   * Creates a new {@code UnmodifiableSourceLibrary}
   *
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  protected UnmodifiableSourceLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    // TODO: sources should report scopes somehow
  }
}
