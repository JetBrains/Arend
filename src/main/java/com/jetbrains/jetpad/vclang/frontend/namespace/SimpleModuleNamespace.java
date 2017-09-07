package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleModuleNamespace implements ModuleNamespace {
  private final Map<String, SimpleModuleNamespace> mySubmoduleNamespaces = new HashMap<>();
  private Group myRegisteredClass = null;

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

  void registerClass(Group group) {
    if (myRegisteredClass != null) throw new IllegalStateException();
    myRegisteredClass = group;
  }

  @Override
  public GlobalReferable getRegisteredClass() {
    return myRegisteredClass == null ? null : myRegisteredClass.getReferable();
  }

  void unregisterClass() {
    if (myRegisteredClass == null) throw new IllegalStateException();
    myRegisteredClass = null;
  }
}
