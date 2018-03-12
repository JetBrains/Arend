package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A module scope provider that loads modules before obtaining their scope.
 * This modules scope provider reports errors, so it should be invoked at most once per module.
 */
public class LoadingModuleScopeProvider implements ModuleScopeProvider {
  private final SourceLoader mySourceLoader;

  public LoadingModuleScopeProvider(SourceLoader sourceLoader) {
    mySourceLoader = sourceLoader;
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    // If the module belongs to the loading library, load the module first.
    if (mySourceLoader.getLibrary().containsModule(module) && !mySourceLoader.load(module)) {
      return null;
    }

    Scope scope = mySourceLoader.getModuleScopeProvider().forModule(module);
    if (scope == null) {
      mySourceLoader.getErrorReporter().report(new ModuleNotFoundError(module));
    }
    return scope;
  }
}
