package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModuleScopeProvider {
  @Nullable
  Scope forModule(@NotNull ModulePath module, @NotNull Scope.Kind kind);

  default @Nullable Scope forModule(@NotNull ModulePath module) {
    return forModule(module, Scope.Kind.EXPR);
  }

  default boolean isCaching() {
    return false;
  }
}
