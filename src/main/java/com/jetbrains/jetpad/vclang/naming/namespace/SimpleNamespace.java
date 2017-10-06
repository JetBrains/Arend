package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.*;

public class SimpleNamespace implements Namespace {
  private final Map<String, GlobalReferable> myNames = new HashMap<>();

  public SimpleNamespace() {
  }

  public SimpleNamespace(GlobalReferable def) {
    addDefinition(def, null);
  }

  public void addDefinition(GlobalReferable def, LocalErrorReporter errorReporter) {
    addDefinition(def.textRepresentation(), def, errorReporter);
  }

  private void addDefinition(String name, final GlobalReferable def, LocalErrorReporter errorReporter) {
    final GlobalReferable prev = myNames.putIfAbsent(name, def);
    if (!(prev == null || prev == def)) {
      errorReporter.report(new DuplicateNameError(Error.Level.ERROR, def, prev));
    }
  }

  public void addAll(SimpleNamespace other, LocalErrorReporter errorReporter) {
    for (Map.Entry<String, GlobalReferable> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue(), errorReporter);
    }
  }

  @Override
  public Collection<GlobalReferable> getElements() {
    return myNames.values();
  }

  Set<Map.Entry<String, GlobalReferable>> getEntrySet() {
    return myNames.entrySet();
  }

  @Override
  public GlobalReferable resolveName(String name) {
    return myNames.get(name);
  }
}
