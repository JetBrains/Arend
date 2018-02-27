package com.jetbrains.jetpad.vclang.module.scopeprovider;

import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.library.resolver.ModuleLocator;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LocatingModuleScopeProvider implements ModuleScopeProvider {
  private final ModuleLocator myModuleLocator;

  public LocatingModuleScopeProvider(ModuleLocator moduleLocator) {
    myModuleLocator = moduleLocator;
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    Library library = myModuleLocator.locate(module);
    return library == null ? null : library.getModuleScopeProvider().forModule(module);
  }
}
