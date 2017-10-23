package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptyModuleScopeProvider implements ModuleScopeProvider {
  public final static EmptyModuleScopeProvider INSTANCE = new EmptyModuleScopeProvider();

  private EmptyModuleScopeProvider() {}

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    return null;
  }
}
