package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.frontend.namespace.CacheScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CacheModuleScopeProvider extends SimpleModuleScopeProvider {
  public final Map<ModulePath, CacheScope> cachedScopes = new HashMap<>();

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    Scope scope = super.forModule(module);
    if (scope != null) {
      return scope;
    } else {
      CacheScope cacheScope = cachedScopes.get(module);
      if (cacheScope != null) {
        return cacheScope.root;
      } else {
        return null;
      }
    }
  }
}
