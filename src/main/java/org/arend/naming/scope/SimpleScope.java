package org.arend.naming.scope;

import org.arend.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleScope implements Scope {
  public final Map<String, Referable> names = new HashMap<>();
  public final Map<String, SimpleScope> namespaces = new HashMap<>();

  @Nonnull
  @Override
  public Collection<? extends Referable> getElements() {
    return names.values();
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    return names.get(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    return namespaces.get(name);
  }
}
