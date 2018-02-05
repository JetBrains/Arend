package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides an implementation of {@link Library#getModuleScopeProvider} in which the scopes are fixed after loading.
 */
public abstract class UnmodifiableSourceLibrary extends SourceLibrary {
  private final SimpleModuleScopeProvider myModuleScopeProvider = new SimpleModuleScopeProvider();

  /**
   * Creates a new {@code UnmodifiableSourceLibrary}
   *
   * @param name              the name of this library.
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  protected UnmodifiableSourceLibrary(String name, TypecheckerState typecheckerState) {
    super(name, typecheckerState);
  }

  @Nullable
  @Override
  public final Source getRawSource(ModulePath modulePath) {
    return null;
  }

  @Override
  public void setModuleGroup(ModulePath modulePath, Group group) {
    myModuleScopeProvider.registerModule(modulePath, group);
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return myModuleScopeProvider;
  }
}
