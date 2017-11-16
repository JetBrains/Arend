package com.jetbrains.jetpad.vclang.module.caching.sourceless;

import com.jetbrains.jetpad.vclang.frontend.namespace.CacheScope;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.scope.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CacheModuleScopeProvider implements ModuleScopeProvider {
  private final ModuleScopeProvider mySourceScopeProvider;
  private final Map<ModulePath, CacheScope> myCachedScopes = new HashMap<>();

  public CacheModuleScopeProvider(ModuleScopeProvider soureScopeProvider) {
    mySourceScopeProvider = soureScopeProvider;
  }

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
    return mySourceScopeProvider.forModule(module);
  }

  public CacheScope forCacheModule(@Nonnull ModulePath module) {
    return myCachedScopes.get(module);
  }

  public CacheScope ensureForCacheModule(@Nonnull ModulePath module) {
    return myCachedScopes.computeIfAbsent(module, m -> new CacheScope());
  }
}
