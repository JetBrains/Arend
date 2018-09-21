package org.arend.module.scopeprovider;

import org.arend.module.ModulePath;
import org.arend.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModuleScopeProvider {
  @Nullable
  Scope forModule(@Nonnull ModulePath module);
}
