package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleNamespace implements Namespace {
  private final Map<String, ModuleNamespace> mySubmoduleNamespaces = new HashMap<>();
  private Abstract.ClassDefinition myRegisteredClass = null;
  private Namespace myClassNamespace = null;

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(mySubmoduleNamespaces.keySet());
    if (myClassNamespace != null) names.addAll(myClassNamespace.getNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    ModuleNamespace submoduleNamespace = getSubmoduleNamespace(name);
    Abstract.ClassDefinition submodule = submoduleNamespace != null ? submoduleNamespace.getRegisteredClass() : null;
    Abstract.Definition resolved = myClassNamespace != null ? myClassNamespace.resolveName(name) : null;

    if (submodule == null) return resolved;
    else if (resolved == null) return submodule;
    // FIXME[error] proper exception
    else throw new IllegalStateException("Multiple declarations");
  }

  public ModuleNamespace getSubmoduleNamespace(String submodule) {
    return mySubmoduleNamespaces.get(submodule);
  }

  public ModuleNamespace ensureSubmoduleNamespace(String submodule) {
    ModuleNamespace ns = mySubmoduleNamespaces.get(submodule);
    if (ns == null) {
      ns = new ModuleNamespace();
      mySubmoduleNamespaces.put(submodule, ns);
    }
    return ns;
  }

  public void registerClass(Abstract.ClassDefinition module) {
    if (myRegisteredClass != null) throw new IllegalStateException();
    myRegisteredClass = module;
    myClassNamespace = SimpleStaticNamespaceProvider.forClass(module);
  }

  public Abstract.ClassDefinition getRegisteredClass() {
    return myRegisteredClass;
  }
}
