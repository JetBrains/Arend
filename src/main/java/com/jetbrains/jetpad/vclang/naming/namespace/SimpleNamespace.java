package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleNamespace implements Namespace {
  private final Map<String, Referable> myNames = new HashMap<>();

  public SimpleNamespace() {
  }

  public void addDefinition(Referable def) {
    if (myNames.put(def.getName(), def) != null) {
      throw new IllegalStateException("Duplicate name"); // FIXME[error]
    }
  }

  public void addAll(SimpleNamespace other) {
    for (Referable definition : other.myNames.values()) {
      addDefinition(definition);
    }
  }

  @Override
  public Set<String> getNames() {
    return myNames.keySet();
  }

  @Override
  public Referable resolveName(String name) {
    return myNames.get(name);
  }
}
