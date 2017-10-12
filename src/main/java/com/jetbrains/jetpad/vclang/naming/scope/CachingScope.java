package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class CachingScope implements Scope {
  private final Map<String, Referable> myElements = new LinkedHashMap<>();

  public static Scope fromScope(Scope scope) {
    if (scope.isEmpty()) {
      return EmptyScope.INSTANCE;
    }

    CachingScope result = new CachingScope();
    scope.find(ref -> { result.myElements.put(ref.textRepresentation(), ref); return false; });
    return result;
  }

  @Override
  public Collection<? extends Referable> getElements() {
    return myElements.values();
  }

  @Override
  public Referable resolveName(String name) {
    return myElements.get(name);
  }

  @Override
  public boolean isEmpty() {
    return myElements.isEmpty();
  }
}
