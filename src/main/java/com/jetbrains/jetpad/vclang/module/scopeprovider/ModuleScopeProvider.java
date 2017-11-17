package com.jetbrains.jetpad.vclang.module.scopeprovider;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModuleScopeProvider {
  @Nullable
  Scope forModule(@Nonnull ModulePath module);
}
