package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CachingModuleScopeProvider implements ModuleScopeProvider {
  private final ModuleScopeProvider myModuleScopeProvider;
  private final Map<ModulePath, Scope> myExprScopes = new HashMap<>();
  private final Map<ModulePath, Scope> myPLevelScopes = new HashMap<>();
  private final Map<ModulePath, Scope> myHLevelScopes = new HashMap<>();

  private final static Scope NULL_SCOPE = new Scope() {};

  private CachingModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    myModuleScopeProvider = moduleScopeProvider;
  }

  public static ModuleScopeProvider make(ModuleScopeProvider moduleScopeProvider) {
    return moduleScopeProvider.isCaching() ? moduleScopeProvider : new CachingModuleScopeProvider(moduleScopeProvider);
  }

  public void reset(ModulePath modulePath) {
    myExprScopes.remove(modulePath);
    myPLevelScopes.remove(modulePath);
    myHLevelScopes.remove(modulePath);
  }

  public void reset() {
    myExprScopes.clear();
    myPLevelScopes.clear();
    myHLevelScopes.clear();
  }

  @Nullable
  @Override
  public Scope forModule(@NotNull ModulePath module, @NotNull Scope.Kind kind) {
    Map<ModulePath, Scope> scopes = kind == Scope.Kind.EXPR ? myExprScopes : kind == Scope.Kind.PLEVEL ? myPLevelScopes : myHLevelScopes;
    Scope scope = scopes.get(module);
    if (scope == NULL_SCOPE) {
      return null;
    }
    if (scope != null) {
      return scope;
    }

    scope = myModuleScopeProvider.forModule(module, kind);
    if (scope != null) {
      scope = CachingScope.make(scope);
    }
    scopes.put(module, scope == null ? NULL_SCOPE : scope);
    return scope;
  }

  @Override
  public boolean isCaching() {
    return true;
  }
}
