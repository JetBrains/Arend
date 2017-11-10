package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.frontend.namespace.CacheScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CacheModuleScopeProvider extends SimpleModuleScopeProvider {
  private final Map<ModulePath, CacheScope> cachedScopes = new HashMap<>();

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    Scope scope = forLoadedModule(module);
    if (scope != null) {
      return scope;
    } else {
      CacheScope cacheScope = forCacheModule(module);
      if (cacheScope != null) {
        return cacheScope.root;
      } else {
        return null;
      }
    }
  }

  public Scope forLoadedModule(@Nonnull ModulePath module) {
    return super.forModule(module);
  }

  public CacheScope forCacheModule(@Nonnull ModulePath module) {
    return cachedScopes.get(module);
  }

  public CacheScope ensureForCacheModule(@Nonnull ModulePath module) {
    return cachedScopes.computeIfAbsent(module, m -> new CacheScope());
  }
}
