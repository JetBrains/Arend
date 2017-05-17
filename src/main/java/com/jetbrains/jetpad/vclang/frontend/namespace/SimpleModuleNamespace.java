package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleModuleNamespace implements ModuleNamespace {
  private final Map<String, SimpleModuleNamespace> mySubmoduleNamespaces = new HashMap<>();
  private Abstract.ClassDefinition myRegisteredClass = null;

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

  void registerClass(Abstract.ClassDefinition module) {
    if (myRegisteredClass != null) throw new IllegalStateException();
    myRegisteredClass = module;
  }

  @Override
  public Abstract.ClassDefinition getRegisteredClass() {
    return myRegisteredClass;
  }
}
