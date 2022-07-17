package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyModuleScopeProvider implements ModuleScopeProvider {
  public final static EmptyModuleScopeProvider INSTANCE = new EmptyModuleScopeProvider();

  private EmptyModuleScopeProvider() {}

  @Override
  public @Nullable Scope forModule(@NotNull ModulePath module, Scope.@NotNull Kind kind) {
    return null;
  }

  @Override
  public boolean isCaching() {
    return true;
  }
}
