package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

public class SimpleModuleNamespace implements ModuleNamespace {
  private final Map<String, SimpleModuleNamespace> mySubmoduleNamespaces = new HashMap<>();
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
    SimpleModuleNamespace submoduleNamespace = getSubmoduleNamespace(name);
    Abstract.ClassDefinition submodule = submoduleNamespace != null ? submoduleNamespace.getRegisteredClass() : null;
    Abstract.Definition resolved = myClassNamespace != null ? myClassNamespace.resolveName(name) : null;

    if (submodule == null) return resolved;
    else if (resolved == null) return submodule;
    // FIXME[error] proper exception
    else throw new IllegalStateException("Multiple declarations");
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return myClassNamespace == null ? Collections.<Abstract.ClassViewInstance>emptySet() : myClassNamespace.getInstances();
  }

  @Override
  public SimpleModuleNamespace getSubmoduleNamespace(String submodule) {
    return mySubmoduleNamespaces.get(submodule);
  }

  public SimpleModuleNamespace ensureSubmoduleNamespace(String submodule) {
    SimpleModuleNamespace ns = mySubmoduleNamespaces.get(submodule);
    if (ns == null) {
      ns = new SimpleModuleNamespace();
      mySubmoduleNamespaces.put(submodule, ns);
    }
    return ns;
  }

  public void registerClass(Abstract.ClassDefinition module) {
    if (myRegisteredClass != null) throw new IllegalStateException();
    myRegisteredClass = module;
    myClassNamespace = SimpleStaticNamespaceProvider.forClass(module);
  }

  @Override
  public Abstract.ClassDefinition getRegisteredClass() {
    return myRegisteredClass;
  }
}
