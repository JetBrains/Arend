package org.arend.module.scopeprovider;

import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleRegistry;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.term.group.Group;

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

  public void clear() {
    myMap.clear();
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
