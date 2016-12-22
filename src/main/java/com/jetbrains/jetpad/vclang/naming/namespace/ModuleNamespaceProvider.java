package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleNamespaceProvider {
  ModuleNamespace forModule(Abstract.ClassDefinition definition);
  ModuleNamespace root();
}
