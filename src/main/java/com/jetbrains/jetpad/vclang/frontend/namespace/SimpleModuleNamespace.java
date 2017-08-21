package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleModuleNamespace implements ModuleNamespace {
  private final Map<String, SimpleModuleNamespace> mySubmoduleNamespaces = new HashMap<>();
  private Concrete.ClassDefinition myRegisteredClass = null;

  @Override
  public Set<String> getNames() {
    return new HashSet<>(mySubmoduleNamespaces.keySet());
  }

  @Override
  public SimpleModuleNamespace getSubmoduleNamespace(String submodule) {
    return mySubmoduleNamespaces.get(submodule);
  }

  SimpleModuleNamespace ensureSubmoduleNamespace(String submodule) {
    return mySubmoduleNamespaces.computeIfAbsent(submodule, k -> new SimpleModuleNamespace());
  }

  void registerClass(Concrete.ClassDefinition module) {
    if (myRegisteredClass != null) throw new IllegalStateException();
    myRegisteredClass = module;
  }

  @Override
  public Concrete.ClassDefinition getRegisteredClass() {
    return myRegisteredClass;
  }

  void unregisterClass() {
    if (myRegisteredClass == null) throw new IllegalStateException();
    myRegisteredClass = null;
  }
}
