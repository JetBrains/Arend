package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleNamespace implements Namespace {
  private final Map<String, Referable> myNames = new HashMap<>();

  public SimpleNamespace() {
  }

  public SimpleNamespace(SimpleNamespace other) {
    myNames.putAll(other.myNames);
  }

  public SimpleNamespace(Abstract.Definition def) {
    this();
    addDefinition(def);
  }

  public void addDefinition(Referable def) {
    addDefinition(def.getName(), def);
  }

  public void addDefinition(String name, final Referable def) {
    final Referable prev = myNames.put(name, def);
    if (prev != null && prev != def) {
      throw new InvalidNamespaceException() {
        @Override
        public GeneralError toError() {
          return new DuplicateDefinitionError(prev, def);
        }
      };
    }
  }

  public void addAll(SimpleNamespace other) {
    for (Map.Entry<String, Referable> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Set<String> getNames() {
    return myNames.keySet();
  }

  Set<Map.Entry<String, Referable>> getEntrySet() {
    return myNames.entrySet();
  }

  @Override
  public Referable resolveName(String name) {
    return myNames.get(name);
  }
}
