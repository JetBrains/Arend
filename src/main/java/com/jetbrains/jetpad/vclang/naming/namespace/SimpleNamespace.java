package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.ReferableDuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleNamespace implements Namespace {
  private final Map<String, GlobalReferable> myNames = new HashMap<>();

  public SimpleNamespace() {
  }

  public SimpleNamespace(GlobalReferable def) {
    addDefinition(def, null);
  }

  public void addDefinition(GlobalReferable def, ErrorReporter errorReporter) {
    addDefinition(def.textRepresentation(), def, errorReporter);
  }

  private void addDefinition(String name, final GlobalReferable def, ErrorReporter errorReporter) {
    final GlobalReferable prev = myNames.putIfAbsent(name, def);
    if (!(prev == null || prev == def)) {
      errorReporter.report(new ReferableDuplicateNameError(Error.Level.ERROR, def, prev));
    }
  }

  public void addAll(SimpleNamespace other, ErrorReporter errorReporter) {
    for (Map.Entry<String, GlobalReferable> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue(), errorReporter);
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
