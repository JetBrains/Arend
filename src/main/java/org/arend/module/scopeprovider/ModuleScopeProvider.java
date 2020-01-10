package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModuleScopeProvider extends org.arend.ext.module.ModuleScopeProvider {
  @Nullable
  @Override
  Scope forModule(@Nonnull ModulePath module);
}
