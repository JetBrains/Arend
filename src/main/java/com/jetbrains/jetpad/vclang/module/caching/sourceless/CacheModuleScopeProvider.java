package com.jetbrains.jetpad.vclang.module.caching.sourceless;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheDefinitionRegistry;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheModuleScopeProvider implements ModuleScopeProvider, CacheDefinitionRegistry {
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

  @Override
  public GlobalReferable registerDefinition(ModulePath module, List<String> path, Precedence precedence, GlobalReferable parent) {
    CacheScope cacheScope = ensureForCacheModule(module);
    return cacheScope.registerDefinition(path, precedence, parent);
  }
}
