package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
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

  private CachingScope(Scope scope) {
    myScope = scope;
    scope.find(ref -> {
      String name = ref instanceof ModuleReferable ? ((ModuleReferable) ref).path.getLastName() : ref.textRepresentation();
      myElements.putIfAbsent(name, ref);
      return false;
    });
  }

  public static Scope make(Scope scope) {
    return scope instanceof CachingScope ? scope : new CachingScope(scope);
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
  public Scope resolveNamespace(String name) {
    Scope namespace = myNamespaces.get(name);
    if (namespace == null) {
      namespace = myScope.resolveNamespace(name);
      namespace = namespace == null ? EMPTY_SCOPE : make(namespace);
      myNamespaces.put(name, namespace);
    }

    return namespace == EMPTY_SCOPE ? null : namespace;
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    Scope result = myScope.getGlobalSubscopeWithoutOpens();
    return result == myScope ? this : result;
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myScope.getImportedSubscope();
  }
}
