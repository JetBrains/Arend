package com.jetbrains.jetpad.vclang.module.scopeprovider;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CachingModuleScopeProvider implements ModuleScopeProvider {
  private final ModuleScopeProvider myModuleScopeProvider;
  private final Map<ModulePath, Scope> myScopes = new HashMap<>();

  private final static Scope NULL_SCOPE = new Scope() {};

  public CachingModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    myModuleScopeProvider = moduleScopeProvider;
  }

  public void reset(ModulePath modulePath) {
    myScopes.remove(modulePath);
  }

  public void reset() {
    myScopes.clear();
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    Scope scope = myScopes.get(module);
    if (scope == NULL_SCOPE) {
      return null;
    }
    if (scope != null) {
      return scope;
    }

    scope = myModuleScopeProvider.forModule(module);
    if (scope != null) {
      scope = CachingScope.make(scope);
    }
    myScopes.put(module, scope == null ? NULL_SCOPE : scope);
    return scope;
  }
}
