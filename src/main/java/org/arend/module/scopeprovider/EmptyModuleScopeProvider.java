package org.arend.module.scopeprovider;

import org.arend.module.ModulePath;
import org.arend.naming.scope.Scope;

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
