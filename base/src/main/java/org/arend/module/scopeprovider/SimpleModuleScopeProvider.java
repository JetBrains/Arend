package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleRegistry;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.term.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleModuleScopeProvider implements ModuleScopeProvider, ModuleRegistry {
  private final Map<ModulePath, Scope> myMap = new LinkedHashMap<>();

  @Override
  public void registerModule(ModulePath module, Group group) {
    myMap.put(module, CachingScope.make(LexicalScope.opened(group)));
  }

  public void addModule(ModulePath module, Scope scope) {
    myMap.put(module, scope);
  }

  public Collection<? extends ModulePath> getRegisteredModules() {
    return myMap.keySet();
  }

  public Collection<? extends Map.Entry<ModulePath, Scope>> getRegisteredEntries() {
    return myMap.entrySet();
  }

  @Override
  public void unregisterModule(ModulePath path) {
    myMap.remove(path);
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  public void clear() {
    myMap.clear();
  }

  @Override
  public boolean isRegistered(ModulePath modulePath) {
    return myMap.containsKey(modulePath);
  }

  @Nullable
  @Override
  public Scope forModule(@NotNull ModulePath module) {
    return myMap.get(module);
  }
}
