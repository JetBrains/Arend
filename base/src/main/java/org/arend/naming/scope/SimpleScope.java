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
  public Collection<? extends Referable> getAllElements() {
    return names.values();
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(Referable.RefKind kind) {
    return kind == Referable.RefKind.EXPR ? names.values() : Collections.emptyList();
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, Referable.RefKind kind) {
    return kind == null || kind == Referable.RefKind.EXPR ? names.get(name) : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    return namespaces.get(name);
  }
}
