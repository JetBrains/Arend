package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;

public abstract class BaseModuleNamespaceProvider implements ModuleNamespaceProvider {
  private final SimpleModuleNamespace myRoot = new SimpleModuleNamespace();

  @Override
  public SimpleModuleNamespace root() {
    return myRoot;
  }

  protected static SimpleModuleNamespace ensureModuleNamespace(SimpleModuleNamespace rootNamespace, ModulePath modulePath) {
    if (modulePath.toList().isEmpty()) {
      return rootNamespace;
    }
    SimpleModuleNamespace parentNs = ensureModuleNamespace(rootNamespace, modulePath.getParent());
    return parentNs.ensureSubmoduleNamespace(modulePath.getName());
  }
}
