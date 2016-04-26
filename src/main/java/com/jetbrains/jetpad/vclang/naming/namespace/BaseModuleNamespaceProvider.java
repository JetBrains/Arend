package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;

public abstract class BaseModuleNamespaceProvider implements ModuleNamespaceProvider {
  private final ModuleNamespace myRoot = new ModuleNamespace();

  @Override
  public ModuleNamespace root() {
    return myRoot;
  }

  protected static ModuleNamespace ensureModuleNamespace(ModuleNamespace rootNamespace, ModulePath modulePath) {
    if (modulePath.list().length == 0) {
      return rootNamespace;
    }
    ModuleNamespace parentNs = ensureModuleNamespace(rootNamespace, modulePath.getParent());
    return parentNs.ensureSubmoduleNamespace(modulePath.getName());
  }
}
