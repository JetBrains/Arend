package com.jetbrains.jetpad.vclang.module.caching.sourceless;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheDefinitionRegistry;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheModuleScopeProvider<SourceIdT extends SourceId> implements ModuleScopeProvider, CacheDefinitionRegistry {
  private final ModuleScopeProvider mySourceScopeProvider;
  private SourceSupplier<SourceIdT> mySourceSupplier;
  private CacheManager<SourceIdT> myCacheManager;
  private final Map<ModulePath, CacheScope> myCachedScopes = new HashMap<>();

  public CacheModuleScopeProvider(ModuleScopeProvider sourceScopeProvider) {
    mySourceScopeProvider = sourceScopeProvider;
  }

  public void initialise(SourceSupplier<SourceIdT> sourceSupplier, CacheManager<SourceIdT> cacheManager) {
    assert mySourceSupplier != null && myCacheManager != null;
    mySourceSupplier = sourceSupplier;
    myCacheManager = cacheManager;
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    CacheScope cacheScope = forCacheModule(module);
    if (cacheScope != null) {
      return cacheScope.root;
    } else {
      return forLoadedModule(module);
    }
  }

  public Scope forLoadedModule(@Nonnull ModulePath module) {
    return mySourceScopeProvider.forModule(module);
  }

  public CacheScope forCacheModule(@Nonnull ModulePath module) {
    CacheScope scope = myCachedScopes.get(module);
    if (scope != null) {
      return scope;
    } else {
      SourceIdT sourceId = mySourceSupplier.locateModule(module);
      if (sourceId != null) {
        try {
          myCacheManager.loadCache(sourceId);
        } catch (CacheLoadingException e) {
          e.printStackTrace();
        }
      }
      return myCachedScopes.get(module);
    }
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
