package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.NamespaceDuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleNamespace implements Namespace {
  private final Map<String, GlobalReferable> myNames = new HashMap<>();

  public SimpleNamespace() {
  }

  public SimpleNamespace(SimpleNamespace other) {
    myNames.putAll(other.myNames);
  }

  public SimpleNamespace(GlobalReferable def) {
    this();
    addDefinition(def);
  }

  public void addDefinition(GlobalReferable def) {
    addDefinition(def.textRepresentation(), def);
  }

  public void addDefinition(String name, final GlobalReferable def) {
    final GlobalReferable prev = myNames.put(name, def);
    if (!(prev == null || prev == def)) {
      throw new InvalidNamespaceException() {
        @Override
        public GeneralError toError() {
          return new NamespaceDuplicateNameError(Error.Level.ERROR, def, prev, (Concrete.SourceNode) def); // TODO[abstract]
        }
      };
    }
  }

  public void addAll(SimpleNamespace other) {
    for (Map.Entry<String, GlobalReferable> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Set<String> getNames() {
    return myNames.keySet();
  }

  Set<Map.Entry<String, GlobalReferable>> getEntrySet() {
    return myNames.entrySet();
  }

  @Override
  public GlobalReferable resolveName(String name) {
    return myNames.get(name);
  }
}
