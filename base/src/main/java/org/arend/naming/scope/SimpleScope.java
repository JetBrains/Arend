package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SimpleScope implements Scope {
  public final Map<String, Referable> names = new LinkedHashMap<>();
  public final Map<String, SimpleScope> namespaces = new HashMap<>();

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(@Nullable ScopeContext context) {
    return context == null || context == ScopeContext.STATIC ? names.values() : Collections.emptyList();
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    return context == null || context == ScopeContext.STATIC ? names.get(name) : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name) {
    return namespaces.get(name);
  }
}
