package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleNamespace implements Namespace {
  private final Map<String, Concrete.Definition> myNames = new HashMap<>();

  public SimpleNamespace() {
  }

  public SimpleNamespace(SimpleNamespace other) {
    myNames.putAll(other.myNames);
  }

  public SimpleNamespace(Concrete.Definition def) {
    this();
    addDefinition(def);
  }

  public void addDefinition(Concrete.Definition def) {
    addDefinition(def.getName(), def);
  }

  public void addDefinition(String name, final Concrete.Definition def) {
    final Concrete.Definition prev = myNames.put(name, def);
    if (!(prev == null || prev == def)) {
      throw new InvalidNamespaceException() {
        @Override
        public GeneralError toError() {
          return new DuplicateNameError(Error.Level.ERROR, def, prev, def); // TODO[abstract]
        }
      };
    }
  }

  public void addAll(SimpleNamespace other) {
    for (Map.Entry<String, Concrete.Definition> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Set<String> getNames() {
    return myNames.keySet();
  }

  Set<Map.Entry<String, Concrete.Definition>> getEntrySet() {
    return myNames.entrySet();
  }

  @Override
  public Concrete.Definition resolveName(String name) {
    return myNames.get(name);
  }
}
