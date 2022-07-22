package org.arend.naming.scope;

import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class CachingScope implements Scope {
  private final EnumMap<Referable.RefKind, Map<String, Referable>> myElements = new EnumMap<>(Referable.RefKind.class);
  private final Map<String, Scope> myNamespaces = new HashMap<>();
  private final Map<String, Scope> myOnlyInternalNamespaces = new HashMap<>();
  private final Scope myScope;
  private final static Scope EMPTY_SCOPE = new Scope() {};

  private CachingScope(Scope scope) {
    myScope = scope;
    scope.find(ref -> {
      myElements.compute(ref.getRefKind(), (k, map) -> {
        if (map == null) map = new LinkedHashMap<>();
        map.putIfAbsent(ref instanceof ModuleReferable ? ((ModuleReferable) ref).path.getLastName() : ref.textRepresentation(), ref);
        return map;
      });
      return false;
    });
  }

  public static Scope make(Scope scope) {
    return scope instanceof CachingScope || scope instanceof ImportedScope || scope == EmptyScope.INSTANCE || scope instanceof SimpleScope ? scope : new CachingScope(scope);
  }

  @Override
  public @Nullable Referable find(Predicate<Referable> pred) {
    for (Map<String, Referable> map : myElements.values()) {
      for (Referable ref : map.values()) {
        if (pred.test(ref)) {
          return ref;
        }
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(Referable.RefKind kind) {
    if (kind == null) {
      return Scope.super.getElements(null);
    }
    Map<String, Referable> map = myElements.get(kind);
    return map == null ? Collections.emptyList() : map.values();
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, @Nullable Referable.RefKind kind) {
    if (kind == null) {
      for (Referable.RefKind refKind : Referable.RefKind.values()) {
        Referable ref = resolveName(name, refKind);
        if (ref != null) {
          return ref;
        }
      }
      return null;
    } else {
      var map = myElements.get(kind);
      return map == null ? null : map.get(name);
    }
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    Map<String, Scope> namespaces = onlyInternal ? myOnlyInternalNamespaces : myNamespaces;
    Scope namespace = namespaces.get(name);
    if (namespace == null) {
      namespace = myScope.resolveNamespace(name, onlyInternal);
      namespace = namespace == null ? EMPTY_SCOPE : CachingScope.make(namespace);
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
