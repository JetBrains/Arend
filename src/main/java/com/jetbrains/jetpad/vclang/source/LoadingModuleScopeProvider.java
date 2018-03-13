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
  private final ModulePath myCurrentModule;

  public LoadingModuleScopeProvider(SourceLoader sourceLoader, ModulePath currentModule) {
    mySourceLoader = sourceLoader;
    myCurrentModule = currentModule;
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    Scope scope = mySourceLoader.getModuleScopeProvider().forModule(module);
    if (scope != null) {
      return scope;
    }

    if (!mySourceLoader.load(module)) {
      return null;
    }

    scope = mySourceLoader.getModuleScopeProvider().forModule(module);
    if (scope == null) {
      mySourceLoader.getErrorReporter().report(new ModuleNotFoundError(module, myCurrentModule));
    }
    return scope;
  }
}
