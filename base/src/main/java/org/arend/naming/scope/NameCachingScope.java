package org.arend.naming.scope;

import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class NameCachingScope implements Scope {
  private final Map<String, Referable> myElements = new LinkedHashMap<>();

  protected NameCachingScope(Scope scope, boolean withModules) {
    scope.find(ref -> {
      if (withModules || !(ref instanceof ModuleReferable)) {
        myElements.putIfAbsent(ref instanceof ModuleReferable ? ((ModuleReferable) ref).path.getLastName() : ref.textRepresentation(), ref);
      }
      return false;
    });
  }

  protected static boolean isCached(Scope scope) {
    return scope instanceof NameCachingScope || scope instanceof ImportedScope || scope == EmptyScope.INSTANCE || scope instanceof SimpleScope;
  }

  public static Scope make(Scope scope) {
    if (isCached(scope)) return scope;
    NameCachingScope result = new NameCachingScope(scope, true);
    return result.myElements.isEmpty() ? EmptyScope.INSTANCE : result;
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
}
