package org.arend.naming.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CachingScope extends NameCachingScope {
  private final Map<String, Scope> myNamespaces = new HashMap<>();
  private final Map<String, Scope> myOnlyInternalNamespaces = new HashMap<>();
  private final Scope myScope;
  private final static Scope EMPTY_SCOPE = new Scope() {};
  private final boolean myWithModules;

  private CachingScope(Scope scope, boolean withModules) {
    super(scope, withModules);
    myScope = scope;
    myWithModules = withModules;
  }

  public static Scope make(Scope scope) {
    return isCached(scope) ? scope : new CachingScope(scope, true);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    Map<String, Scope> namespaces = onlyInternal ? myOnlyInternalNamespaces : myNamespaces;
    Scope namespace = namespaces.get(name);
    if (namespace == null) {
      namespace = myScope.resolveNamespace(name, onlyInternal);
      namespace = namespace == null ? EMPTY_SCOPE : namespace instanceof CachingScope || namespace instanceof ImportedScope ? namespace : new CachingScope(namespace, myWithModules);
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
