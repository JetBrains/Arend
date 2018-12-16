package org.arend.naming.scope;

import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CachingScope implements Scope {
  private final Map<String, Referable> myElements = new LinkedHashMap<>();
  private final Map<String, Scope> myNamespaces = new HashMap<>();
  private final Map<String, Scope> myOnlyInternalNamespaces = new HashMap<>();
  private final Scope myScope;
  private final static Scope EMPTY_SCOPE = new Scope() {};

  private CachingScope(Scope scope) {
    myScope = scope;
    scope.find(ref -> {
      if (!(ref instanceof ModuleReferable)) {
        myElements.putIfAbsent(ref.textRepresentation(), ref);
      }
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
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    Map<String, Scope> namespaces = onlyInternal ? myOnlyInternalNamespaces : myNamespaces;
    Scope namespace = namespaces.get(name);
    if (namespace == null) {
      namespace = myScope.resolveNamespace(name, onlyInternal);
      namespace = namespace == null ? EMPTY_SCOPE : make(namespace);
      namespaces.put(name, namespace);
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
