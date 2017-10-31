package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CachingScope implements Scope {
  private final Map<String, Referable> myElements = new LinkedHashMap<>();
  private final Map<String, Scope> myNamespaces = new HashMap<>();
  private final Scope myScope;
  private final static Scope EMPTY_SCOPE = new Scope() {};

  public CachingScope(Scope scope) {
    myScope = scope;
    scope.find(ref -> { myElements.put(ref.textRepresentation(), ref); return false; });
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getElements() {
    return myElements.values();
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    return myElements.get(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    if (!resolveModuleNames) {
      Referable referable = resolveName(name);
      if (referable == null || referable instanceof GlobalReferable && ((GlobalReferable) referable).isModule()) {
        return null;
      }
    }

    Scope namespace = myNamespaces.get(name);
    if (namespace == null) {
      namespace = myScope.resolveNamespace(name, true);
      namespace = namespace == null ? EMPTY_SCOPE : new CachingScope(namespace);
      myNamespaces.put(name, namespace);
    }

    return namespace == EMPTY_SCOPE ? null : namespace;
  }
}
