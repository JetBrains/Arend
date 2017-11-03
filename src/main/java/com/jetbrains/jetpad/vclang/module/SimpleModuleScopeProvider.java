package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SimpleModuleScopeProvider implements ModuleScopeProvider, ModuleRegistry {
  private final Map<ModulePath, Scope> myMap = new HashMap<>();

  @Override
  public void registerModule(ModulePath module, Group group) {
    myMap.put(module, CachingScope.make(LexicalScope.opened(group)));
  }

  @Override
  public void unregisterModule(ModulePath path) {
    myMap.remove(path);
  }

  @Override
  public boolean isRegistered(ModulePath modulePath) {
    return myMap.containsKey(modulePath);
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    return myMap.get(module);
  }
}
