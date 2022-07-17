package org.arend.naming.scope;

import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    scope.find(ref -> {
      myElements.putIfAbsent(ref instanceof ModuleReferable ? ((ModuleReferable) ref).path.getLastName() : ref.textRepresentation(), ref);
      return false;
    });
    myScope = scope;
  }

  public static Scope make(Scope scope) {
    return scope instanceof ImportedScope || scope == EmptyScope.INSTANCE || scope instanceof SimpleScope ? scope : new CachingScope(scope);
  }

  @NotNull
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
      namespace = namespace == null ? EMPTY_SCOPE : namespace instanceof CachingScope || namespace instanceof ImportedScope ? namespace : new CachingScope(namespace);
      namespaces.put(name, namespace);
    }

    return namespace == EMPTY_SCOPE ? null : namespace;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    Scope result = myScope.getGlobalSubscopeWithoutOpens(withImports);
    return result == myScope ? this : result;
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myScope.getImportedSubscope();
  }
}
