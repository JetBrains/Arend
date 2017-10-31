package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.scope.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SimpleModuleScopeProvider implements ModuleScopeProvider {
  private final Map<ModulePath, Scope> myMap = new HashMap<>();
  public static final SimpleModuleScopeProvider INSTANCE = new SimpleModuleScopeProvider();

  private SimpleModuleScopeProvider() {}

  public void registerModule(ModulePath module, Scope scope) {
    myMap.put(module, scope);
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module, boolean includeExports) {
    return myMap.get(module);
  }
}
