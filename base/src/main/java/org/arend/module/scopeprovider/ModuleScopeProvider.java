package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModuleScopeProvider extends org.arend.ext.module.ModuleScopeProvider {
  @Nullable
  @Override
  Scope forModule(@NotNull ModulePath module);
}
